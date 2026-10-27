package net.minecraft.world.item;
import com.mojang.serialization.Codec;
public final class ItemStack {
    public static final ItemStack EMPTY = new ItemStack("");
    public static final Codec<ItemStack> OPTIONAL_CODEC = new Codec<>();
    private final String id;
    public ItemStack(String id) { this.id = id; }
    public boolean isEmpty() { return id.isEmpty(); }
    public ItemStack copy() { return new ItemStack(id); }
    public String testId() { return id; }
}
