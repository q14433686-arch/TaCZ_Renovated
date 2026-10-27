package net.minecraft.world.level;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
public final class Level {
    public record Drop(BlockPos pos, String item) {}
    public final List<Drop> drops = new ArrayList<>();
    public int updates;
    private final boolean client;
    public Level(boolean client) { this.client = client; }
    public boolean isClientSide() { return client; }
    public long getGameTime() { return 1L; }
    public Random getRandom() { return new Random(1L); }
    public BlockState getBlockState(BlockPos pos) { return new BlockState(); }
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState state, int flags) { updates++; }
    public boolean setBlock(BlockPos pos, BlockState state, int flags) { return true; }
    public void scheduleTick(BlockPos pos, Block block, int ticks) {}
    public void playSound(Object player, BlockPos pos, Object sound, SoundSource source, float volume, float pitch) {}
    public void addParticle(Object particle, double x, double y, double z, double dx, double dy, double dz) {}
    public void recordDrop(BlockPos pos, ItemStack item) { drops.add(new Drop(pos, item.testId())); }
}
