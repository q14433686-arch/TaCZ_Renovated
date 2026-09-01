package com.tacz.guns.mixin.client.voxy;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隔离了 Iris 管线、却没能把 Voxy 切到第二套渲染栈时，让 Voxy 在镜内那一遍缺席。
 *
 * <p>Voxy 的渲染栈终身绑定在一套 Iris 管线上，第二套管线下它画出来必然是错的
 * （远景错块）。宁可镜内没有 LOD，也不能画错 —— 主画面必须是对的。
 *
 * <p>想让镜内也有 LOD 远景，关掉 {@code ScopePipIsolatePipeline}
 * （那样只有一套管线，Voxy 照常画，只是时域效果会失准）。
 */
@Mixin(targets = "me.cortex.voxy.client.core.VoxyRenderSystem", remap = false)
public abstract class VoxyRenderSystemMixin {

    @Unique
    private static boolean tacz$logged;

    @Inject(method = "renderOpaque", at = @At("HEAD"), cancellable = true, require = 0)
    private void tacz$skipVoxyDrawInIsolatedScopePass(CallbackInfo ci) {
        if (!ScopePipRenderer.shouldSuppressVoxyDraw()) {
            return;
        }
        if (!tacz$logged) {
            tacz$logged = true;
            GunMod.LOGGER.info("[TACZ Scope] Voxy is sitting out the scope pass while that pass uses "
                    + "its own Iris pipeline. Voxy binds to a single Iris pipeline for its lifetime, so "
                    + "it cannot draw correctly under a second one. The lens will not show distant LOD "
                    + "terrain; the main view stays correct. Set ScopePipIsolatePipeline=false to get "
                    + "LOD in the lens instead, at the cost of temporal artifacts.");
        }
        ci.cancel();
    }
}
