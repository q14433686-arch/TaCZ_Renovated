package net.minecraft.core;
public record BlockPos(int x, int y, int z) {
    public BlockPos below() { return new BlockPos(x, y - 1, z); }
    public BlockPos above() { return new BlockPos(x, y + 1, z); }
    public BlockPos relative(Direction d) { return new BlockPos(x + d.getStepX(), y, z + d.getStepZ()); }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}
