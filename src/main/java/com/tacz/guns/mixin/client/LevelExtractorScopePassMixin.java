package com.tacz.guns.mixin.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import com.tacz.guns.compat.iris.IrisScopePipelineCompat;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 镜内那一遍期间，取消 Iris 请求的「整份渲染器重载」。
 *
 * <h2>为什么要拦</h2>
 * Iris 在一套管线第一次渲染时会请求一次 {@code allChanged()}。那一次调用会顺着
 * Voxy 挂在它上面的钩子把整个 {@code VoxyRenderSystem} 拆掉重建 ——
 * 而我们为镜内那一遍建的<b>第二套 Voxy 渲染栈</b>当场变成一堆已释放的 GL 对象，
 * 而且重建后 Voxy 会绑到<b>瞄具</b>那套管线上，主画面的远景从此永久错乱。
 *
 * <p>它要刷新的方块 id 状态是全局的、主管线已经设好，拦掉没有副作用。
 *
 * <h2>真正的重载必须放行</h2>
 * 不在镜内那一遍时，这是一次<b>货真价实</b>的重载（玩家改了区块视距、按了 F3+A、
 * 换了资源包）。那时必须反过来<b>主动</b>把第二套还回去，见
 * {@link IrisScopePipelineCompat#onLevelRendererReload()}。
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorScopePassMixin {

    @Unique
    private static boolean tacz$logged;

    @Inject(method = "allChanged", at = @At("HEAD"), cancellable = true)
    private void tacz$noFullReloadDuringScopePass(CallbackInfo ci) {
        // 【预热窗口同样要挡 —— 05170(26.1.2) 实机 ESC 崩溃 d3f0fdc 的保险带】
        // 实机链条（26.1.2 线 RawOutput.log 钉死）：prewarm 的 preparePipeline 期间
        // 瞄具管线是 Iris 的«当前管线»，一次漏网的 allChanged 让 Voxy 全量重建、
        // 把主渲染栈绑到瞄具管线上，主画面远景此后永久错乱（本仓若有空闲释放
        // 机制还会在销毁时连带崩 "Tried to use destroyed RenderTargets"）。
        // 26.2 侧已核实的触发点是«管线首次渲染»（本类头注释那条链，已被
        // isScopePassActive 挡住）；构建期是否也会触发未在 26.2 复现 ——
        // 本闸是防御带：窗口极窄（一次 preparePipeline 调用），误伤一次真实
        // 重载的概率可忽略，且 cancel 本身无害（它刷新的全局状态主管线早已设好）。
        // 取消的 reload 从未执行，Voxy 不会收到通知，无需补偿。
        if (!ScopePipRenderer.isScopePassActive()
                && !IrisScopePipelineCompat.isBuildingScopePipeline()) {
            IrisScopePipelineCompat.onLevelRendererReload();
            return;
        }
        if (!tacz$logged) {
            tacz$logged = true;
            GunMod.LOGGER.info("[TACZ Scope] Suppressed a full renderer reload requested during the "
                    + "scope pass (or the scope-pipeline prewarm build). Iris asks for it once when a "
                    + "pipeline first renders, and it would make Voxy rebind itself to the scope "
                    + "pipeline for the rest of the session, permanently corrupting distant terrain "
                    + "in the main view. The block-id state it refreshes is global and already set by "
                    + "the main pipeline.");
        }
        ci.cancel();
    }
}
