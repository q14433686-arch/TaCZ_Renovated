package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 【Iris 专属】把「镜内那一遍」里的 Iris 手部 pass 整个取消。
 *
 * <h2>修掉什么（2026-09-02 用户实机报告：枪身只在二次渲染时被「高倍镜」裁切）</h2>
 * Iris 把第一人称手部渲染搬进了 {@code LevelRenderer#render} 内部
 * （{@code handler$boc000$iris$beginTranslucents → HandRenderer.renderSolid /
 * renderTranslucent → renderAllFeatures} —— 用户实机 latest.log 的调用栈逐帧可证）。
 * 而二次渲染的镜内那一遍<b>正是又调了一次</b> {@code levelRenderer.render}
 * （{@code ScopePipRenderer#renderScopeView}，窄投影 + 主 target 重定向）。
 * 于是镜内那一遍<b>自带一次手部 pass</b>：视模立方体（枪身/镜筒/准星）按
 * <b>窄投影</b>提交并画进了镜内画面，其中目镜掩码裁掉的区域（窄投影下的孔径）
 * 就是用户看到的「枪身被高倍镜裁了一个孔」。
 *
 * <p>重投影模式没有第二次 {@code levelRenderer.render}，自然没有这遍手部 pass ——
 * 所以症状「仅开启二次渲染时」出现。1.21.11 / 26.1.2 线没有二次渲染 PIP，
 * 同样触发不了。</p>
 *
 * <h2>为什么是「取消整遍」而不是别的</h2>
 * 镜内画面按定义就是<b>放大后的世界</b>：合成只取镜片孔径内的像素，孔径里
 * 「本来就该是干净的世界画面，不该有枪件」（{@code PolyMeshGpuRenderer#renderAfterSolid}
 * 里 mesh 表那道 {@code isInsideScopeLevelRender} 闸的注释原文就是这个意思）。
 * 那道闸此前只挡住了 <b>mesh 手部表</b>（清空不画），立方体（executeSolid）
 * 仍然照画 —— 本 mixin 把同一原则补到立方体上：手部 pass 在镜内那一遍
 * 整体不跑。取消后：
 * <ul>
 *   <li>视模立方体、mesh 枪件都不进镜内画面（口径与重投影模式一致：
 *       重投影拷贝取于 {@code renderItemInHand} 之前，同样不含视模）；</li>
 *   <li>镜内不再消费/登记 {@code HAND_DRAWS}、不再触发阶段边界的掩码绘制
 *       （窄投影掩码从根上消失，主画面那一遍仍按主投影正常画掩码）；</li>
 *   <li>{@code drawnThisFrame} 等帧级防线保留（belt &amp; braces，本类与它们不互斥）。</li>
 * </ul>
 *
 * <h2>注入点与签名（Iris 1.11.2+mc26.2 源码核对）</h2>
 * {@code public void renderSolid(Matrix4fc, float, Camera, CameraRenderState,
 * GameRenderer, WorldRenderingPipeline)} —— 六个实参里只有
 * {@code WorldRenderingPipeline} 是 Iris 内部类型：本仓不引 Iris 编译依赖
 * （与 {@code IrisGlCommandEncoderMixin} 同款 {@code @Coerce Object} 手法），
 * 目标类用字符串引用，编译期不触 Iris。
 *
 * <p>两个方法都打：Iris 一帧两趟手部 pass（solid + translucent，共用同一份
 * {@code SubmitNodeStorage}），只挡 solid 的话 translucent 那趟会把重复提交
 * 的节点再画一遍，镜内照样进枪件。</p>
 *
 * <h2>不取消会漏什么 / 取消会漏什么</h2>
 * <ul>
 *   <li>取消发生在 HEAD：方法体未执行，{@code ACTIVE}/{@code renderingSolid}/
 *       投影备份/相位切换都没发生 —— 无状态残留可清；</li>
 *   <li> {@code renderTranslucent} 体内的 {@code bufferSource.endFrame()} 被跳过：
 *       镜内那一遍没有分配任何手部顶点缓冲，主画面那一趟照常
 *       {@code endFrame()}，每帧一次，正确；</li>
 *   <li>镜内孤立管线（tacz:scope_pip）的相位停在世界末相位而不是 NONE：
 *       该管线在「相位→拷贝→归还」之间没有任何使用点，主画面管线是另一实例
 *       （时序状态天然隔离），无跨遍影响；</li>
 *   <li>Iris 升级改了方法名/签名时，本注入在 {@code defaultRequire=0} 下静默
 *       失效 —— 首次跳过时打的那行 log 就是「注入匹配成功」的实机证据，
 *       用户若仍见镜内枪件而日志无此行，即可判定是注入脱靶而非别处。</li>
 * </ul>
 */
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer")
public abstract class IrisHandRendererMixin {

    private static boolean loggedScopeSkip;

    @Inject(method = "renderSolid", at = @At("HEAD"), cancellable = true)
    private void tacz$skipHandSolidInsideScopePass(Matrix4fc modelMatrix, float tickDelta,
                                                   Camera camera, CameraRenderState cameraState,
                                                   GameRenderer gameRenderer,
                                                   @Coerce Object pipeline, CallbackInfo ci) {
        tacz$maybeSkipInsideScopePass("solid", ci);
    }

    @Inject(method = "renderTranslucent", at = @At("HEAD"), cancellable = true)
    private void tacz$skipHandTranslucentInsideScopePass(Matrix4fc modelMatrix, float tickDelta,
                                                         Camera camera, CameraRenderState cameraState,
                                                         GameRenderer gameRenderer,
                                                         @Coerce Object pipeline, CallbackInfo ci) {
        tacz$maybeSkipInsideScopePass("translucent", ci);
    }

    private void tacz$maybeSkipInsideScopePass(String phase, CallbackInfo ci) {
        if (!ScopePipRenderer.isInsideScopeLevelRender()) {
            return;
        }
        if (!loggedScopeSkip) {
            loggedScopeSkip = true;
            GunMod.LOGGER.info("[TACZ Scope] Iris hand pass ({} phase) skipped inside the scope PIP "
                            + "re-render pass: the in-lens image must be the magnified world only, "
                            + "without the first-person viewmodel. Previously the viewmodel cubes were "
                            + "drawn there under the narrow projection and clipped by the ocular mask, "
                            + "which showed up as the gun body being cut out by the scope aperture. "
                            + "(Logged once; this line also proves the injection matched.)",
                    phase);
        }
        ci.cancel();
    }
}
