package net.minecraft.world.level.block.state;
import java.util.Objects;
import net.minecraft.world.level.block.Block;
public final class BlockState {
    private final Block block;
    public final boolean root;
    public final String facing;
    public BlockState(Block block, boolean root, String facing) { this.block = block; this.root = root; this.facing = facing; }
    public Block getBlock() { return block; }
    public boolean is(Block other) { return block == other; }
    @Override public boolean equals(Object other) {
        return other instanceof BlockState state && block == state.block && root == state.root && Objects.equals(facing, state.facing);
    }
    @Override public int hashCode() { return Objects.hash(block, root, facing); }
}
