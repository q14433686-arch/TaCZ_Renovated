package me.xjqsh.lrtactical.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.item.ConsumableItem;
import me.xjqsh.lrtactical.item.DetonatorItem;
import me.xjqsh.lrtactical.item.MeleeItem;
import me.xjqsh.lrtactical.item.ThrowableItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册（NeoForge 26.2）。
 *
 * <h2>WP-LR2 改写说明（对照 refab Fabric 原文）</h2>
 * refab 侧为 Fabric 直注册（静态字段 + 类加载期 {@code Registry.register}）——
 * 该形态在 NeoForge 26.1+ 即 WP07 坑 A-1（mod 构造期注册表已冻结，
 * {@code Item.<init>} 的 intrusive holder 直接抛 frozen，r28 崩溃实证）。
 * 本改写按工单 LR2-1 采用 <b>DeferredRegister 全量重写</b>，与主 mod
 * {@code com.tacz.guns.init.ModItems} 同习语。
 *
 * <p>{@code registerItem} 会自动对 {@code Item.Properties} 调
 * {@code setId(ResourceKey)}（证据：records/WP02，NeoForge
 * {@code DeferredRegister.Items} 源码），refab 手写的 {@code itemProps(name)}
 * 不再需要。
 *
 * <p><b>命名空间</b>：注册在 {@code lrtactical:} 下（内容包引用
 * {@code lrtactical:throwable} 等 id），与 refab / 原作一致。
 *
 * <p><b>消费方注意</b>：字段类型由裸 Item 变为 {@link DeferredItem}，
 * 取实例需 {@code .get()}——全部调用点在 LR2-2/3 逐一跟改（清单见
 * records/LR2_INVENTORY.md）。
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EquipmentMod.MOD_ID);

    public static final DeferredItem<ThrowableItem> THROWABLE =
            ITEMS.registerItem("throwable", ThrowableItem::new);

    public static final DeferredItem<MeleeItem> MELEE =
            ITEMS.registerItem("melee", MeleeItem::new);

    public static final DeferredItem<DetonatorItem> DETONATOR =
            ITEMS.registerItem("detonator", DetonatorItem::new);

    public static final DeferredItem<ConsumableItem> CONSUMABLE =
            ITEMS.registerItem("consumable", ConsumableItem::new);

    private ModItems() {
    }
}
