package me.xjqsh.lrtactical.client.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.client.renderer.entity.ThrowableEntityRenderer;
import me.xjqsh.lrtactical.entity.GrenadeEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.function.BiConsumer;

/**
 * 客户端注册接线（NeoForge 事件式）。
 *
 * <p>由 GunModClient / ClientSetupEvent 在对应 mod bus 事件里调用：
 * 实体渲染器 → {@code EntityRenderersEvent.RegisterRenderers}，
 * 粒子 provider → {@code RegisterParticleProvidersEvent}，
 * HUD → {@code RegisterGuiLayersEvent}，
 * display 资源加载器 → {@code AddClientReloadListenersEvent}（经 BiConsumer 传入，
 * 保持与 TACZ 侧 ClientAssetsManager/PAL 相同的注册通道）。
 */
public final class ModEntitiesRender {
    private ModEntitiesRender() {
    }

    /** 实体渲染器 ——【必须】每新增一种投掷物实体都要加一行，否则一进视野就 NPE 崩溃。 */
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 五种投掷物实体都继承 ThrowableItemEntity，故共用同一个渲染器。
        event.registerEntityRenderer(GrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(
                me.xjqsh.lrtactical.entity.StickyGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(
                me.xjqsh.lrtactical.entity.SmokeGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(
                me.xjqsh.lrtactical.entity.EffectCloudGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(
                me.xjqsh.lrtactical.entity.StunGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        // 效果云本体用 NoopRenderer：云自身【不绘制任何模型】，视觉完全由粒子构成。
        // 注意它同样【必须注册】—— 没有渲染器一进视野就 NPE 崩溃，
        // 「不需要画」和「不注册」是两回事。
        event.registerEntityRenderer(
                me.xjqsh.lrtactical.entity.sp.SpEffectCloudEntity.TYPE,
                net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    /** 粒子 provider（烟雾云粒子）。 */
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(me.xjqsh.lrtactical.init.ModParticleTypes.SMOKE_CLOUD,
                me.xjqsh.lrtactical.client.particle.SmokeCloudParticle.Provider::new);
    }

    /**
     * 客户端物品模型类型 lrtactical:dynamic_item —— 经 RegisterItemModelsEvent 注册
     * （ItemModels.ID_MAPPER 在 26.1.2 是 private，主 mod 的 TaczDynamicItemModel 同款习语）。
     */
    public static void registerItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
        event.register(
                me.xjqsh.lrtactical.client.renderer.item.LrDynamicItemModel.TYPE_ID,
                me.xjqsh.lrtactical.client.renderer.item.LrDynamicItemModel.Unbaked.MAP_CODEC);
    }

    /**
     * 条件属性 lrtactical:has_custom_display（用于「有无内容包」的模型分流）——
     * 经 RegisterConditionalItemModelPropertyEvent 注册
     * （ConditionalItemModelProperties.ID_MAPPER 在 26.1.2 是 private）。
     */
    public static void registerConditionalProperties(
            net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent event) {
        event.register(
                me.xjqsh.lrtactical.client.renderer.item.HasCustomDisplayProperty.ID,
                me.xjqsh.lrtactical.client.renderer.item.HasCustomDisplayProperty.MAP_CODEC);
    }

    /** display 资源加载器（assets/lrtactical/display/**），须与 TACZ 侧同一 reload 事件注册。 */
    public static void registerReloadListeners(BiConsumer<Identifier, PreparableReloadListener> register) {
        me.xjqsh.lrtactical.client.resource.LrClientAssetsManager.INSTANCE.reloadAndRegister(register);
    }

    /** 把四个基础物品的动态渲染器登记进 BuiltinItemRendererRegistry。 */
    public static void registerItemRenderers() {
        register(me.xjqsh.lrtactical.init.ModItems.MELEE);
        register(me.xjqsh.lrtactical.init.ModItems.THROWABLE);
    }

    private static void register(net.minecraft.world.item.Item item) {
        if (item instanceof me.xjqsh.lrtactical.api.extension.IItem ext) {
            var renderer = ext.getCustomRenderer();
            if (renderer != null) {
                com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry.INSTANCE.register(item, renderer);
            }
        }
    }

    /** HUD 覆盖层：使用进度条 + 致盲遮罩（闪光弹的实际效果所在）。 */
    public static void registerHudOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(EquipmentMod.MOD_ID, "using_progress"),
                (graphics, deltaTracker) ->
                        me.xjqsh.lrtactical.client.overlay.UsingProgressOverlay.render(
                                graphics, deltaTracker.getRealtimeDeltaTicks()));
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(EquipmentMod.MOD_ID, "blindness"),
                (graphics, deltaTracker) ->
                        me.xjqsh.lrtactical.client.overlay.BlindnessOverlay.render(
                                graphics, deltaTracker.getRealtimeDeltaTicks()));
    }
}
