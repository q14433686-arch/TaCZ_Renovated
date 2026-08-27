package com.tacz.guns.mixin.compat.punchy;

import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Prevents Punchy from submitting its independent arm rig over a TACZ viewmodel
 * and clears any stale camera animation offsets.
 *
 * <p>TACZ first-person guns already attach player skin arms at authored
 * {@code left_hand}/{@code right_hand} locators. Punchy's own first-person arm
 * pass is a second rig and appears detached from the gun.</p>
 */
@Pseudo
@Mixin(targets = "punchy.client.render.PunchyArmRenderer", remap = false)
public abstract class PunchyArmRendererMixin {
    private static boolean clearAttempted;
    private static Method clearMethod;

    @Inject(method = "renderFirstPerson", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private static void tacz$yieldFirstPersonRenderer(CallbackInfo ci) {
        if (FirstPersonAnimationCompat.shouldUseTaczRenderer(Minecraft.getInstance().player)) {
            clearPunchyCameraState();
            ci.cancel();
        }
    }

    private static void clearPunchyCameraState() {
        if (!clearAttempted) {
            clearAttempted = true;
            try {
                Class<?> clazz = Class.forName("punchy.client.render.CameraAnimationState", false,
                        PunchyArmRendererMixin.class.getClassLoader());
                clearMethod = clazz.getMethod("clear");
            } catch (Throwable ignored) {
            }
        }
        if (clearMethod != null) {
            try {
                clearMethod.invoke(null);
            } catch (Throwable ignored) {
            }
        }
    }
}
