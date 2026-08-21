package com.tacz.guns.compat.controllable;

import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Optional Controllable (MrCrayfish) compat: controller bindings + gun-fire rumble.
 * Runtime modid {@code controllable}; compile classpath
 * {@code curse.maven:controllable-317269:8403602} (Controllable 0.26.1, NeoForge 26.2).
 */
public class ControllableCompat {
    private static final String MOD_ID = "controllable";
    private static volatile boolean installed;

    public static void init() {
        installed = ModList.get().isLoaded(MOD_ID);
        if (installed) {
            ControllableInner.init();
        }
    }

    public static void onGunShoot(ItemStack gunItem, FireMode fireMode) {
        if (installed) {
            ControllableInner.rumbleShoot(gunItem, fireMode);
        }
    }
}
