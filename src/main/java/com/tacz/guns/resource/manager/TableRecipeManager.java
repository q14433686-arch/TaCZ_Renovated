package com.tacz.guns.resource.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.GunMod;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import com.tacz.guns.util.ResourceScanner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class TableRecipeManager extends CommonDataManager<TableRecipe> {
    private static final String RECIPE_TYPE_ID = resolveRecipeTypeId();
    private static final String LEGACY_RECIPE_DIRECTORY = "recipes";
    private static final FileToIdConverter LEGACY_CONVERTER = FileToIdConverter.json(LEGACY_RECIPE_DIRECTORY);

    private Map<Identifier, JsonElement> legacyVanillaRecipes = Map.of();

    public Map<Identifier, JsonElement> getLegacyVanillaRecipes() {
        return legacyVanillaRecipes;
    }

    public TableRecipeManager() {
        super(DataType.RECIPES, TableRecipe.class, CommonAssetsManager.GSON, "recipe", "TableRecipeLoader");
    }

    @NotNull
    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<Identifier, JsonElement> legacy =
                ResourceScanner.scanDirectory(pResourceManager, LEGACY_CONVERTER, CommonAssetsManager.GSON);
        Map<Identifier, JsonElement> current = super.prepare(pResourceManager, pProfiler);
        if (legacy.isEmpty()) {
            return current;
        }
        Map<Identifier, JsonElement> merged = new LinkedHashMap<>(legacy);
        merged.putAll(current);
        GunMod.LOGGER.info(getMarker(),
                "Found {} recipe file(s) in legacy 'recipes/' directory (old gun pack layout), {} in 'recipe/'.",
                legacy.size(), current.size());
        return merged;
    }

    private static String resolveRecipeTypeId() {
        Identifier id = BuiltInRegistries.RECIPE_TYPE.getKey(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());
        return id != null ? id.toString() : GunMod.MOD_ID + ":gun_smith_table_crafting";
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<Identifier, JsonElement> ours = new LinkedHashMap<>();
        Map<Identifier, JsonElement> vanillaLegacy = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry : pObject.entrySet()) {
            if (isGunSmithTableRecipe(entry.getValue())) {
                ours.put(entry.getKey(), entry.getValue());
            } else if (isVanillaCraftingRecipe(entry.getValue())) {
                vanillaLegacy.put(entry.getKey(), convertOldRecipe(entry.getValue()));
            }
        }
        this.legacyVanillaRecipes = vanillaLegacy;
        if (!vanillaLegacy.isEmpty()) {
            GunMod.LOGGER.info(getMarker(),
                    "Preserved {} vanilla recipe(s) from 'recipes/' for RecipeManager injection.",
                    vanillaLegacy.size());
        }
        GunMod.LOGGER.debug(getMarker(), "Gun smith table recipes: {} accepted, {} vanilla legacy preserved",
                ours.size(), vanillaLegacy.size());
        super.apply(ours, pResourceManager, pProfiler);
    }

    private static boolean isGunSmithTableRecipe(JsonElement element) {
        if (element == null || !element.isJsonObject()) return false;
        JsonObject object = element.getAsJsonObject();
        JsonElement type = object.get("type");
        if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) return false;
        return RECIPE_TYPE_ID.equals(type.getAsString());
    }

    private static boolean isVanillaCraftingRecipe(JsonElement element) {
        if (element == null || !element.isJsonObject()) return false;
        JsonObject obj = element.getAsJsonObject();
        JsonElement typeEl = obj.get("type");
        if (typeEl == null || !typeEl.isJsonPrimitive()) return false;
        String type = typeEl.getAsString();
        return "minecraft:crafting_shaped".equals(type) || "minecraft:crafting_shapeless".equals(type);
    }

    private static JsonElement convertOldRecipe(JsonElement element) {
        if (element == null || !element.isJsonObject()) return element;
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has("result") || !obj.get("result").isJsonObject()) return element;
        JsonObject result = obj.getAsJsonObject("result");
        if (!result.has("item")) return element;
        JsonObject newResult = new JsonObject();
        newResult.addProperty("id", result.get("item").getAsString());
        if (result.has("nbt") && result.get("nbt").isJsonObject()) {
            JsonObject components = new JsonObject();
            components.add("minecraft:custom_data", result.getAsJsonObject("nbt"));
            newResult.add("components", components);
        }
        JsonObject out = new JsonObject();
        out.addProperty("type", obj.get("type").getAsString());
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