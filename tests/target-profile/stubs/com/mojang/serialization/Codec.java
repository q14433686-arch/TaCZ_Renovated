package com.mojang.serialization;
import com.google.gson.JsonElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
public final class Codec<T> {
    @SuppressWarnings("unchecked")
    public DataResult<T> parse(JsonOps ops, JsonElement json) { return new DataResult<>((T) Component.literal(json.text())); }
    public DataResult<JsonElement> encodeStart(JsonOps ops, T value) { return new DataResult<>(new JsonElement(((Component) value).getString())); }
    public DataResult<Tag> encodeStart(NbtOps ops, T value) { return new DataResult<>(new Tag()); }
}
