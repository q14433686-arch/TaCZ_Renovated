package com.tacz.guns.crafting;
import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
public final class GunSmithTableIngredient {
    public final JsonElement raw;
    public final int count;
    public int resolutions;
    public GunSmithTableIngredient(JsonElement raw, int count) { this.raw = raw; this.count = count; }
    public void resolve(RegistryAccess registry) { resolutions++; }
}
