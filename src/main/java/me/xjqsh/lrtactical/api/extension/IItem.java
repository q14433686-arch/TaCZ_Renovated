package me.xjqsh.lrtactical.api.extension;

/**
 * LR 侧的物品扩展点：物品声明自己的动态渲染器。
 *
 * <p>原 refab 实现里这是 tacz 主 mod 的 {@code cn.sh1rocu.tacz.api.extension.IItem}；
 * 本 NeoForge 移植把它收进 LR 自身包（tacz 侧没有该接口，注册由
 * {@code ModEntitiesRender#registerItemRenderers} 显式完成，语义不变）。</p>
 */
public interface IItem {
    com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry.DynamicItemRenderer getCustomRenderer();
}
