package com.tacz.guns.crafting;

import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class GunSmithTableRecipe implements Recipe<SingleRecipeInput> {
    private final Identifier id;
    private final GunSmithTableResult result;
    private final List<GunSmithTableIngredient> inputs;

    public GunSmithTableRecipe(Identifier id, GunSmithTableResult result, List<GunSmithTableIngredient> inputs) {
        // Work on a snapshot: POJO changes must not remove a cost or insert a null while
        // this recipe is displayed or crafted. The copy also accepts immutable List.of inputs.
        List<GunSmithTableIngredient> snapshot = inputs == null ? null : new ArrayList<>(inputs);
        if (snapshot == null || snapshot.contains(null)) {
            throw new IllegalArgumentException("Invalid materials for gun smith table recipe " + id);
        }
        this.id = id;
        this.result = result;
        this.inputs = snapshot;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    /**
     * Prevents the vanilla recipe book from syncing this recipe to the client:
     * it has no displayable ingredients and only serves the gun smith table.
     * Without this, the client recipe book logs
     * "Recipe ... can't be placed due to empty ingredients" for it.
     * (NeoForge 1.21.11 docs, Custom Recipes: "When true, will prevent the
     * recipe from being synced within the recipe book or awarded on use/unlock.")
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return (RecipeSerializer<? extends Recipe<SingleRecipeInput>>) (RecipeSerializer<?>) ModRecipe.GUN_SMITH_TABLE_RECIPE_SERIALIZER.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return (RecipeType<? extends Recipe<SingleRecipeInput>>) (RecipeType<?>) ModRecipe.GUN_SMITH_TABLE_CRAFTING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipe.GUN_SMITH_TABLE_CATEGORY.get();
    }

    public List<GunSmithTableIngredient> getInputs() {
        return inputs;
    }

    public GunSmithTableResult getResult() {
        return result;
    }

    public Identifier getTab() {
        return result.getGroup();
    }

    public Identifier getId() {
        return this.id;
    }

    public GunSmithTableRecipe(Identifier id, TableRecipe tableRecipe) {
        this(id, tableRecipe.getResult(), tableRecipe.getMaterials());
    }

    public void init() {
        result.init();
    }

    /** Resolve delayed tag ingredients after the level registry has finished loading. */
    public void resolveIngredients(RegistryAccess registryAccess) {
        for (GunSmithTableIngredient input : inputs) {
            input.resolve(registryAccess);
        }
    }

    public ItemStack getOutput() {
        return result.getResult();
    }
}
