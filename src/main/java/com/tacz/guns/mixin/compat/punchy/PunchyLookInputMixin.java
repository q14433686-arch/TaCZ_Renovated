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
 * Punchy applies its own look-lag / FOV-compensated mouse scale. On a TACZ
 * ADS viewmodel that compensation runs on hip-fire FOV (or undoes TACZ's MDV
 * scale), which is the "ADS sensitivity is too high" report.
 */
@Pseudo
@Mixin(targets = {
        "punchy.client.input.MouseController",
        "punchy.client.input.PunchyMouseHandler",
        "punchy.client.state.LookStateMachine",
        "punchy.client.render.CameraLagController"
}, remap = false)
public abstract class PunchyLookInputMixin {
    @Inject(method = {
            "modifyMouseDelta",
            "scaleMouse",
            "applyLookLag",
            "applyCameraLag",
            "compensateFov",
            "modifyFov"
    }, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void tacz$skipLookCompensation(CallbackInfo ci) {
        if (FirstPersonAnimationCompat.shouldUseTaczRenderer(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }

    @Inject(method = {
            "getSensitivityMultiplier",
            "getFovCompensation",
            "getLookLag"
    }, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void tacz$identityLookScale(CallbackInfoReturnable<Float> cir) {
        if (FirstPersonAnimationCompat.shouldUseTaczRenderer(Minecraft.getInstance().player)) {
            cir.setReturnValue(1.0F);
        }
    }
}
