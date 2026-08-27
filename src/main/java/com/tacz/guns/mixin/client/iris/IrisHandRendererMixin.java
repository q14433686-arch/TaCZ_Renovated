package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards Iris HandRenderer against redundant, premature, or post-projection-restore
 * flushes injected by Punchy while TACZ owns the first-person viewmodel.
 *
 * <p>Punchy's IrisHandRendererCoreMixin injects calls to endRender() at
 * RenderSystem.restoreProjectionMatrix() and iris$renderHandsWithCustomRenderer.
 * When TACZ holds a gun, these calls prematurely trigger FeatureRenderDispatcher
 * after the hand projection matrix has already been replaced with the world projection matrix,
 * corrupting ScopeMaskRenderer NDC coordinates and prematurely clearing ocular geometry.</p>
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public abstract class IrisHandRendererMixin {
    @Inject(method = "endRender", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void tacz$guardPunchyEndRender(CallbackInfo ci) {
        if (FirstPersonAnimationCompat.shouldUseTaczRenderer(Minecraft.getInstance().player)) {
            boolean fromPunchy = StackWalker.getInstance()
                    .walk(frames -> frames.anyMatch(f -> f.getClassName().startsWith("punchy.")));
            if (fromPunchy) {
                ci.cancel();
            }
        }
    }
}
