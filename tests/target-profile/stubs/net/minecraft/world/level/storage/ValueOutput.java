package net.minecraft.world.level.storage;
import com.mojang.serialization.Codec;
public interface ValueOutput {
    void putString(String key, String value);
    <T> void store(String key, Codec<T> codec, T value);
}
