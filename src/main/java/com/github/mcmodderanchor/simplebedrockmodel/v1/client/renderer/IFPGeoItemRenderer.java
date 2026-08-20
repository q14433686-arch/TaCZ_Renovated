package com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Stub of SimpleBedrockModel's first-person geo renderer contract. */
public interface IFPGeoItemRenderer {
    long getPutAwayDuration(ItemStack stack);
    @Nullable
    IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity);
    boolean isSameItem(ItemStack oldStack, ItemStack newStack);
    boolean blockOffhandRender();
}
