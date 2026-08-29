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
        if (!ScopePipRenderer.isScopePassActive()) {
            IrisScopePipelineCompat.onLevelRendererReload();
            return;
        }
        if (!tacz$logged) {
            tacz$logged = true;
            GunMod.LOGGER.info("[TACZ Scope] Suppressed a full renderer reload requested during the "
                    + "scope pass. Iris asks for it once when a pipeline first renders, and it would "
                    + "make Voxy rebind itself to the scope pipeline for the rest of the session, "
                    + "permanently corrupting distant terrain in the main view. The block-id state it "
                    + "refreshes is global and already set by the main pipeline.");
        }
        ci.cancel();
    }
}
