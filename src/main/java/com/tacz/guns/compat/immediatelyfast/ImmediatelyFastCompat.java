package com.tacz.guns.compat.immediatelyfast;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Compatibility facade for ImmediatelyFast (upstream 26.2 semantics).
 *
 * <p>The old TACZ integration targeted ImmediatelyFast's pre-26.x public HUD batching API
 * ({@code ImmediatelyFastApi#getBatching}), which no longer exists. Minecraft 26.2 and current
 * ImmediatelyFast builds both use extracted/collector rendering; no manual batch break is required.
 * This no-op hook preserves call-site compatibility without claiming an active integration.</p>
 */
public final class ImmediatelyFastCompat {
    private static final String MOD_ID = "immediatelyfast";
    private static boolean installed;

    private ImmediatelyFastCompat() {
    }

    public static void init() {
        installed = ModList.get().isLoaded(MOD_ID);
    }

    public static void renderHotbarItem(ItemStack stack, boolean pre) {
        // Intentionally empty on 26.2. See class documentation.
    }

    public static boolean isInstalled() {
        return installed;
    }
}
