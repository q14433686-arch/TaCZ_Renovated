package com.tacz.guns.mixin.client;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge already fires {@link ViewportEvent.ComputeFov} from ClientHooks#getFieldOfView
 * (26.1.2.97 sources). Camera angles are NOT posted by NeoForge, so we fire
 * ComputeCameraAngles here. HUD FOV (calculateHudFov) is also not hooked by NF.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true)
    private void tacz$modifyHandFov(float partialTick, CallbackInfoReturnable<Float> cir) {
        Camera self = (Camera) (Object) this;
        ViewportEvent.ComputeFov event = new ViewportEvent.ComputeFov(
                Minecraft.getInstance().gameRenderer, self, partialTick, cir.getReturnValue(), false);
        NeoForge.EVENT_BUS.post(event);
        cir.setReturnValue(event.getFOV());
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void tacz$applyCameraAnimations(DeltaTracker deltaTracker, CallbackInfo ci) {
        // Camera.update() also runs while the title screen is active. During that
        // phase Camera has no Level, but getCameraEntityPartialTicks() consults
        // the Level's tick-rate manager. Do not dispatch world camera events until
        // the client has an active level, otherwise entering the game crashes on
        // the first render frame with an NPE.
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Camera self = (Camera) (Object) this;
        float partialTick = self.getCameraEntityPartialTicks(deltaTracker);
        ViewportEvent.ComputeCameraAngles event = new ViewportEvent.ComputeCameraAngles(
                self, partialTick, self.yRot(), self.xRot(), 0.0F);
        NeoForge.EVENT_BUS.post(event);
        this.setRotation(event.getYaw(), event.getPitch());
        if (event.getRoll() != 0.0F) {
            self.rotation().mul(Axis.ZP.rotationDegrees(event.getRoll()));
        }
    }
}
