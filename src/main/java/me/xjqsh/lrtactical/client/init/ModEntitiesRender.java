package me.xjqsh.lrtactical.client.init;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.api.item.ILrItemExtension;
import com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry;
import me.xjqsh.lrtactical.client.overlay.BlindnessOverlay;
import me.xjqsh.lrtactical.client.overlay.UsingProgressOverlay;
import me.xjqsh.lrtactical.client.particle.SmokeCloudParticle;
import me.xjqsh.lrtactical.client.renderer.entity.ThrowableEntityRenderer;
import me.xjqsh.lrtactical.client.renderer.item.HasCustomDisplayProperty;
import me.xjqsh.lrtactical.client.renderer.item.LrDynamicItemModel;
import me.xjqsh.lrtactical.client.resource.LrClientAssetsManager;
import me.xjqsh.lrtactical.entity.EffectCloudGrenadeEntity;
import me.xjqsh.lrtactical.entity.GrenadeEntity;
import me.xjqsh.lrtactical.entity.SmokeGrenadeEntity;
import me.xjqsh.lrtactical.entity.StickyGrenadeEntity;
import me.xjqsh.lrtactical.entity.StunGrenadeEntity;
import me.xjqsh.lrtactical.entity.sp.SpEffectCloudEntity;
import me.xjqsh.lrtactical.init.ModItems;
import me.xjqsh.lrtactical.init.ModParticleTypes;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * LR 客户端注册面（WP-LR2：refab 的显式调用链 → NeoForge 事件形态）。
 *
 * <p>各事件方法由 {@code LrClientEvents} 转调；{@link #registerItemRenderers()} 则由
 * tacz 的 {@code GunModClient} 在 {@code FMLClientSetupEvent.enqueueWork} 中调用——
 * 26.1.2 LR2 的注册时序教训：
 * 构造期调用时 DeferredRegister 字段尚未填充，注册静默跳过。
 */
public final class ModEntitiesRender {
    private ModEntitiesRender() {
    }

    /** 实体渲染器：EntityRendererRegistry（Fabric）→ EntityRenderersEvent.RegisterRenderers。 */
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(StickyGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(SmokeGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(EffectCloudGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(StunGrenadeEntity.TYPE, ThrowableEntityRenderer::new);
        event.registerEntityRenderer(SpEffectCloudEntity.TYPE, NoopRenderer::new);
    }

    /** 粒子提供器：SpriteSet 构造式 Provider 与 registerSpriteSet 形参吻合。 */
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.SMOKE_CLOUD.get(), SmokeCloudParticle.Provider::new);
    }

    /** 物品模型类型：ID_MAPPER 私有（B-5）→ RegisterItemModelsEvent。 */
    public static void onRegisterItemModels(RegisterItemModelsEvent event) {
        event.register(LrDynamicItemModel.TYPE_ID, LrDynamicItemModel.Unbaked.MAP_CODEC);
    }

    /** 条件属性：同为 B-5 事件化。 */
    public static void onRegisterConditionalProperties(RegisterConditionalItemModelPropertyEvent event) {
        event.register(HasCustomDisplayProperty.ID, HasCustomDisplayProperty.MAP_CODEC);
    }

    /** display 资源加载器（assets/lrtactical/display/**）。 */
    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        LrClientAssetsManager.INSTANCE.reloadAndRegister(event::addListener);
    }

    /**
     * HUD 覆盖层。registerAboveAll 语义 = Fabric addLast（画在既有元素之上）；
     * 致盲遮罩最后注册，盖住包括物品栏/血条在内的全部 HUD（玩法语义，见 refab 注释）。
     */
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(EquipmentMod.id("using_progress"), (graphics, delta) ->
                UsingProgressOverlay.render(graphics, delta.getRealtimeDeltaTicks()));
        event.registerAboveAll(EquipmentMod.id("blindness"), (graphics, delta) ->
                BlindnessOverlay.render(graphics, delta.getRealtimeDeltaTicks()));
    }

    /** 物品渲染器登记——必须在 FMLClientSetupEvent.enqueueWork 之后（r29）。 */
    public static void registerItemRenderers() {
        register(ModItems.MELEE.get());
        register(ModItems.THROWABLE.get());
        register(ModItems.CONSUMABLE.get());
    }

    private static void register(Item item) {
        if (item instanceof ILrItemExtension ext) {
            var renderer = ext.getCustomRenderer();
            if (renderer != null) {
                BuiltinItemRendererRegistry.INSTANCE.register(item, renderer);
            }
        }
    }
}
