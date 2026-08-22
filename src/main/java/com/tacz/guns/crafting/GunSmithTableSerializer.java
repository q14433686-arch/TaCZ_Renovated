package com.tacz.guns.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tacz.guns.crafting.result.GunSmithTableResult;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 26.1.2 codec for the legacy TACZ gun-smith recipe JSON.
 * Semantics copied from Fabric 26.1.2 {@code GunSmithTableSerializer} (RecipeCompat).
 */
public final class GunSmithTableSerializer {
    private static final Codec<GunSmithTableIngredient> INGREDIENT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("item").forGetter(GunSmithTableIngredient::getIngredientOrThrow),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(GunSmithTableIngredient::getCount)
            ).apply(instance, GunSmithTableIngredient::new)
    );

    private static final Codec<Map<String, Identifier>> ATTACHMENTS_CODEC =
            Codec.unboundedMap(Codec.STRING, Identifier.CODEC);

    private static final Codec<Identifier> GROUP_CODEC = Codec.STRING.xmap(
            RecipeCompat::parseGroup,
            Identifier::toString
    );

    private record ResultSpec(String type,
                              Identifier id,
                              int count,
                              int ammoCount,
                              Optional<Identifier> group,
                              Map<String, Identifier> attachments) {
        private static final Codec<ResultSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(ResultSpec::type),
                Identifier.CODEC.optionalFieldOf("id", Identifier.withDefaultNamespace("air")).forGetter(ResultSpec::id),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ResultSpec::count),
                Codec.INT.optionalFieldOf("ammo_count", 0).forGetter(ResultSpec::ammoCount),
                GROUP_CODEC.optionalFieldOf("group").forGetter(ResultSpec::group),
                ATTACHMENTS_CODEC.optionalFieldOf("attachments", Map.of()).forGetter(ResultSpec::attachments)
        ).apply(instance, ResultSpec::new));

        GunSmithTableResult toResult() {
            return new GunSmithTableResult(ItemStack.EMPTY, group.orElse(null));
        }

        static ResultSpec fromRecipe(GunSmithTableRecipe recipe) {
            return new ResultSpec(
                    GunSmithTableResult.CUSTOM,
                    Identifier.withDefaultNamespace("air"),
                    1,
                    0,
                    Optional.ofNullable(recipe.getResult().getGroup()),
                    Map.of()
            );
        }
    }

    public static final MapCodec<GunSmithTableRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResultSpec.CODEC.fieldOf("result").forGetter(ResultSpec::fromRecipe),
                    INGREDIENT_CODEC.listOf().fieldOf("materials").forGetter(GunSmithTableRecipe::getInputs)
            ).apply(instance, (result, materials) ->
                    new GunSmithTableRecipe(result.id(), result.toResult(), materials))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public GunSmithTableRecipe decode(RegistryFriendlyByteBuf buffer) {
                    Identifier recipeId = buffer.readIdentifier();
                    int size = buffer.readInt();
                    List<GunSmithTableIngredient> ingredients = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        ingredients.add(new GunSmithTableIngredient(
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readInt()));
                    }
                    ItemStack resultItem = ItemStack.STREAM_CODEC.decode(buffer);
                    Identifier group = buffer.readIdentifier();
                    return new GunSmithTableRecipe(recipeId, new GunSmithTableResult(resultItem, group), ingredients);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, GunSmithTableRecipe recipe) {
                    buffer.writeIdentifier(recipe.getId());
                    buffer.writeInt(recipe.getInputs().size());
                    for (GunSmithTableIngredient ingredient : recipe.getInputs()) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.getIngredientOrThrow());
                        buffer.writeInt(ingredient.getCount());
                    }
                    ItemStack.STREAM_CODEC.encode(buffer, recipe.getResult().getResult());
                    buffer.writeIdentifier(recipe.getResult().getGroup());
                }
            };

    private GunSmithTableSerializer() {
    }

    public static RecipeSerializer<GunSmithTableRecipe> create() {
        // 1.21.11：vanilla RecipeSerializer 是接口（codec/streamCodec 两个抽象方法），
        // 不再有 26.1 的 (MapCodec, StreamCodec) 构造器。
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<GunSmithTableRecipe> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
