package com.tacz.guns.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Single-block gun smith table. Fabric 26.1.2 semantics. */
public class GunSmithTableBlockA extends AbstractGunSmithTableBlock {
    public GunSmithTableBlockA(Properties props) {
        super(props);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public boolean isRoot(BlockState blockState) {
        return true;
    }

    @Override
    public BlockPos getRootPos(BlockPos pos, BlockState blockState) {
        return pos;
    }
}
