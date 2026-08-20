package com.tacz.guns.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.tacz.guns.GunMod;

import net.minecraft.resources.Identifier;

/**
 * Fabric 26.1.2 RecipeCompat semantics (codec-era recipes, no {@code ShapedRecipe#itemStackFromJson}).
 *
 * <ul>
 *   <li>{@code result.item} → {@code result.id}; NBT → components (handled by callers).</li>
 *   <li>{@code recipes/} (legacy gun packs) vs {@code recipe/} (26.x datapack layout).</li>
 *   <li>Ingredient strings only at the vanilla codec layer; object {@code {item}/{tag}}
 *       rewritten to {@code "id"} / {@code "#tag"}.</li>
 *   <li>{@code result.group} bare names default to {@code tacz:} not {@code minecraft:}.</li>
 * </ul>
 *
 * ① {@code ShapedRecipe} in 26.1.2 client.jar has {@code MAP_CODEC}/{@code STREAM_CODEC}/{@code SERIALIZER}
 * only — no JSON parse mixin target.
 */
public final class RecipeCompat {
    private RecipeCompat() {
    }

    /** Bare group names get the tacz namespace, matching upstream GunSmithTableResult#decode. */
    public static Identifier parseGroup(String raw) {
        return Identifier.parse(raw.contains(":") ? raw : GunMod.MOD_ID + ":" + raw);
    }

    /**
     * {@code {"tag":"forge:ingots/iron"}} → {@code "#forge:ingots/iron"};
     * {@code {"item":"minecraft:flint"}} → {@code "minecraft:flint"}.
     */
    public static JsonElement normalizeLegacyIngredient(JsonElement raw) {
        if (raw.isJsonObject()) {
            JsonObject obj = raw.getAsJsonObject();
            if (obj.has("type")) {
                return raw;
            }
            JsonElement tag = obj.get("tag");
            if (tag != null && tag.isJsonPrimitive() && tag.getAsJsonPrimitive().isString()) {
                return new JsonPrimitive("#" + tag.getAsString());
            }
            JsonElement item = obj.get("item");
            if (item != null && item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                return item;
            }
            return raw;
        }
        if (raw.isJsonArray()) {
            JsonArray src = raw.getAsJsonArray();
            JsonArray out = new JsonArray(src.size());
            boolean changed = false;
            for (JsonElement e : src) {
                JsonElement n = normalizeLegacyIngredient(e);
                changed |= n != e;
                out.add(n);
            }
            return changed ? out : raw;
        }
        return raw;
    }

    public static boolean isLegacyRecipesDirectory(String path) {
        return path.contains("/recipes/") && !path.contains("/recipe/");
    }
}
