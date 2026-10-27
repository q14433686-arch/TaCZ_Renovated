package net.minecraft.world.item.crafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
public interface Recipe<T> {
    boolean matches(T input, Level level);
    boolean isSpecial();
    ItemStack assemble(T input);
    boolean showNotification();
    String group();
    RecipeSerializer<? extends Recipe<T>> getSerializer();
    RecipeType<? extends Recipe<T>> getType();
    PlacementInfo placementInfo();
    RecipeBookCategory recipeBookCategory();
}
