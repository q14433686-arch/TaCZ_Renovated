package com.tacz.guns.mixin.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.GunMod;
import com.tacz.guns.util.ResourceScanner;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Makes {@link RecipeManager} also scan the legacy {@code recipes/} directory
 * (used by gun packs written for Minecraft 1.20 and earlier) in addition to
 * the standard {@code recipe/} directory.
 *
 * <p>Old gun packs place their {@code minecraft:crafting_shaped} recipes in
 * {@code data/&lt;ns&gt;/recipes/} (plural).  Minecraft 26.x uses the singular
 * {@code data/&lt;ns&gt;/recipe/}.  Without this mixin those legacy recipes are
 * invisible, so custom workbench blocks from addon packs cannot be crafted in
 * survival, and JEI/REI cannot show them either.</p>
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",
            at = @At("RETURN"))
    private void onPrepare(ResourceManager resourceManager, ProfilerFiller profiler,
                           CallbackInfoReturnable<Map<Identifier, JsonElement>> cir) {
        Map<Identifier, JsonElement> map = cir.getReturnValue();
        // Scan the legacy recipes/ directory
        FileToIdConverter legacyConverter = FileToIdConverter.json("recipes");
        Map<Identifier, JsonElement> legacy = ResourceScanner.scanDirectory(
                resourceManager, legacyConverter, new Gson());
        if (legacy.isEmpty()) {
            return;
        }
        // Merge legacy results into the main map, converting old format to new
        Map<Identifier, JsonElement> merged = null;
        for (Map.Entry<Identifier, JsonElement> entry : legacy.entrySet()) {
            if (map.containsKey(entry.getKey())) {
                continue; // new-format recipe wins
            }
            JsonElement converted = convertOldRecipe(entry.getValue());
            if (converted != null) {
                if (merged == null) {
                    merged = new LinkedHashMap<>(map);
                }
                merged.put(entry.getKey(), converted);
            }
        }
        if (merged != null) {
            cir.setReturnValue(merged);
            int count = merged.size() - map.size();
            if (count > 0) {
                GunMod.LOGGER.info("[TACZ] Injected {} legacy recipe(s) from 'recipes/' directory", count);
            }
        }
    }

    /**
     * Converts an old-format ({@code "item"} + {@code "nbt"}) recipe to the
     * 26.x format ({@code "id"} + {@code "components"}.{@code minecraft:custom_data}).
     * Returns {@code null} if the element is not a convertable old-format recipe.
     */
    @Unique
    private static JsonElement convertOldRecipe(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        // Only handle minecraft:crafting_shaped and minecraft:crafting_shapeless
        JsonElement typeEl = obj.get("type");
        if (typeEl == null || !typeEl.isJsonPrimitive()) return null;
        String type = typeEl.getAsString();
        if (!"minecraft:crafting_shaped".equals(type) && !"minecraft:crafting_shapeless".equals(type)) {
            return null;
        }
        // Must have a result with "item" field (old format)
        if (!obj.has("result") || !obj.get("result").isJsonObject()) return null;
        JsonObject result = obj.getAsJsonObject("result");
        if (!result.has("item")) return null; // already new format, skip
        // Build converted result object
        JsonObject newResult = new JsonObject();
        newResult.addProperty("id", result.get("item").getAsString());
        if (result.has("nbt") && result.get("nbt").isJsonObject()) {
            JsonObject components = new JsonObject();
            components.add("minecraft:custom_data", result.getAsJsonObject("nbt"));
            newResult.add("components", components);
        }
        // Rebuild the full recipe JSON with the new result
        JsonObject out = new JsonObject();
        out.addProperty("type", type);
        out.add("result", newResult);
        for (Map.Entry<String, JsonElement> field : obj.entrySet()) {
            String key = field.getKey();
            if (!"type".equals(key) && !"result".equals(key)) {
                out.add(key, field.getValue());
            }
        }
        return out;
    }
}