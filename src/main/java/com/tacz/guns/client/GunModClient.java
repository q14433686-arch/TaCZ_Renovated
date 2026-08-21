package com.tacz.guns.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.render.scope.ScopeRenderTypes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Physical-client entry. Scope pipelines must register before ShaderManager's first reload.
 */
@Mod(value = GunMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GunMod.MOD_ID, value = Dist.CLIENT)
public class GunModClient {
    public GunModClient(net.neoforged.bus.api.IEventBus modEventBus, ModContainer container) {
        ScopeRenderTypes.init();
        // 附属模块 LRTactical 的客户端物品模型类型（lrtactical:dynamic_item）与条件属性
        // （lrtactical:has_custom_display）—— 必须在任何客户端物品 JSON 解码之前注册。
        me.xjqsh.lrtactical.client.init.ModEntitiesRender.registerItemModels();
        me.xjqsh.lrtactical.client.init.ModEntitiesRender.registerItemRenderers();
        // LR 的 mod bus 注册：实体渲染器（缺则进视野 NPE）、粒子 provider、HUD 覆盖层。
        modEventBus.addListener(me.xjqsh.lrtactical.client.init.ModEntitiesRender::registerEntityRenderers);
        modEventBus.addListener(me.xjqsh.lrtactical.client.init.ModEntitiesRender::registerParticles);
        modEventBus.addListener(me.xjqsh.lrtactical.client.init.ModEntitiesRender::registerHudOverlays);
        // LR 的 game bus 注册：近战左右键、冷却/动画 tick、耳鸣声驱动。
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(me.xjqsh.lrtactical.client.input.MeleeAttackKeys::onMousePress);
        bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) -> {
            var mc = Minecraft.getInstance();
            me.xjqsh.lrtactical.init.ModCapabilities.onClientPlayerTick(mc.player);
            me.xjqsh.lrtactical.client.event.LrTickAnimationEvent.tickAnimation(mc);
        });
        bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> {
            me.xjqsh.lrtactical.client.event.LrTickAnimationEvent.tickAnimation(Minecraft.getInstance());
            me.xjqsh.lrtactical.client.audio.DeafenState.tick(Minecraft.getInstance());
        });
        bus.addListener(me.xjqsh.lrtactical.client.event.LrTickAnimationEvent::tickAnimation);
        // TACZ 侧的 Cloth Config / Controllable 兼容注册。
        // cloth present -> cloth UI, absent -> download-hint screen. Registration mirrors MUKSC's CompatRegistry.
        if (net.neoforged.fml.ModList.get().isLoaded(com.tacz.guns.init.CompatRegistry.CLOTH_CONFIG)) {
            com.tacz.guns.compat.cloth.MenuIntegration.registerModsPage(container);
        } else {
            com.tacz.guns.client.gui.compat.ClothConfigScreen.registerNoClothConfigPage(container);
        }
        GunMod.LOGGER.info("TaCZ NeoForge 26.1.2 port work package ⑤ client loading. modId={}", GunMod.MOD_ID);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.tacz.guns.client.init.ClientSetupEvent.onClientSetup();
            com.tacz.guns.client.init.ClientSetupEvent.registerItemRenderers();
            com.tacz.guns.compat.shader.ShaderCompat.assignCommonEntityPipelinesToHandIfNeeded();
            GunMod.LOGGER.info("TaCZ client setup (work package ⑥ ShaderCompat). minecraft={}",
                    Minecraft.getInstance().getUser().getName());
        });
    }
}
