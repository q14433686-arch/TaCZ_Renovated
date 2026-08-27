package com.tacz.guns.mixin.client;

import me.xjqsh.lrtactical.client.input.UsePressGate;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels the vanilla held-use restart after one LRTactical item use consumes the press. */
@Mixin(Minecraft.class)
public abstract class MinecraftUseRestartMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void lr$blockHeldUseRestart(CallbackInfo ci) {
        if (UsePressGate.shouldBlockRestart()) {
            ci.cancel();
        }
    }
}
