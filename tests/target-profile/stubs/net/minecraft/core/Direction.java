package net.minecraft.core;
public enum Direction {
    NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);
    private final int x, z;
    Direction(int x, int z) { this.x = x; this.z = z; }
    public int getStepX() { return x; }
    public int getStepZ() { return z; }
}
