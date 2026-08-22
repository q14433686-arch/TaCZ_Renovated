package me.xjqsh.lrtactical.api.item;

import com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * WP-LR2：替代 refab 垫片 {@code cn.sh1rocu.tacz.api.extension.IItem} 的 LR 本地接口。
 *
 * <p>{@link #getCustomRenderer()} 返回类型是客户端类——按本仓纪律这是"惰性解析安全型"：
 * 仅 {@code ModEntitiesRender.registerItemRenderers}（客户端启动链）调用，
 * dedicated 上该方法从不执行，参数/返回类型不触发类加载（与 tacz
 * GunSmithTableItem#getCustomRenderer 同模式，R1 专服 L2 实证）。
 *
 * <p>{@code tacz$onEntitySwing}：refab 靠其 LivingEntityMixin 驱动；本仓主 mod
 * 无该 mixin（WP07 C 表末行明示），default 不会被调用——近战真正的攻击拦截走
 * {@code AttackEntityEvent}（EquipmentMod 接线），仅损失"挥臂动画抑制"这一
 * 化妆级效果。LR2-6 实测若确认影响观感，再评估补 mixin。
 */
public interface ILrItemExtension {
    default BuiltinItemRendererRegistry.DynamicItemRenderer getCustomRenderer() {
        return null;
    }

    default boolean tacz$onEntitySwing(ItemStack stack, LivingEntity entity) {
        return false;
    }
}
