package com.tacz.guns.crafting;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.GunMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One material line of a gun-smith recipe.
 *
 * <p>Gun-pack recipes are loaded before the server has refreshed item tags. Keep legacy
 * {@code {"item":"#c:..."}} JSON until a registry access is available, then resolve it lazily.
 * Replacing the JSON with stone during deserialization makes every recipe appear to have the
 * wrong material and can allow free crafting.</p>
 */
public class GunSmithTableIngredient {
    @Nullable
    private Ingredient ingredient;
    @Nullable
    private JsonElement rawIngredient;
    private final int count;
    private boolean loggedFailure;

    public GunSmithTableIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = Math.max(1, count);
    }

    public GunSmithTableIngredient(JsonElement rawIngredient, int count) {
        this.rawIngredient = rawIngredient == null ? null : rawIngredient.deepCopy();
        this.count = Math.max(1, count);
    }

    public Ingredient getIngredientOrThrow() {
        return Objects.requireNonNull(getIngredient(), "Gun smith ingredient has not been resolved");
    }

    /**
     * Resolve on demand using the currently bound built-in item/tag registries. The retry behavior
     * is intentional: a first call can happen before tags are applied during a resource reload.
     */
    @Nullable
    public synchronized Ingredient getIngredient() {
        return resolve(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    /** Resolve this material against a supplied level/server registry access. */
    @Nullable
    public synchronized Ingredient resolve(RegistryAccess registryAccess) {
        if (ingredient != null || rawIngredient == null) {
            return ingredient;
        }
        JsonElement raw = rawIngredient;
        try {
            JsonElement normalized = RecipeCompat.normalizeLegacyIngredient(raw);
            // NeoForge patches Ingredient.CODEC itself to dispatch native custom ingredients
            // through neoforge:ingredient_type. Do not wrap it a second time: the current
            // 26.1.2 codec already contains IngredientCodecs.codec(...).
            ingredient = Ingredient.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, registryAccess), normalized
            ).getOrThrow();
            rawIngredient = null;
        } catch (RuntimeException exception) {
            if (!loggedFailure) {
                loggedFailure = true;
                GunMod.LOGGER.warn("Failed to resolve gun smith table ingredient {}", raw, exception);
            }
        }
        return ingredient;
    }

    public int getCount() {
        return count;
    }
}
