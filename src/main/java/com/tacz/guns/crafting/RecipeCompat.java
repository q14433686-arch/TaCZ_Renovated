package com.tacz.guns.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.tacz.guns.GunMod;

import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * NeoForge 26.1.2 RecipeCompat semantics (codec-era recipes, no {@code ShapedRecipe#itemStackFromJson}).
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
    private static final Set<String> SUPPORTED_FORGE_INGREDIENTS =
            Set.of("forge:partial_nbt", "forge:nbt");

    /**
     * The Forge-era gun packs in the wild use {@code forge:partial_nbt} and {@code forge:nbt}.
     * NeoForge 26.1 has an equivalent native custom ingredient, registered as
     * {@code tacz:partial_nbt}; convert only these two known types and leave every unknown
     * custom type untouched rather than guessing its semantics.
     */
    private static JsonElement normalizeCustomIngredient(JsonObject source) {
        JsonElement typeElement = source.get("type");
        if (typeElement == null || !typeElement.isJsonPrimitive()
                || !typeElement.getAsJsonPrimitive().isString()
                || !SUPPORTED_FORGE_INGREDIENTS.contains(typeElement.getAsString())) {
            return source;
        }

        JsonElement itemsElement = source.get("items");
        JsonArray items = new JsonArray();
        if (itemsElement != null && itemsElement.isJsonArray()) {
            itemsElement.getAsJsonArray().forEach(items::add);
        } else if (source.has("item") && source.get("item").isJsonPrimitive()) {
            items.add(source.get("item"));
        } else {
            return source;
        }

        JsonElement nbt = source.get("nbt");
        if (nbt == null || !nbt.isJsonObject()) {
            return source;
        }

        JsonObject normalized = new JsonObject();
        normalized.addProperty("neoforge:ingredient_type", "tacz:partial_nbt");
        normalized.add("items", items);
        normalized.add("nbt", nbt.deepCopy());
        if ("forge:nbt".equals(typeElement.getAsString())) {
            normalized.addProperty("strict", true);
        }
        return normalized;
    }

    public static JsonElement normalizeLegacyIngredient(JsonElement raw) {
        if (raw.isJsonObject()) {
            JsonObject obj = raw.getAsJsonObject();
            if (obj.has("type")) {
                return normalizeCustomIngredient(obj);
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
