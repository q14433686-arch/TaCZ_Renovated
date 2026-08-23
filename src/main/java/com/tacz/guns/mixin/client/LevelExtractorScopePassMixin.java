package com.tacz.guns.mixin.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopePipRenderer;
import com.tacz.guns.compat.iris.IrisScopePipelineCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 镜内那一遍期间不允许触发「全量重载」——它会让 Voxy 把自己重新绑到<b>错误的</b>渲染管线上，
 * 也会让 LevelRenderer / ChunkBuilder 停止并重启工作线程。
 */
@Mixin(targets = {
        "net.minecraft.client.renderer.extract.LevelExtractor",
        "net.minecraft.client.renderer.LevelRenderer"
})
public abstract class LevelExtractorScopePassMixin {

    @Unique
    private static boolean tacz$logged;

    @Inject(method = "allChanged", at = @At("HEAD"), cancellable = true, require = 0)
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
