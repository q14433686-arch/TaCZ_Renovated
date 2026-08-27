package com.tacz.guns.mixin.compat.punchy;

import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables Punchy's camera animation state while TACZ owns the first-person viewmodel.
 *
 * <p>Punchy's CameraMixin modifies Camera.rotation and basis vectors when CameraAnimationState.isActive()
 * is true. Since TACZ yields Punchy's arm renderer, CameraAnimationState is never updated/cleared,
 * leaving residual tilt/offset that severely desyncs camera view axes and ADS mouse sensitivity.</p>
 */
@Pseudo
@Mixin(targets = "punchy.client.render.CameraAnimationState", remap = false)
public abstract class PunchyCameraAnimationStateMixin {
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void tacz$suppressCameraAnimation(CallbackInfoReturnable<Boolean> cir) {
        if (FirstPersonAnimationCompat.shouldUseTaczRenderer(Minecraft.getInstance().player)) {
            cir.setReturnValue(false);
        }
    }
}
