package com.tacz.guns.resource;
import com.google.gson.*;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.resource.serialize.GunSmithTableIngredientSerializer;
public final class CommonAssetsManager {
    public static final Gson GSON = new GsonBuilder().setStrictness(Strictness.LENIENT)
        .registerTypeAdapter(GunSmithTableIngredient.class, new GunSmithTableIngredientSerializer()).create();
}
