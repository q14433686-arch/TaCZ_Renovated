package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, GunMod.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, GunMod.MOD_ID);
    public static final DeferredRegister<RecipeBookCategory> RECIPE_BOOK_CATEGORIES =
            DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, GunMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GunSmithTableRecipe>> GUN_SMITH_TABLE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("gun_smith_table_crafting", GunSmithTableSerializer::create);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GunSmithTableRecipe>> GUN_SMITH_TABLE_CRAFTING =
            RECIPE_TYPES.register("gun_smith_table_crafting", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return GunMod.MOD_ID + ":gun_smith_table_crafting";
                }
            });

    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> GUN_SMITH_TABLE_CATEGORY =
            RECIPE_BOOK_CATEGORIES.register("gun_smith_table", RecipeBookCategory::new);
}
