package net.minecraft.world.level.block.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
public class BlockEntity {
    protected Level level;
    protected final BlockPos worldPosition;
    private final BlockState state;
    public int parentLoads, parentSaves, parentRemovals;
    public BlockEntity(Object type, BlockPos pos, BlockState state) { this.worldPosition = pos; this.state = state; }
    public void setLevel(Level level) { this.level = level; }
    public BlockState getBlockState() { return state; }
    public void setChanged() {}
    protected void loadAdditional(ValueInput input) { parentLoads++; }
    protected void saveAdditional(ValueOutput output) { parentSaves++; }
    public void preRemoveSideEffects(BlockPos pos, BlockState state) { parentRemovals++; }
    public Packet<ClientGamePacketListener> getUpdatePacket() { return null; }
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return new CompoundTag(); }
    public CompoundTag saveWithoutMetadata(HolderLookup.Provider provider) { return new CompoundTag(); }
}
