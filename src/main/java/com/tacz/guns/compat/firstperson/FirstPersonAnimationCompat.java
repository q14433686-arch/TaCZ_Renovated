package com.tacz.guns.compat.firstperson;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Makes generic first-person body/animation mods yield while TACZ owns the viewmodel.
 *
 * <p>The compatibility contract is one-way: ordinary items stay under the other mod's control,
 * while an animated TACZ/LRTactical {@link AnimateGeoItemRenderer} with a loaded model keeps its
 * authored gun/hand animation without a second arm rig.</p>
 *
 * <ul>
 *   <li>Punchy: no public Java disable API. Optional {@code @Pseudo} mixins route TACZ viewmodels
 *       through Punchy's supported item-blacklist / yield path. See
 *       {@code com.tacz.guns.mixin.compat.punchy}.</li>
 * </ul>
 */
public final class FirstPersonAnimationCompat {
    private FirstPersonAnimationCompat() {
    }

    public static void init() {
        if (ModList.get().isLoaded("punchy")) {
            GunMod.LOGGER.info("Punchy detected; TACZ viewmodels use the blacklist/yield mixins");
        }
    }

    /** Returns the kept/main-hand stack that TACZ will actually draw this frame. */
    public static ItemStack getMainRenderStack(LocalPlayer player) {
        ItemStack kept = KeepingItemRenderer.getRenderer().getCurrentItem();
        return kept != null && !kept.isEmpty() ? kept : player.getMainHandItem();
    }

    /** True only when TACZ has both a custom renderer and a real model to submit. */
    public static boolean isTaczViewmodel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var renderer = BuiltinItemRendererRegistry.INSTANCE.get(stack.getItem());
        return renderer instanceof AnimateGeoItemRenderer<?, ?> animated && animated.getModel(stack) != null;
    }

    public static boolean shouldUseTaczRenderer(@Nullable LocalPlayer player) {
        return player != null && isTaczViewmodel(getMainRenderStack(player));
    }

    public static boolean shouldVanillaRenderArms() {
        return !shouldUseTaczRenderer(Minecraft.getInstance().player);
    }
}
