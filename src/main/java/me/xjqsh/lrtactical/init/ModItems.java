package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.item.ThrowableItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * 物品注册。
 *
 * <h2>与 NeoForge 版的差异</h2>
 * NeoForge 用 {@code DeferredRegister} 延迟注册；Fabric 直接 {@code Registry.register}。
 * 写法沿用本仓库 {@code com.tacz.guns.init.ModItems} 的既有模式，保持全仓一致。
 *
 * <p><b>26.2 硬性要求</b>：{@code Item.Properties} 必须通过 {@code setId(ResourceKey)}
 * 带上注册键，否则注册期直接报错。故此处提供 {@link #itemProps(String)} helper，
 * 与 TACZ 侧同名方法作用相同。
 *
 * <p>当前注册并接通四个数据驱动基础物品：投掷物、近战武器、消耗品与遥控起爆器。
 * 原作的 {@code flash_shield} 仍未移植，因此这里不注册一个无法使用的空壳物品。</p>
 */
public final class ModItems {
    /**
     * 26.1 NeoForge：{@code new Item(...)} 的构造器会立即向注册表写 intrusive holder，
     * 而 mod 构造期注册表已冻结 —— 物品只能在 {@code RegisterEvent} 窗口内构造。
     * 因此字段不是 final，由 {@link #register(IEventBus)} 在窗口内填充（引用点无需改动）。
     */
    public static ThrowableItem THROWABLE;
    public static me.xjqsh.lrtactical.item.MeleeItem MELEE;
    public static me.xjqsh.lrtactical.item.DetonatorItem DETONATOR;
    public static me.xjqsh.lrtactical.item.ConsumableItem CONSUMABLE;

    private ModItems() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(net.neoforged.neoforge.registries.RegisterEvent.class, event -> {
            if (event.getRegistryKey() != Registries.ITEM) {
                return;
            }
            THROWABLE = register("throwable", new ThrowableItem(itemProps("throwable")));
            MELEE = register("melee", new me.xjqsh.lrtactical.item.MeleeItem(itemProps("melee")));
            DETONATOR = register("detonator", new me.xjqsh.lrtactical.item.DetonatorItem(itemProps("detonator")));
            CONSUMABLE = register("consumable", new me.xjqsh.lrtactical.item.ConsumableItem(itemProps("consumable")));
        });
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(EquipmentMod.MOD_ID, name));
    }

    private static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(itemKey(name));
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(EquipmentMod.MOD_ID, name), item);
    }
}
