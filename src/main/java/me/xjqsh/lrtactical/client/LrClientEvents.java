package me.xjqsh.lrtactical.client;

import me.xjqsh.lrtactical.client.audio.DeafenState;
import me.xjqsh.lrtactical.client.event.LrTickAnimationEvent;
import me.xjqsh.lrtactical.client.init.ModEntitiesRender;
import me.xjqsh.lrtactical.client.input.MeleeAttackKeys;
import me.xjqsh.lrtactical.client.input.StuckUseRecovery;
import me.xjqsh.lrtactical.client.input.UsePressGate;
import me.xjqsh.lrtactical.init.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * LR 客户端事件订阅（WP-LR2 接线核心）。
 *
 * <p>modid 必须是 {@code tacz}（LR 内置在 tacz 容器内）；注解扫描不分包名，
 * 与 tacz 自己的 ModEntitiesRender / ClientGameEvents 同形态——
 * IModBusEvent（注册类）与 game 总线事件（tick/输入/渲染帧）均经此投递
 * （records/WP05：MDK 26.1.2 语义）。
 *
 * <p>tick 与渲染帧的双通道驱动照抄 refab / tacz ClientGameEvents：
 * START/END 各推一次状态机（幂等 trigger 降输入延迟），第三人称动画走渲染帧。
 */
@EventBusSubscriber(modid = com.tacz.guns.GunMod.MOD_ID, value = Dist.CLIENT)
public final class LrClientEvents {
    private LrClientEvents() {
    }

    // ---------- 注册类（IModBusEvent） ----------

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModEntitiesRender.onRegisterRenderers(event);
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        ModEntitiesRender.onRegisterParticles(event);
    }

    @SubscribeEvent
    public static void onRegisterItemModels(RegisterItemModelsEvent event) {
        ModEntitiesRender.onRegisterItemModels(event);
    }

    @SubscribeEvent
    public static void onRegisterConditionalProperties(RegisterConditionalItemModelPropertyEvent event) {
        ModEntitiesRender.onRegisterConditionalProperties(event);
    }

    @SubscribeEvent
    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        ModEntitiesRender.onAddClientReloadListeners(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        ModEntitiesRender.onRegisterGuiLayers(event);
    }

    /**
     * LR 三类 tooltip 数据组件 → 客户端组件工厂。
     * 首轮 runClient 实测崩溃修复：漏注册时鼠标悬停任意 LR 物品即
     * IllegalArgumentException: Unknown TooltipComponent
     * （ClientTooltipComponent.create，refab 侧走 Fabric TooltipComponentCallback）。
     */
    @SubscribeEvent
    public static void onRegisterTooltips(net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(me.xjqsh.lrtactical.inventory.tooltip.ThrowableTooltip.class,
                me.xjqsh.lrtactical.client.tooltip.ClientThrowableTooltip::new);
        event.register(me.xjqsh.lrtactical.inventory.tooltip.MeleeTooltip.class,
                me.xjqsh.lrtactical.client.tooltip.ClientMeleeTooltip::new);
        event.register(me.xjqsh.lrtactical.inventory.tooltip.ConsumableTooltip.class,
                me.xjqsh.lrtactical.client.tooltip.ClientConsumableTooltip::new);
    }

    // ---------- game 总线（tick / 输入 / 渲染帧） ----------

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        LrTickAnimationEvent.tickAnimation(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LrTickAnimationEvent.tickAnimation(mc);
        DeafenState.tick(mc);
        // 一次按压只消耗一次使用：必须在本 tick 末尾采样，
        // 才能在「使用结束」的那一次 tick 内看到下降沿（见 UsePressGate 类注释）。
        UsePressGate.onClientTick(mc);
        // 分叉兜底：客户端若陷进服务端不存在的使用状态，自动停止（见类注释）。
        StuckUseRecovery.onClientTick(mc);
        // 客户端玩家换代检测（小退/重进清客户端冷却表，见 ModCapabilities 注释）
        ModCapabilities.onClientPlayerTick(mc.player);
    }

    /** D-12 备注：tickAnimation 双重载，此处经显式类型形参消歧。 */
    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        LrTickAnimationEvent.tickAnimation(event);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        MeleeAttackKeys.onMousePress(event);
    }
}
