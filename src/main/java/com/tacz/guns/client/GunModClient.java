package com.tacz.guns.client;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Physical-client entry. Scope pipelines register through RegisterRenderPipelinesEvent before ShaderManager's first reload.
 */
@Mod(value = GunMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GunMod.MOD_ID, value = Dist.CLIENT)
public class GunModClient {
    public GunModClient(ModContainer container) {
        // TACZ classic Cloth Config screen (MUKSC idiom): cloth present -> cloth UI,
        // absent -> download-hint screen. Registration mirrors MUKSC's CompatRegistry.
        if (net.neoforged.fml.ModList.get().isLoaded(com.tacz.guns.init.CompatRegistry.CLOTH_CONFIG)) {
            com.tacz.guns.compat.cloth.MenuIntegration.registerModsPage(container);
        } else {
            com.tacz.guns.client.gui.compat.ClothConfigScreen.registerNoClothConfigPage(container);
        }
        GunMod.LOGGER.info("TaCZ NeoForge 26.2 port R1 client loading. modId={}", GunMod.MOD_ID);
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
