package com.tacz.guns.block;
import net.minecraft.world.level.block.state.Property;
import net.minecraft.world.phys.BlockHitResult;
public final class TargetBlock {
    public static final Property<Boolean> STAND = new Property<>();
    public static final Property<Integer> OUTPUT_POWER = new Property<>();
    public static int getRedstoneStrength(BlockHitResult hit, boolean upper) { return 1; }
}
