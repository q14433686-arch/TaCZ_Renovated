package net.minecraft.world.level.block.state;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
public final class BlockState {
    private final Map<Property<?>, Object> values = new HashMap<>();
    public <T> BlockState setValue(Property<T> property, T value) { values.put(property, value); return this; }
    @SuppressWarnings("unchecked") public <T> T getValue(Property<T> property) { return (T) values.get(property); }
    public boolean hasProperty(Property<?> property) { return values.containsKey(property); }
    public Block getBlock() { return new Block(); }
}
