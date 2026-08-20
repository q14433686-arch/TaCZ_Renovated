package com.tacz.guns.crafting;

import net.minecraft.world.item.crafting.Ingredient;

/**
 * One material line of a gun-smith recipe.
 * Delayed tag parse / legacy {@code {item}/{tag}} JSON (RecipeCompat) is in
 * {@link com.tacz.guns.crafting.RecipeCompat}.
 */
public class GunSmithTableIngredient {
    private final Ingredient ingredient;
    private final int count;

    public GunSmithTableIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = Math.max(1, count);
    }

    public GunSmithTableIngredient(com.google.gson.JsonElement ignored, int count) {
        this(Ingredient.of(net.minecraft.world.item.Items.STONE), count);
    }

    public Ingredient getIngredientOrThrow() {
        return ingredient;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getCount() {
        return count;
    }
}
