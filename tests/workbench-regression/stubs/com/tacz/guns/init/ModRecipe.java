package com.tacz.guns.init;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.RecipeBookCategory;
public final class ModRecipe {
    public static final Supplier<Object> GUN_SMITH_TABLE_RECIPE_SERIALIZER = Object::new;
    public static final Supplier<Object> GUN_SMITH_TABLE_CRAFTING = Object::new;
    public static final Supplier<RecipeBookCategory> GUN_SMITH_TABLE_CATEGORY = RecipeBookCategory::new;
}
