package com.tacz.guns.mixin.compat.punchy;

import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Punchy 2.7d injects its own {@code ItemInHandRenderer} around vanilla
 * {@code submitHandsWithItems}. That path applies a second ModelView / FOV
 * after TACZ has submitted the gun, which is what makes Iris HAND ocular
 * clipping look disabled while the mask log stays clean.
 */
@Pseudo
@Mixin(targets = {
        "punchy.client.render.ItemInHandRenderer",
        "punchy.client.render.PunchyItemInHandRenderer"
}, remap = false)
public abstract class PunchyItemInHandRendererMixin {
    @Inject(method = {
            "renderFirstPersonModel",
            "renderFirstPerson",
            "applyHandTransform",
            "applyCameraLag",
            "applyLookLag",
            "applyItemFov",
            "modifyHandFov"
    }, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void tacz$yieldHandTransforms(CallbackInfo ci) {
        if (FirstPersonAnimationCompat.shouldUseTaczRenderer(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }

    @Inject(method = {
            "shouldApplyHandTransform",
            "shouldApplyCameraLag",
            "isHandPassActive"
    }, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void tacz$disableHandPassFlags(CallbackInfoReturnable<Boolean> cir) {
        if (FirstPersonAnimationCompat.shouldUseTaczRenderer(Minecraft.getInstance().player)) {
            cir.setReturnValue(false);
        }
    }
}
