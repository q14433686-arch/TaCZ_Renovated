package net.minecraft.world.level;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
public final class Level {
    private final Map<BlockPos, BlockState> states = new HashMap<>();
    private final BlockState air = new BlockState(new Block(), false, "air");
    public void setBlockState(BlockPos pos, BlockState state) { states.put(pos, state); }
    public BlockState getBlockState(BlockPos pos) { return states.getOrDefault(pos, air); }
}
