package com.tacz.guns.compat.firstperson;

import com.tacz.guns.api.client.other.KeepingItemRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public final class FirstPersonAnimationCompat {
    private FirstPersonAnimationCompat() {
    }

    public static void init() {
    }

    public static ItemStack getMainRenderStack(LocalPlayer player) {
        ItemStack kept = KeepingItemRenderer.getRenderer().getCurrentItem();
        return kept.isEmpty() ? player.getMainHandItem() : kept;
    }

    public static boolean shouldVanillaRenderArms() {
        return true;
    }

    public static boolean isTaczViewmodel(ItemStack stack) {
        return com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry.INSTANCE.get(stack.getItem())
                instanceof com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer<?, ?>;
    }
}

