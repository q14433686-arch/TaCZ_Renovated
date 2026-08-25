package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.compat.iris.IrisScopeMaskState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the TACZ scope-mask uniforms immediately before a GL draw.
 *
 * <p>{@code GlCommandEncoder#trySetup} is a better semantic hook, but its
 * descriptor has already moved; drawing is the last moment the current program
 * and pipeline are both known.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlRenderPass")
public abstract class IrisGlRenderPassMixin {
    @Inject(method = "drawIndexed", at = @At("HEAD"), require = 0)
    private void tacz$applyScopeMaskBeforeDraw(CallbackInfo ci) {
        IrisScopeMaskState.applyToGlRenderPass(this);
    }

    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void tacz$applyScopeMaskBeforeArrayDraw(CallbackInfo ci) {
        IrisScopeMaskState.applyToGlRenderPass(this);
    }
}
