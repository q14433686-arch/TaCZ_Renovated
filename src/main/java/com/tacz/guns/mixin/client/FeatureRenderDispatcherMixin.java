package com.tacz.guns.mixin.client;

import cn.sh1rocu.tacz.compat.meshloader.render.PolyMeshGpuRenderer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopeMaskRenderer;
import com.tacz.guns.client.render.scope.ScopeFinalRingOverlay;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 {@code renderAllFeatures} 的<b>阶段边界</b>插入瞄具掩码 pass 与镜内画中画的合成。
 *
 * <h2>为什么必须是这个位置（26.2 的论证，作为背景保留）</h2>
 * 26.2 的绘制结构（字节码确认）：
 * <pre>
 * renderAllFeatures(storage) {
 *     PreparedFrame f = prepareFrame(storage);   // 只准备，不绘制
 *     f.executeSolid();                          // ← 各 executeXxx 内部才开关 pass
 *     f.executeTranslucent();
 *     f.executeTranslucentAfterTerrain();
 *     f.executeAlwaysOnTop();
 *     f.close();
 * }
 * </pre>
 * 也就是说<b>各阶段之间不在任何 render pass 内</b>，
 * 满足 {@code CommandEncoder#createRenderPass} 开头那句断言。
 * 这正是 r51 失败的反面：vanilla 自己的多 target 从来都是
 * <b>成批地、在阶段边界</b>切 —— 本 mixin 就是回到那个模式。
 *
 * <h2>26.3：注入点从「阶段边界」搬到 prepareFrame 之后</h2>
 *
 * <p>26.2 的 {@code renderAllFeatures} 自己不开 pass，各 {@code executeXxx}
 * 内部各开各的，所以阶段之间是「无 pass 状态」，我们可以在那里插一个
 * 自己的 pass 画掩码。<b>26.3 把 pass 的归属整个倒过来了</b>：
 * {@code GameRenderer#renderItemInHand}（GR:399-408）先
 * {@code createRenderPass("Item in hand")}，再把这个 pass 当参数一路传进
 * {@code renderAllFeatures(renderPass, frame)} → {@code executeSolid(renderPass)}。
 * {@code FeatureRenderDispatcher} 内部<b>一个 pass 都不开</b>。
 *
 * <p>于是原来的阶段边界已经身处 vanilla 的 pass 之内，再调
 * {@code createRenderPass} 必然撞上：
 * <pre>Close the existing render pass before creating a new one!</pre>
 * （refab 2026-09-18 实机日志：掩码整条被禁用，随后镜身管线拿不到掩码而崩。）
 *
 * <p>新锚点选 {@code prepareFrame} 的 RETURN，它同时满足三个约束：
 * <ul>
 *   <li>在 {@code prepareFrameWithContext} 的 {@code stagedVertexBuffer.upload()}
 *       <b>之后</b> —— 掩码几何要的顶点数据已经上传（这是原注释里
 *       「必须在 upload 之后」那条约束，依旧成立）；</li>
 *   <li>在 vanilla {@code createRenderPass("Item in hand")} <b>之前</b> ——
 *       try-with-resources 的资源按书写顺序初始化，{@code prepareFrame} 是
 *       第一个，pass 是第二个（GR:399-405），所以此刻确实还没有 pass 开着；</li>
 *   <li>仍在 {@code executeSolid} 之前 —— 镜身在 solid 阶段采样掩码，
 *       掩码必须先就绪。</li>
 * </ul>
 */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin {

    /**
     * 全程<b>只有这一个</b> PreparedFrame 实例，主画面那一遍与镜内那一遍轮流用它。
     * 正因为是同一个，镜内那遍漏关就会把主画面那遍顶掉。
     */
    @Shadow
    @Final
    private FeatureRenderDispatcher.PreparedFrame preparedFrame;

    @Unique
    private static boolean tacz$loggedFrameRecovery;

    /**
     * 把镜内那一遍失败时漏关的 PreparedFrame 关掉。
     *
     * <h2>不做会怎样</h2>
     * {@code LevelRenderer#render} 是「{@code prepareFrame} → 执行 frame graph →
     * {@code close}」的直写结构，中间抛异常 {@code close()} 就没了。
     * 而我们是在同一帧里<b>先</b>驱动一遍 {@code levelRenderer.render} 画镜内画面、
     * <b>再</b>让 vanilla 画主画面的，于是镜内那遍留下的「在用」标志会把主画面那遍
     * 直接顶成 {@code IllegalStateException: PreparedFrame already in use}。
     *
     * <p>结果就是：{@code ScopePipRenderer} 那边明明捕获了异常、打印了
     * 「PIP disabled, falling back to whole-screen FOV zoom」，游戏却仍旧崩了，
     * 而且崩溃报告里<b>只剩这个二次错误</b>，真正的病因一个字都看不见。
     *
     * <h2>为什么调 close() 而不是把 context 抹成 null</h2>
     * {@code close()} 做的是<b>真正的收尾</b>：给每个 FeatureRenderer 调
     * {@code finishExecute(context)}、给 {@code stagedVertexBuffer} 调 {@code endDraw()}
     * （与 {@code prepareFrameWithContext} 里的 {@code upload()} 配对）、
     * 再清掉本帧攒下的 submit 列表。直接抹字段会把这些全跳过。
     *
     * <h2>为什么只在「刚失败过」时才动它</h2>
     * 这个标志由 {@code ScopePipRenderer} 在它自己的 catch 里置位，取一次即清。
     * 正常帧上这里读一个 volatile boolean 就返回，既不改变任何行为，
     * 也绝不会去碰一个本来就该开着的 frame。
     */
    @Inject(method = "prepareFrame", at = @At("HEAD"))
    private void tacz$releaseFrameLeakedByFailedScopePass(
            SubmitNodeStorage storage,
            CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        if (!ScopePipRenderer.consumePreparedFrameLeak()) {
            return;
        }
        // 失败发生在 prepareFrame 之前（比如投影都没建起来）时这里是 null，什么都没漏。
        if (((PreparedFrameAccessor) this.preparedFrame).tacz$context() == null) {
            return;
        }
        this.preparedFrame.close();
        if (!tacz$loggedFrameRecovery) {
            tacz$loggedFrameRecovery = true;
            GunMod.LOGGER.warn("[TACZ Scope] The scope pass left this frame's PreparedFrame open when "
                    + "it failed; closed it so the main view can still render. The real cause is the "
                    + "exception logged just above this line - without this recovery the game would "
                    + "have crashed here with a misleading 'PreparedFrame already in use'.");
        }
    }

    /** 记住「当前正在准备哪一个 storage」，镜内那一遍据此只保留主画面那一份提交节点。 */
    @Inject(method = "prepareFrameWithContext", at = @At("HEAD"))
    private void tacz$trackPreparingStorage(
            FeatureFrameContext context,
            SubmitNodeStorage storage,
            CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        ScopePipRenderer.setCurrentPreparingStorage(storage);
    }

    @Inject(method = "prepareFrameWithContext", at = @At("RETURN"))
    private void tacz$resetPreparingStorage(
            FeatureFrameContext context,
            SubmitNodeStorage storage,
            CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        ScopePipRenderer.setCurrentPreparingStorage(null);
    }

    @Inject(method = "prepareFrame", at = @At("RETURN"))
    private void tacz$scopeMaskAtPhaseBoundary(SubmitNodeStorage storage,
                                               CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        // 【Step 2】画真正的目镜掩码。
        ScopeMaskRenderer.renderAtPhaseBoundary();
        // 【镜内画中画】紧跟掩码之后合成。三者的先后关系是硬约束：
        //
        //   掩码           -> 知道镜内是哪些像素
        //   合成（这一句）  -> 那些像素被贴上离屏渲染的放大世界
        //   executeSolid…  -> 镜身在镜内 discard（PIP 画面得以留住）；
        //                     准星反向裁剪只画镜内（浮在 PIP 画面之上）
        //
        // 往前挪掩码还没就绪，往后挪（比如手持渲染之后）准星会被 PIP 盖掉。
        ScopePipRenderer.compositeAtPhaseBoundary();
        // 【遮光环最终覆盖】就在此刻快照手持那一遍的投影/模型视图 —— 再晚一点
        // （手部几何画完之后）这两个矩阵就被还原成世界的了，延后重画会飘。
        // 内部自判手部 pass + 队列非空，无光影零开销。
        if (ScopeMaskRenderer.isInHandPass()) {
            ScopeFinalRingOverlay.captureHandTransform();
        }
    }

    /**
     * <b>第一人称</b> poly_mesh GPU 绘制：必须在 executeSolid <b>之后</b>。
     *
     * <p>本注入点只服务手部表（HAND_DRAWS）。MV-PROBE v2 字节码取证证明
     * renderAllFeatures 的调用者只有手部（renderItemInHand 偏移 185）与
     * GUI 系（GuiItemAtlas / PictureInPictureRenderer / renderLevel 560 的
     * 收尾调用）—— <b>26.2 的世界实体 pass 不经过 renderAllFeatures</b>
     * （LevelRenderer.render 的帧图 lambda 直调 executeSolid）。
     * 世界表（WORLD_DRAWS）的消费点因此在 {@code LevelRendererWorldPassMixin}
     * （LevelRenderer#executeSolid RETURN，调用者判据见该类）。</p>
     *
     * <p>关 PR（#33/#69/#70/#71）画在 executeSolid 之前、并且用一张全局 WORLD 表，
     * GUI / 掉落物于是会在<b>世界</b> pass 里被画出去（这就是「贴图不对」那类症状）。
     * 这里只在手部 pass 消费 HAND_DRAWS（{@code renderAfterSolid} 内部判
     * {@code ScopeMaskRenderer#isInHandPass}），其余调用者直接把手部残留清空。</p>
     *
     * <h2>26.3：同样被迫离开「无 pass」的阶段边界</h2>
     *
     * <p>理由与上面的掩码注入点完全相同 —— {@code executeSolid} 之后仍在
     * vanilla 那个 "Item in hand" pass 内部，{@code PolyMeshGpuRenderer}
     * 要自开 pass 就会撞 isInRenderPass 断言。</p>
     *
     * <p>一度搬到 {@code renderItemInHand} 的 RETURN —— 但那里
     * {@code modelViewStack} 已 pop（无光影下只有正北跟手），且 Iris 下手部
     * 由 {@code HandRenderer} 在 {@code LevelRenderer.render} 内部自调
     * {@code renderAllFeatures}（拖到外面画 = 顶点格式/HAND program 全错 ⇒
     * 拉伸成片）。refab 2026-09-20/21 两轮实机定案（§4.6）：
     * <b>留在 renderAllFeatures 内、executeSolid 之后</b>。</p>
     *
     * <p>「pass 内不许再开 pass」的问题用另一种方式解决：不自开，
     * 把 {@code renderAllFeatures} 的形参 {@code renderPass} 直接传下去录制
     * （世界表 {@code LevelRendererWorldPassMixin} 已是同一做法）。
     * {@code renderAllFeatures} 是静态方法，处理器随之为静态。</p>
     */
    @Inject(
            method = "renderAllFeatures",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSolid(Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void tacz$polyMeshAfterHandSolid(RenderPass renderPass,
                                                    FeatureRenderDispatcher.PreparedFrame frame,
                                                    CallbackInfo ci) {
        PolyMeshGpuRenderer.renderAfterSolid(renderPass);
    }
}
