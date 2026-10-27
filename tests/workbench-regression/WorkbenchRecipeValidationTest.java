package com.tacz.guns.resource.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exercises production parsing, validation and sync against lightweight loader stubs. */
public final class WorkbenchRecipeValidationTest {
    private static final String TYPE = "\"type\":\"tacz:gun_smith_table_crafting\",";
    private static final String INGREDIENT = "{\"item\":\"minecraft:iron_ingot\",\"count\":2}";
    private static final Identifier VALID = id("test", "valid");
    private static final Identifier EMPTY = id("test", "empty");
    // Synthetic reproduction, not the unavailable original JSON from this gun pack.
    private static final Identifier NULL_ENTRY = id("ra1k", "attachments/fast_mag");
    private static final Identifier TRAILING_COMMA = id("test", "trailing_comma");
    private static final Identifier MISSING_MATERIALS = id("test", "missing_materials");
    private static final Identifier NULL_MATERIALS = id("test", "null_materials");
    private static final Identifier MISSING_RESULT = id("test", "missing_result");
    private static final Identifier BAD_INGREDIENT = id("test", "bad_ingredient");

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private static JsonElement raw(String body) {
        return CommonAssetsManager.GSON.fromJson("{" + TYPE + body + "}", JsonElement.class);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static boolean logged(String fragment) {
        return GunMod.LOGGER.messages.stream().anyMatch(message -> message.contains(fragment));
    }

    private static void expectIllegal(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void expectJsonError(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Expected JsonParseException");
        } catch (JsonParseException expected) {
            // Expected.
        }
    }

    public static void main(String[] args) throws Exception {
        Map<Identifier, JsonElement> source = new LinkedHashMap<>();
        source.put(VALID, raw("\"result\":{},\"materials\":[" + INGREDIENT + "]"));
        source.put(EMPTY, raw("\"result\":{},\"materials\":[]"));
        source.put(NULL_ENTRY, raw("\"result\":{},\"materials\":[" + INGREDIENT + ",null]"));
        source.put(TRAILING_COMMA, raw("\"result\":{},\"materials\":[" + INGREDIENT + ",]"));
        source.put(MISSING_MATERIALS, raw("\"result\":{}"));
        source.put(NULL_MATERIALS, raw("\"result\":{},\"materials\":null"));
        source.put(MISSING_RESULT, raw("\"materials\":[" + INGREDIENT + "]"));
        source.put(BAD_INGREDIENT, raw("\"result\":{},\"materials\":[{}]"));

        // LENIENT Gson represents a trailing comma as a null element; explicit null has
        // the same tree shape, so removing it would silently guess the recipe's cost.
        check(source.get(TRAILING_COMMA).getAsJsonObject().getAsJsonArray("materials")
                .get(1).isJsonNull(), "trailing comma must become a null array entry");
        check(TableRecipe.validationError(null) != null, "null recipe rejected");
        TableRecipe nullEntry = CommonAssetsManager.GSON.fromJson(source.get(NULL_ENTRY), TableRecipe.class);
        check(TableRecipe.validationError(nullEntry).contains("materials[1]"), "null index reported");
        TableRecipe leadingNull = CommonAssetsManager.GSON.fromJson(
                raw("\"result\":{},\"materials\":[null," + INGREDIENT + "]"), TableRecipe.class);
        check(TableRecipe.validationError(leadingNull).contains("materials[0]"), "leading null rejected");
        TableRecipe empty = CommonAssetsManager.GSON.fromJson(source.get(EMPTY), TableRecipe.class);
        check(TableRecipe.validationError(empty) == null && empty.getMaterials().isEmpty(),
                "an explicitly empty ingredient list remains valid");
        var materialsField = TableRecipe.class.getDeclaredField("materials");
        materialsField.setAccessible(true);
        materialsField.set(empty, List.of());
        check(TableRecipe.validationError(empty) == null, "validator also accepts immutable empty lists");
        expectJsonError(() -> CommonAssetsManager.GSON.fromJson(source.get(BAD_INGREDIENT), TableRecipe.class));

        GunMod.LOGGER.messages.clear();
        TableRecipeManager server = new TableRecipeManager();
        server.apply(source, null, null);
        check(server.getAllData().keySet().equals(java.util.Set.of(VALID, EMPTY)),
                "server accepts the valid and explicit-empty recipes only");
        check(server.getNetworkCache().keySet().equals(java.util.Set.of(VALID, EMPTY)),
                "new server must not sync the raw JSON of rejected recipes to old clients");
        check(logged("ra1k:attachments/fast_mag") && logged("materials[1]"),
                "server logs the rejected recipe ID and a useful reason");
        check(logged("test:bad_ingredient"), "server also logs serializer failures");

        // The production CommonNetworkCache must cope with old servers that still send the
        // unfiltered raw JSON, without taking down the connection or exposing a cheaper recipe.
        Map<Identifier, String> oldServerCache = new LinkedHashMap<>();
        source.forEach((recipeId, element) -> oldServerCache.put(recipeId, element.toString()));
        GunMod.LOGGER.messages.clear();
        CommonNetworkCache.INSTANCE.fromNetwork(Map.of(DataType.RECIPES, oldServerCache));
        check(CommonNetworkCache.INSTANCE.tableRecipe.keySet().equals(java.util.Set.of(VALID, EMPTY)),
                "new client rejects invalid recipes from an old server");
        check(logged("ra1k:attachments/fast_mag") && logged("materials[1]"),
                "client logs the ID and reason for an invalid synced recipe");

        GunSmithTableResult result = new GunSmithTableResult();
        expectIllegal(() -> new GunSmithTableRecipe(VALID, result, null));
        // Vanilla codecs can supply immutable lists whose contains(null) throws.
        check(new GunSmithTableRecipe(EMPTY, result, List.of()).getInputs().isEmpty(), "immutable empty list accepted");
        List<GunSmithTableIngredient> bad = new ArrayList<>();
        bad.add(null);
        expectIllegal(() -> new GunSmithTableRecipe(VALID, result, bad));
        TableRecipe validPojo = server.getData(VALID);
        GunSmithTableIngredient ingredient = validPojo.getMaterials().get(0);
        check(new GunSmithTableRecipe(VALID, result, List.of(ingredient)).getInputs().size() == 1,
                "immutable non-empty list accepted");
        GunSmithTableRecipe snapshot = new GunSmithTableRecipe(VALID, validPojo);
        validPojo.getMaterials().clear();
        check(snapshot.getInputs().size() == 1, "later POJO mutation cannot remove a cost from an existing recipe");
        snapshot.resolveIngredients(new RegistryAccess());
        check(ingredient.resolutions == 1, "valid delayed ingredient is still resolved");
        new GunSmithTableRecipe(EMPTY, server.getData(EMPTY)).resolveIngredients(new RegistryAccess());

        System.out.println("Workbench recipe validation and sync regression passed");
    }
}
