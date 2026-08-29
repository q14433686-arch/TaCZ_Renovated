package com.tacz.guns.client.render.scope;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.renderer.snapshot.BedrockRenderSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 【遮光环最终覆盖】把手持那一遍的物理目镜框（{@code ocular_ring}）延后到
 * 「镜内画中画的合成」<b>之后</b>再画一遍。
 *
 * <h2>它补的是哪个洞</h2>
 * 无光影时，合成跑在阶段边界（{@code executeSolid()} 之前），枪身/配件/目镜框/准星
 * 全部画在它<b>之后</b>，于是合成多铺的那部分（掩码略大于真实通光孔径）会被目镜框
 * 原样盖住 —— 症状不可见。
 *
 * <p><b>光影下这个先后关系是反的</b>：Iris 把手部渲染搬进了
 * {@code LevelRenderer#render} 内部，{@code ScopePipRenderer#compositeAfterLevelUnderShaders}
 * 只能等整条 Iris 管线收工后（{@code LevelRenderer#render} 返回处）再合成 ——
 * 也就是<b>画在手部之后</b>。于是合成把刚刚画好的遮光环整片盖掉，孔径内那一圈
 * 变成「放大后的世界」。世界本身是暗的时候看不出来，<b>对着光就一目了然</b>：
 * 表现就是「目镜的遮光环变成半透明」。
 *
 * <h2>机制来源</h2>
 * 1.21.11 邻链 commit {@code 2710c7c}「render reticle after Iris final composite」
 * 用 {@code ScopeFinalOverlayState} + {@code scope_ring_final.fsh} 解决过<b>同一类</b>
 * 问题（遮光环被光影包的后置 pass 盖掉）。本类是它在 26.2 的同源形态：
 * 排队 → 合成之后用「无雾、无掩码」的原版 entity 管线重画。
 *
 * <h2>26.2 与本仓的适配（与 1.21.11 那版的三处不同，务必别照抄）</h2>
 * <ol>
 *   <li><b>不自建 {@code FeatureRenderDispatcher}</b>：26.2 的构造函数改成吃
 *       {@code RenderBuffers}（1.21.11 那版吃 7 个参数，在这里编译不过）。
 *       官方配方是 {@code Minecraft.getInstance().gameRenderer.featureRenderDispatcher()
 *       .renderAllFeatures(storage)}（见 NeoForge 26.2 迁移指南）。</li>
 *   <li><b>不调 {@code SubmitNodeStorage#endFrame}</b>：26.2 里它连同 {@code clear}
 *       一起被移除，{@code renderAllFeatures} 自己收尾。</li>
 *   <li><b>刷新点不是 Iris 的 {@code finalizeLevelRendering} TAIL</b>：那一步跑在
 *       {@code LevelRenderer#render} 内部，<b>早于</b>我们的合成。这里由
 *       {@code GameRendererMixin} 在合成调用之后直接刷新。</li>
 * </ol>
 *
 * <h2>排队失败的代价</h2>
 * 只在「光影 + 镜内画中画」这条路径上排队；其它任何情况
 * （无光影 / 未开 PIP / 第三人称 / 没有该骨骼）走的还是原来的主提交，
 * 行为与改动前逐位等价。刷新阶段整体包在 try/catch 里：
 * 画不出去最坏只是「遮光环仍被盖住」（即今天的行为），不会让客户端崩。
 */
public final class ScopeFinalRingOverlay {

    /**
     * 覆盖层的提交序号。
     *
     * <p>取一个很大的正数：{@code SubmitNodeStorage} 按 order 升序渲染，
     * 20_001 保证这一批在默认（order 0）的视模提交之后 —— 本类用的是
     * 【独立】的一份 storage，所以这个序号只影响批次内的排序，与画面无关，
     * 留它是为了与邻链保持一致、便于对照。</p>
     */
    private static final int FINAL_RING_ORDER = 20_001;

    private static final List<RingDraw> PENDING = new ArrayList<>();

    /**
     * 手持那一遍的变换快照。
     *
     * <p>快照里的几何已经把 ADS/后坐/视角摇晃烘进顶点，但<b>投影与模型视图</b>
     * 是渲染时才生效的全局状态 —— 刷新发生在 {@code LevelRenderer#render} 之后，
     * 那时它们早被还原成世界相机，必须在这里换回来，否则目镜框会飘到画面外。</p>
     */
    private static @Nullable HandTransform handTransform;

    private static boolean loggedQueued;
    private static boolean loggedRendered;
    private static boolean loggedFailure;

    private ScopeFinalRingOverlay() {
    }

    /**
     * 帧首清空。接 {@code ScopePipRenderer#beginFrame}（{@code GameRenderer#extract} HEAD）。
     *
     * <p>必须清：排队发生在手部 pass，刷新发生在合成之后；万一那一帧没走到刷新点
     * （光影被临时关掉、PIP 自我停用），残留的快照会在下一帧被画到错的地方。</p>
     */
    public static void beginFrame() {
        PENDING.clear();
        handTransform = null;
    }

    /**
     * 快照手持那一遍<b>真正使用</b>的投影与模型视图。
     *
     * <h2>为什么必须在阶段边界取，不能在排队时取</h2>
     * 排队发生在 {@code submit}（收集节点）阶段，那时 {@code RenderSystem} 里挂的还是
     * <b>世界</b>的投影/模型视图；而手持那一遍用的是另一套（固定窄 FOV）。
     * 几何快照里的顶点是【已套用 poseStack 的世界坐标】、节点 pose 是单位矩阵，
     * 所以最终落点<b>完全</b>取决于绘制那一刻的这两个矩阵 —— 取错就是整个目镜框
     * 飘到画面外。阶段边界（{@code renderAllFeatures} 里 {@code executeSolid()} 之前）
     * 正是这些矩阵已经就位、而手部几何还没画的那一刻，与
     * {@code ScopeMaskRenderer#renderAtPhaseBoundary} 取掩码投影的位置逐字相同。
     *
     * <p>只取切片对象、<b>不读回内容</b>：那 64 字节在光影下不可读（本仓
     * {@code latest.log} 有实录的 {@code Buffer is not readable}），这里只是原样
     * 交给 {@code RenderSystem.setProjectionMatrix} 还回去。</p>
     *
     * <p>幂等：一帧内第一次调用生效，HandRenderer 的两次手部 pass 不会互相覆盖。</p>
     */
    public static void captureHandTransform() {
        if (handTransform != null) {
            return;
        }
        handTransform = new HandTransform(
                new Matrix4f(RenderSystem.getModelViewMatrix()),
                RenderSystem.getProjectionMatrixBuffer(),
                RenderSystem.getProjectionType());
    }

    /**
     * 把手持那一遍的物理目镜框排进最终覆盖层。
     *
     * @param snapshot {@code BedrockRenderSnapshot.captureSubtree} 得到的几何（含子树）
     * @param texture  该瞄具的贴图；为 {@code null} 时无法构造最终覆盖用的 RenderType，放弃排队
     */
    public static void queue(BedrockRenderSnapshot snapshot, @Nullable Identifier texture) {
        if (snapshot.isEmpty() || texture == null) {
            return;
        }
        PENDING.add(new RingDraw(snapshot, texture));
        if (!loggedQueued) {
            loggedQueued = true;
            GunMod.LOGGER.info("[TACZ Scope] Queued the physical ocular ring for the post-composite overlay "
                    + "(Iris owns the lens, so the PIP composite runs after the hand pass).");
        }
    }

    /** @return 本帧是否有排队中的几何（供诊断） */
    public static boolean hasPendingRings() {
        return !PENDING.isEmpty();
    }

    /**
     * 把排队中的目镜框画到主画面上。
     *
     * <p>调用点在 {@code GameRendererMixin} 的 {@code LevelRenderer#render} 返回处，
     * 紧跟 {@code ScopePipRenderer#compositeAfterLevelUnderShaders()} —— 无排队时
     * 立刻返回，开销就是一次 List 判空。</p>
     */
    public static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        if (handTransform == null) {
            // 没取到手持变换（阶段边界注入没跑到）—— 拿世界矩阵去画只会把目镜框
            // 甩到画面外，宁可不画（= 今天的行为：遮光环被合成盖住）。
            if (!loggedFailure) {
                loggedFailure = true;
                GunMod.LOGGER.warn("[TACZ Scope] Ocular ring overlay skipped: the hand transform was never "
                        + "captured, so the ring would be drawn with the wrong projection.");
            }
            PENDING.clear();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) {
            PENDING.clear();
            handTransform = null;
            return;
        }
        List<RingDraw> rings = List.copyOf(PENDING);
        HandTransform transform = handTransform;
        PENDING.clear();
        handTransform = null;

        GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType previousProjectionType = RenderSystem.getProjectionType();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(transform.modelView());
        RenderSystem.setProjectionMatrix(transform.projection(), transform.projectionType());
        try {
            // 26.2 官方配方：自建一份 storage → 按 order 取提交器 → 交给 dispatcher。
            SubmitNodeStorage storage = new SubmitNodeStorage();
            OrderedSubmitNodeCollector collector = storage.order(FINAL_RING_ORDER);
            for (RingDraw draw : rings) {
                collector.submitCustomGeometry(new PoseStack(),
                        ScopeBodyRenderTypes.ringFinal(draw.texture()),
                        (entryPose, consumer) -> draw.snapshot().write(consumer));
            }
            mc.gameRenderer.featureRenderDispatcher().renderAllFeatures(storage);
            if (!loggedRendered) {
                loggedRendered = true;
                GunMod.LOGGER.info("[TACZ Scope] Drew the physical ocular ring after the PIP composite.");
            }
        } catch (RuntimeException | LinkageError e) {
            if (!loggedFailure) {
                loggedFailure = true;
                GunMod.LOGGER.warn("[TACZ Scope] Post-composite ocular ring overlay failed; "
                        + "the ring may stay covered by the magnified view this session.", e);
            }
        } finally {
            modelView.popMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, previousProjectionType);
        }
    }

    private record HandTransform(Matrix4f modelView,
                                 GpuBufferSlice projection,
                                 ProjectionType projectionType) {
        private HandTransform {
            modelView = new Matrix4f(modelView);
        }
    }

    private record RingDraw(BedrockRenderSnapshot snapshot, Identifier texture) {
    }
}
