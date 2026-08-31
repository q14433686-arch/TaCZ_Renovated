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
    public GunModClient(ModContainer container) {
        ScopeRenderTypes.init();
        // Iris 反射桥的版本感知初始化（isRenderShadow 的 1.7.0 分界；必须在任何
        // PolyRenderPolicy / mesh GPU 查询阴影遍之前完成，与 ShaderCompat 注册同相位）。
        com.tacz.guns.compat.iris.IrisCompat.initCompat();
        // 内置 TacZ Mesh Loader：mesh 枪模构造器 + 状态追踪基建（GUI 提取窗口 / 光影翻转检测）。
        // 必须在客户端资源加载前注册（各 GunModelType 构造器只在 setup 期生效）。
        com.tacz.guns.compat.meshloader.TaczMeshyIntegration.onClientSetup();
        // TACZ classic Cloth Config screen (MUKSC idiom): cloth present -> cloth UI,
        // absent -> download-hint screen. Registration mirrors MUKSC's CompatRegistry.
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
            // WP-LR2：LR 物品渲染器登记——必须在 enqueueWork 内（r29：构造期字段未填充会静默跳过）。
            me.xjqsh.lrtactical.client.init.ModEntitiesRender.registerItemRenderers();
            com.tacz.guns.compat.shader.ShaderCompat.assignCommonEntityPipelinesToHandIfNeeded();
            GunMod.LOGGER.info("TaCZ client setup (work package ⑥ ShaderCompat). minecraft={}",
                    Minecraft.getInstance().getUser().getName());
        });
    }
}
