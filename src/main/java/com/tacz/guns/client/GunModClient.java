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
        // TACZ classic Cloth Config screen (MUKSC idiom): cloth present -> cloth UI,
        // absent -> download-hint screen. Registration mirrors MUKSC's CompatRegistry.
        if (net.neoforged.fml.ModList.get().isLoaded(com.tacz.guns.init.CompatRegistry.CLOTH_CONFIG)) {
            com.tacz.guns.compat.cloth.MenuIntegration.registerModsPage(container);
        } else {
            com.tacz.guns.client.gui.compat.ClothConfigScreen.registerNoClothConfigPage(container);
        }
        GunMod.LOGGER.info("TaCZ NeoForge 1.21.11 port work package ⑤ client loading. modId={}", GunMod.MOD_ID);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.tacz.guns.client.init.ClientSetupEvent.onClientSetup();
            // 内置 TacZ Mesh Loader：注册 model_type=mesh 枪模构造器与状态追踪基建。
            // 必须在任何枪 display 资源加载（ClientAssetsManager 的 reload listener 触发）之前
            // 把 model_type=mesh 构造器注册进 GunModelTypeManager，否则 checkTextureAndModel
            // 会落到默认 BedrockGunModel 构造器，mesh 枪退回纯立方体。
            cn.sh1rocu.tacz.compat.meshloader.TaczMeshyIntegration.onClientSetup();
            com.tacz.guns.client.init.ClientSetupEvent.registerItemRenderers();
            // WP-LR2：LR 物品渲染器登记——必须在 enqueueWork 内（r29：构造期字段未填充会静默跳过）。
            me.xjqsh.lrtactical.client.init.ModEntitiesRender.registerItemRenderers();
            com.tacz.guns.compat.shader.ShaderCompat.assignCommonEntityPipelinesToHandIfNeeded();
            GunMod.LOGGER.info("TaCZ client setup (work package ⑥ ShaderCompat). minecraft={}",
                    Minecraft.getInstance().getUser().getName());
        });
    }
}
