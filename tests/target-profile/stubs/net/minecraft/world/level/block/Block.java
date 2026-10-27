package net.minecraft.world.level.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
public class Block {
    public static final int UPDATE_ALL = 3;
    public static void popResource(Level level, BlockPos pos, ItemStack stack) { level.recordDrop(pos, stack); }
}
