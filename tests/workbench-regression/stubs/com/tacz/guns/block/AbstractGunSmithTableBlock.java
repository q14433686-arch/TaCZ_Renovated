package com.tacz.guns.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
public abstract class AbstractGunSmithTableBlock extends Block {
    public abstract boolean isRoot(BlockState state);
    public abstract BlockPos getRootPos(BlockPos pos, BlockState state);
    public abstract BlockPos getCompanionPos(BlockPos pos, BlockState state);
    public abstract BlockState getCompanionState(BlockState state);
}
