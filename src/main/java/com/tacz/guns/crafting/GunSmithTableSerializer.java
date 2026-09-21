package com.tacz.guns.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.crafting.result.RawGunTableResult;
import com.tacz.guns.resource.pojo.data.recipe.GunResult;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 26.1.2 codec for the legacy TACZ gun-smith recipe JSON.
 * Semantics copied from Fabric 26.1.2 {@code GunSmithTableSerializer} (RecipeCompat).
 */
public final class GunSmithTableSerializer {
    /**
     * 材料编解码器：{@code item} 字段以<b>原始 JSON</b> 读入，交给
     * {@link GunSmithTableIngredient#GunSmithTableIngredient(JsonElement, int)} 延迟解析。
     *
     * <h2>为什么不能直接 {@code Ingredient.CODEC.fieldOf("item")}（姊妹仓 2026-09-21 实机日志）</h2>
     * <p>枪包里大量材料仍是旧写法：{@code {"tag":"c:ingots/iron"}}、
     * {@code {"type":"tacz:nbt","nbt":{...},"partial":true,"items":"tacz:attachment"}}。
     * 这些写法只有 {@link GunSmithTableIngredient#resolve} 里的
     * {@code RecipeCompat.normalizeLegacyIngredient} 会改写成 26.3 认得的形态；同一份
     * JSON 的 Gson 路径（GUI/JEI）一直走那里，所以界面正常。但本 codec 是<b>配方注册表</b>
     * 加载路径（26.3 {@code RegistryDataLoader} 把 {@code minecraft:recipe} 当动态注册表
     * 加载），此前直接把原文喂给 {@code Ingredient.CODEC}：{@code No key ... Not a string
     * ... Not a json array}。动态注册表任一元素解析失败 ⇒ {@code Failed to load registries
     * due to errors} ⇒ <b>整个存档进不去</b>（未实测：本仓；姊妹仓 26.3 实录）。</p>
     *
     * <p>{@code ExtraCodecs.JSON} 把任意值原样转成 {@code JsonElement}，后续解析、失败
     * 日志、判空语义全部与 Gson 路径统一；编码方向仍写出解析后的 Ingredient。</p>
     */
    private static final Codec<GunSmithTableIngredient> INGREDIENT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    net.minecraft.util.ExtraCodecs.JSON.fieldOf("item").forGetter(GunSmithTableSerializer::encodeIngredient),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(GunSmithTableIngredient::getCount)
            ).apply(instance, GunSmithTableIngredient::new)
    );

    /** 编码方向：能解析就写解析后的 Ingredient，否则回写原文（不让一条坏材料炸掉整表编码）。 */
    private static com.google.gson.JsonElement encodeIngredient(GunSmithTableIngredient ingredient) {
        Ingredient resolved = ingredient.getIngredient();
        if (resolved != null) {
            var result = Ingredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, resolved).result();
            if (result.isPresent()) {
                return result.get();
            }
        }
        com.google.gson.JsonElement raw = ingredient.getRawItem();
        return raw != null ? raw : com.google.gson.JsonNull.INSTANCE;
    }

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
            RawGunTableResult raw = new RawGunTableResult(type, id, Math.max(1, count));
            if (GunSmithTableResult.GUN.equals(type)) {
                EnumMap<AttachmentType, Identifier> parsedAttachments = new EnumMap<>(AttachmentType.class);
                attachments.forEach((name, attachmentId) -> {
                    try {
                        parsedAttachments.put(AttachmentType.valueOf(name.toUpperCase(java.util.Locale.ROOT)), attachmentId);
                    } catch (IllegalArgumentException ignored) {
                    }
                });
                raw.setExtraData(new GunResult(ammoCount, parsedAttachments));
            }
            return new GunSmithTableResult(raw, group.orElse(null));
        }

        static ResultSpec fromRecipe(GunSmithTableRecipe recipe) {
            ItemStack stack = recipe.getResult().getResult();
            String type = GunSmithTableResult.CUSTOM;
            Identifier id = Identifier.withDefaultNamespace("air");
            if (stack.getItem() instanceof IGun gun) {
                type = GunSmithTableResult.GUN;
                id = gun.getGunId(stack);
            } else if (stack.getItem() instanceof IAmmo ammo) {
                type = GunSmithTableResult.AMMO;
                id = ammo.getAmmoId(stack);
            } else if (stack.getItem() instanceof IAttachment attachment) {
                type = GunSmithTableResult.ATTACHMENT;
                id = attachment.getAttachmentId(stack);
            }
            return new ResultSpec(type, id, Math.max(1, stack.getCount()), 0,
                    Optional.ofNullable(recipe.getResult().getGroup()), Map.of());
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
                    ItemStack resultItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                    Identifier group = buffer.readIdentifier();
                    return new GunSmithTableRecipe(recipeId, new GunSmithTableResult(resultItem, group), ingredients);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, GunSmithTableRecipe recipe) {
                    // OnDatapackSyncEvent can request this recipe type for NeoForge's native
                    // RecipeContentPayload. Results loaded from a gun pack are intentionally
                    // lazy, so resolve them at this post-reload network boundary rather than
                    // serializing ItemStack.EMPTY into the client recipe content.
                    recipe.init();
                    buffer.writeIdentifier(recipe.getId());
                    buffer.writeInt(recipe.getInputs().size());
                    for (GunSmithTableIngredient ingredient : recipe.getInputs()) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.getIngredientOrThrow());
                        buffer.writeInt(ingredient.getCount());
                    }
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.getResult().getResult());
                    buffer.writeIdentifier(recipe.getResult().getGroup());
                }
            };

    private GunSmithTableSerializer() {
    }

    public static RecipeSerializer<GunSmithTableRecipe> create() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
