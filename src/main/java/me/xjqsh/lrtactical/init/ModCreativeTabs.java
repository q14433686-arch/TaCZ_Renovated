package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.api.LrTacticalAPI;
import me.xjqsh.lrtactical.api.item.IConsumable;
import me.xjqsh.lrtactical.api.item.IMeleeWeapon;
import me.xjqsh.lrtactical.api.item.IThrowable;
import me.xjqsh.lrtactical.resource.CommonAssetsManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页（NeoForge 26.2，前滚自 WP-LR2 的 DeferredRegister 实现）。
 *
 * <p>填充策略沿用 refab：先放内置测试物品兜底栈（索引未就绪时保证可见），
 * 再按数据驱动索引列出内容包条目；近战/消耗品与投掷物共用一页。
 * 无内容包时接近空页是<b>正确行为</b>（本移植不打包原作美术）。
 *
 * <p>R1 教训备注：联机时本标签内容依赖 LR 索引网络同步；tacz 侧同步完成后的
 * 创造栏重建（ClientPacketHandlers.onSyncGunPack 的 tryRebuildTabContents）
 * 会连带重建本页——LR 无需自建重建逻辑，但 LR2-7 专服验收必须验证这一点。
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EquipmentMod.MOD_ID);

    private static final Identifier[] BUILTIN_TEST_THROWABLES = new Identifier[]{
            EquipmentMod.id("test_grenade"),
            EquipmentMod.id("test_sticky_grenade"),
            EquipmentMod.id("test_smoke_grenade"),
            EquipmentMod.id("test_gas_grenade"),
            EquipmentMod.id("test_splash_grenade"),
            EquipmentMod.id("test_flashbang"),
            EquipmentMod.id("test_c4")
    };

    private static final Identifier[] BUILTIN_TEST_MELEE = new Identifier[]{
            EquipmentMod.id("test_knife")
    };

    private static final Identifier[] BUILTIN_TEST_CONSUMABLES = new Identifier[]{
            EquipmentMod.id("test_medkit")
    };

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THROWABLE_TAB =
            TABS.register("throwable", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + EquipmentMod.MOD_ID + ".throwable"))
                    .icon(() -> ModItems.THROWABLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        addBuiltinTestStacks(output);
                        LrTacticalAPI.getThrowableIndexes()
                                .forEach(index -> output.accept(index.createItemStack()));
                        LrTacticalAPI.getMeleeIndexes()
                                .forEach(index -> output.accept(index.createItemStack()));
                        LrTacticalAPI.getConsumableIndexes()
                                .forEach(index -> output.accept(index.createItemStack()));
                        output.accept(ModItems.DETONATOR.get());
                    })
                    .build());

    private static void addBuiltinTestStacks(CreativeModeTab.Output output) {
        for (Identifier id : BUILTIN_TEST_THROWABLES) {
            if (CommonAssetsManager.get().getThrowableIndex(id) != null) {
                continue;
            }
            ItemStack stack = new ItemStack(ModItems.THROWABLE.get());
            ((IThrowable) ModItems.THROWABLE.get()).setId(stack, id);
            stack.set(DataComponents.ITEM_NAME,
                    Component.translatable("item." + id.getNamespace() + "." + id.getPath()));
            output.accept(stack);
        }
        for (Identifier id : BUILTIN_TEST_MELEE) {
            if (CommonAssetsManager.get().getMeleeIndex(id) != null) {
                continue;
            }
            ItemStack stack = new ItemStack(ModItems.MELEE.get());
            ((IMeleeWeapon) ModItems.MELEE.get()).setId(stack, id);
            stack.set(DataComponents.ITEM_NAME,
                    Component.translatable("item." + id.getNamespace() + "." + id.getPath()));
            output.accept(stack);
        }
        for (Identifier id : BUILTIN_TEST_CONSUMABLES) {
            if (CommonAssetsManager.get().getConsumableIndex(id) != null) {
                continue;
            }
            ItemStack stack = new ItemStack(ModItems.CONSUMABLE.get());
            ((IConsumable) ModItems.CONSUMABLE.get()).setId(stack, id);
            stack.set(DataComponents.ITEM_NAME,
                    Component.translatable("item." + id.getNamespace() + "." + id.getPath()));
            output.accept(stack);
        }
    }

    private ModCreativeTabs() {
    }
}
