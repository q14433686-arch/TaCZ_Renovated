package com.tacz.guns.mixin.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.util.ResourceScanner;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Makes the vanilla RecipeManager also scan the legacy {@code recipes/} directory
 * (old gun pack layout, pre-26.x), not just the standard {@code recipe/} directory.
 *
 * <p>Gun packs written for Minecraft 1.20 and earlier place their
 * {@code minecraft:crafting_shaped} recipes in {@code data/&lt;ns&gt;/recipes/}.
 * Since 26.x the standard directory is {@code data/&lt;ns&gt;/recipe/} (singular).
 * The vanilla RecipeManager only scans the new directory, so legacy recipes are
 * invisible and custom workbench blocks from addon packs cannot be crafted in
 * survival. JEI/REI cannot show them either.</p>
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void onApplyLegacyRecipes(Map<Identifier, JsonElement> map, ResourceManager resourceManager,
                                       ProfilerFiller profiler, CallbackInfo ci) {
        // Scan the legacy 'recipes/' directory that old gun packs use.
        // RecipeManager only scans 'recipe/' (singular) by default.
        FileToIdConverter legacyConverter = FileToIdConverter.json("recipes");
        Map<Identifier, JsonElement> legacy = ResourceScanner.scanDirectory(
                resourceManager, legacyConverter, CommonAssetsManager.GSON);
        // Merge legacy recipes into the main map.
        // New directory ('recipe/') entries take priority (avoid overwriting).
        for (Map.Entry<Identifier, JsonElement> entry : legacy.entrySet()) {
            JsonElement value = entry.getValue();
            if (shouldInclude(value) && !map.containsKey(entry.getKey())) {
                map.put(entry.getKey(), value);
            }
        }
    }

    /**
     * Filters out recipes that are not valid vanilla recipe types.
     * TaCZ's own {@code tacz:gun_smith_table_crafting} recipes are handled
     * by {@link com.tacz.guns.resource.manager.TableRecipeManager} and would
     * be rejected by the vanilla RecipeManager anyway.
     */
    private static boolean shouldInclude(JsonElement element) {
        if (element == null || !element.isJsonObject()) return false;
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has("type") || !obj.get("type").isJsonPrimitive()) return false;
        String type = obj.get("type").getAsString();
        // Skip TaCZ's own custom recipe type — handled by TableRecipeManager
        if ("tacz:gun_smith_table_crafting".equals(type)) return false;
        return true;
    }
}