package com.tacz.guns.crafting;

import com.google.gson.JsonElement;
import com.tacz.guns.GunMod;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One material line of a gun-smith recipe.
 *
 * <p>Gun-pack recipes are loaded before the server has refreshed item tags. Keep legacy
 * {@code {"item":"#c:..."}} JSON until a registry access is available, then resolve it once.
 * Replacing the JSON with stone during deserialization makes every recipe appear to have the
 * wrong material and is especially dangerous because it can allow free crafting.</p>
 */
public class GunSmithTableIngredient {
    @Nullable
    private Ingredient ingredient;
    @Nullable
    private JsonElement rawIngredient;
    private final int count;
    private boolean resolutionFailed;

    public GunSmithTableIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = Math.max(1, count);
    }

    public GunSmithTableIngredient(JsonElement rawIngredient, int count) {
        this.rawIngredient = rawIngredient == null ? null : rawIngredient.deepCopy();
        this.count = Math.max(1, count);
    }

    public Ingredient getIngredientOrThrow() {
        return Objects.requireNonNull(ingredient, "Gun smith ingredient has not been resolved");
    }

    @Nullable
    public Ingredient getIngredient() {
        return ingredient;
    }

    /** Resolve this material against the current item/tag registry. */
    @Nullable
    public synchronized Ingredient resolve(RegistryAccess registryAccess) {
        if (ingredient != null || rawIngredient == null || resolutionFailed) {
            return ingredient;
        }
        try {
            JsonElement normalized = RecipeCompat.normalizeLegacyIngredient(rawIngredient);
            ingredient = Ingredient.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, registryAccess), normalized
            ).getOrThrow();
            rawIngredient = null;
        } catch (RuntimeException exception) {
            resolutionFailed = true;
            GunMod.LOGGER.warn("Failed to resolve gun smith table ingredient {}", rawIngredient, exception);
        }
        return ingredient;
    }

    public int getCount() {
        return count;
    }
}
