package com.tacz.guns.mixin.client;

import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores the world-versus-HUD FOV discriminator removed from NeoForge's 26.2
 * {@code ViewportEvent.ComputeFov} API.
 *
 * <p>{@code Camera#calculateFov(float)} and {@code Camera#calculateHudFov(float)} both call
 * {@code modifyFovBasedOnDeathOrFluid(float, float)}, where NeoForge posts the event. Bracketing
 * the two callers gives the event listener an exact pass identity without guessing from the FOV
 * value or relying on call order. NeoForge 26.2 now posts {@code ComputeCameraAngles} itself, so
 * this mixin intentionally no longer posts a duplicate camera-angle event.</p>
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov(F)F", at = @At("HEAD"))
    private void tacz$beginWorldFovPass(float partialTick, CallbackInfoReturnable<Float> callback) {
        CameraSetupEvent.beginWorldFovPass();
    }

    @Inject(method = "calculateFov(F)F", at = @At("RETURN"))
    private void tacz$endWorldFovPass(float partialTick, CallbackInfoReturnable<Float> callback) {
        CameraSetupEvent.endFovPass();
    }

    @Inject(method = "calculateHudFov(F)F", at = @At("HEAD"))
    private void tacz$beginItemFovPass(float partialTick, CallbackInfoReturnable<Float> callback) {
        CameraSetupEvent.beginItemFovPass();
    }

    @Inject(method = "calculateHudFov(F)F", at = @At("RETURN"))
    private void tacz$endItemFovPass(float partialTick, CallbackInfoReturnable<Float> callback) {
        CameraSetupEvent.endFovPass();
    }
}
