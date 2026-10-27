package com.tacz.guns.resource.pojo.data.recipe;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TableRecipe {
    @SerializedName("materials")
    private List<GunSmithTableIngredient> materials;

    @SerializedName("result")
    private GunSmithTableResult result;

    /**
     * Validate the complete recipe rather than dropping bad ingredients: dropping one could
     * silently lower its crafting cost. An explicitly empty materials list remains valid.
     *
     * @return a rejection reason, or {@code null} when this recipe is safe to consume
     */
    @Nullable
    public static String validationError(@Nullable TableRecipe recipe) {
        if (recipe == null) {
            return "recipe is null";
        }
        if (recipe.result == null) {
            return "result is missing or null";
        }
        if (recipe.materials == null) {
            return "materials is missing or null";
        }
        for (int index = 0; index < recipe.materials.size(); index++) {
            if (recipe.materials.get(index) == null) {
                return "materials[" + index + "] is null (check for null, a missing entry, or a trailing comma)";
            }
        }
        return null;
    }

    public List<GunSmithTableIngredient> getMaterials() {
        return materials;
    }

    public GunSmithTableResult getResult() {
        return result;
    }
}
