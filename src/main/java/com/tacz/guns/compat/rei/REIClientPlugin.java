package com.tacz.guns.compat.rei;

import com.tacz.guns.compat.rei.category.AmmoQueryCategory;
import com.tacz.guns.compat.rei.category.AttachmentQueryCategory;
import com.tacz.guns.compat.rei.category.GunSmithTableCategory;
import com.tacz.guns.compat.rei.display.AmmoQueryDisplay;
import com.tacz.guns.compat.rei.display.AttachmentQueryDisplay;
import com.tacz.guns.compat.rei.display.GunSmithTableDisplay;
import com.tacz.guns.compat.rei.entry.AttachmentQueryEntry;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.compat.recipeviewer.AmmoQueryEntry;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@me.shedaniel.rei.forge.REIPluginClient
public class REIClientPlugin implements me.shedaniel.rei.api.client.plugins.REIClientPlugin {
    public static final CategoryIdentifier<AttachmentQueryDisplay> ATTACHMENT_QUERY =
            CategoryIdentifier.of(GunMod.MOD_ID, "plugins/attachment_query");
    public static final CategoryIdentifier<AmmoQueryDisplay> AMMO_QUERY =
            CategoryIdentifier.of(GunMod.MOD_ID, "plugins/ammo_query");

    public static final Map<Identifier, CategoryIdentifier<GunSmithTableDisplay>> displays = new HashMap<>();

    @Override
    public void registerCategories(CategoryRegistry registry) {
        // REI can recreate this plugin after an on-server datapack reload. Do not retain
        // workbench identifiers removed by the newly synchronized gun pack.
        displays.clear();
        var map = TimelessAPI.getAllCommonBlockIndex();
        for (var entry : map) {
            BlockItem item = entry.getValue().getBlock();
            ItemStack icon = BlockItemBuilder.create(item).setId(entry.getKey()).build();
            CategoryIdentifier<GunSmithTableDisplay> id = CategoryIdentifier.of(GunMod.MOD_ID, "plugins/gun_smith_table/" + entry.getKey().toString().replace(':', '_'));
            // 26.3：Component.translatable 已不可用（姊妹仓 abfaeffd 同步），
            // 改用物品自身的名称组件 —— 显示同一翻译键。
            registry.add(new GunSmithTableCategory(item.getName(icon), icon, id));
            displays.put(entry.getKey(), id);
            registry.addWorkstations(id, EntryStacks.of(icon));
        }
        registry.add(new AttachmentQueryCategory());
        registry.add(new AmmoQueryCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        // Gun-pack materials are intentionally lazy while tags are loading. This must use the
        // active client RegistryAccess before GunSmithTableDisplay converts inputs to REI entries;
        // EntryIngredients.ofIngredient does not accept a null delayed ingredient.
        RegistryAccess registryAccess = minecraft.level.registryAccess();
        List<GunSmithTableRecipe> recipes = new java.util.ArrayList<>();
        for (var e : com.tacz.guns.resource.CommonAssetsManager.get().getAllTableRecipes()) {
            if (e.getValue() != null && e.getValue().getResult() != null) {
                GunSmithTableRecipe r = new GunSmithTableRecipe(e.getKey(), e.getValue());
                try {
                    r.init();
                    r.resolveIngredients(registryAccess);
                } catch (RuntimeException ex) {
                    GunMod.LOGGER.error("Failed to prepare gun smith table recipe {} for REI, skipping it", e.getKey(), ex);
                    continue;
                }
                recipes.add(r);
            }
        }

        for (var entry : displays.entrySet()) {
            TimelessAPI.getCommonBlockIndex(entry.getKey()).ifPresent(blockIndex -> {
                List<GunSmithTableRecipe> recipeList = blockIndex.getFilter().filter(recipes, GunSmithTableRecipe::getId);
                recipeList.removeIf(recipe ->
                        blockIndex.getData().getTabs().stream().noneMatch(tab -> Objects.equals(tab.id(), recipe.getResult().getGroup())));
                for (GunSmithTableRecipe recipe : recipeList) {
                    try {
                        registry.add(new GunSmithTableDisplay(recipe, entry));
                    } catch (RuntimeException ex) {
                        // A malformed third-party recipe must not prevent the later attachment
                        // and ammo-query displays from registering.
                        GunMod.LOGGER.error("Failed to register gun smith table recipe {} for REI, skipping it",
                                recipe.getId(), ex);
                    }
                }
            });
        }

        AttachmentQueryEntry.getAllAttachmentQueryEntries().forEach(entry ->
                registry.add(new AttachmentQueryDisplay(entry)));
        AmmoQueryEntry.getAllAmmoQueryEntries().forEach(entry ->
                registry.add(new AmmoQueryDisplay(entry)));
    }
}
