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
     * Validate the whole recipe rather than dropping bad ingredients: dropping one would
     * reduce its crafting cost. An explicitly empty materials list remains a valid recipe.
     *
     * @return a human-readable reason to reject the recipe, or {@code null} if it is valid
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
        int index = 0;
        for (GunSmithTableIngredient material : recipe.materials) {
            if (material == null) {
                // The lenient JSON reader also represents a missing array entry (such as a
                // trailing comma) as null. We cannot safely infer the intended cost.
                return "materials[" + index + "] is null (check for null, a missing entry, or a trailing comma)";
            }
            index++;
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
