package com.tacz.guns.mixin.client;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge 21.11.45 already fires {@link ViewportEvent.ComputeFov} from
 * {@code ClientHooks#getFieldOfView} for both world and HUD/item FOV. Posting a second HUD
 * event here made the smoothing state advance twice and produced an incorrect hand/camera
 * distance during ADS. Camera angles are not posted by NeoForge, so only that hook is injected.
 *
 * <p>1.21.11 backport: Camera has no {@code update(DeltaTracker)} — that is a 26.1 addition.
 * The equivalent hook point is {@code Camera#setup(Level, Entity, boolean, boolean, float)},
 * which runs every frame and carries the partial tick as its last parameter
 * (javap-verified against the 1.21.11 named jar; semantic source: sibling 1.21.11 branch).</p>
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void tacz$applyCameraAnimations(Level level,
                                            Entity entity,
                                            boolean detached,
                                            boolean thirdPersonReverse,
                                            float partialTick,
                                            CallbackInfo ci) {
        // Camera.setup() also runs while the title screen is active. During that
        // phase the client has no active level; do not dispatch world camera
        // events until the client has one, otherwise entering the game crashes
        // on the first render frame with an NPE.
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Camera self = (Camera) (Object) this;
        ViewportEvent.ComputeCameraAngles event = new ViewportEvent.ComputeCameraAngles(
                self, partialTick, self.yRot(), self.xRot(), 0.0F);
        NeoForge.EVENT_BUS.post(event);
        this.setRotation(event.getYaw(), event.getPitch());
        if (event.getRoll() != 0.0F) {
            self.rotation().mul(Axis.ZP.rotationDegrees(event.getRoll()));
        }
    }
}
