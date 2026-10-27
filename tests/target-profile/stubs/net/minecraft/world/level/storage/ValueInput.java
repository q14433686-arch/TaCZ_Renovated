package net.minecraft.world.level.storage;
import com.mojang.serialization.Codec;
import java.util.Optional;
public interface ValueInput {
    String getStringOr(String key, String fallback);
    <T> Optional<T> read(String key, Codec<T> codec);
}
