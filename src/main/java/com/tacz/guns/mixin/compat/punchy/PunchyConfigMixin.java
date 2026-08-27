package com.tacz.guns.mixin.compat.punchy;

import com.tacz.guns.compat.firstperson.FirstPersonAnimationCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks Punchy's root item blacklist method so all Punchy subsystems (hand blacklist,
 * attack tracker, arm rendering, equip machine) treat TACZ viewmodels as blacklisted.
 */
@Pseudo
@Mixin(targets = "punchy.config.PunchyConfig", remap = false)
public abstract class PunchyConfigMixin {
    @Inject(method = "isItemBlacklisted", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private static void tacz$blacklistGunItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (FirstPersonAnimationCompat.isTaczViewmodel(stack)) {
            cir.setReturnValue(true);
        }
    }
}
