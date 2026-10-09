package com.tacz.guns;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.config.ServerConfig;
import com.tacz.guns.init.CapabilityRegistry;
import com.tacz.guns.init.CommonRegistry;
import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.init.ModCreativeTabs;
import com.tacz.guns.init.ModEntities;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.init.ModParticles;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(GunMod.MOD_ID)
public class GunMod {
    public static final String MOD_ID = "tacz";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String DEFAULT_GUN_PACK_NAME = "tacz_default_gun";

    public static net.neoforged.fml.ModContainer container;

    public GunMod(IEventBus modEventBus, ModContainer modContainer) {
        container = modContainer;
        modContainer.registerConfig(ModConfig.Type.STARTUP, PreLoadConfig.spec, "tacz-pre.toml");
        // 【26.3.0.51-beta / FML 12.0.8 兼容】ModConfig.Type 的 COMMON/SERVER 已被上游移除，
        // 新枚举为 LOCAL / CLIENT / SYNCED / STARTUP（FancyModLoader main，2026-10-04）。
        // COMMON→LOCAL（双端加载、全局 config 目录、不同步 —— 语义一致）、SERVER→SYNCED
        // （服务端加载并同步到客户端 —— 语义一致）。新枚举的默认文件名会变成
        // tacz-local.toml / tacz-synced.toml，这里显式传旧文件名 tacz-common.toml /
        // tacz-server.toml，老用户的现有配置文件与 LoadingConfigEvent 的文件名匹配
        // 均保持不变（CLIENT 的默认名 tacz-client.toml 新旧相同，无需显式）。
        modContainer.registerConfig(ModConfig.Type.LOCAL, CommonConfig.spec, "tacz-common.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, ServerConfig.spec, "tacz-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.spec);

        CapabilityRegistry.ATTACHMENT_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.TILE_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModRecipe.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipe.RECIPE_TYPES.register(modEventBus);
        ModRecipe.RECIPE_BOOK_CATEGORIES.register(modEventBus);
        ModRecipe.INGREDIENT_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        com.tacz.guns.init.ModContainer.CONTAINER_TYPE.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModAttributes.ATTRIBUTES.register(modEventBus);

        modEventBus.addListener(ModItems::onCommonSetup);
        modEventBus.addListener(NetworkHandler::register);
        modEventBus.addListener(NetworkHandler::registerConfigurationTasks);
        modEventBus.addListener(CommonRegistry::onSetupEvent);
        modEventBus.addListener(CommonRegistry::onLoadComplete);
        modEventBus.addListener(CommonRegistry::registerAttributes);
        modEventBus.addListener(CommonRegistry::onAddPackFinders);

        registerDefaultExtraGunPack();
        AttachmentPropertyManager.registerModifier();
        // LRTactical is an internal layer of the tacz mod container, not a second mod.
        me.xjqsh.lrtactical.EquipmentMod.register(modEventBus);
        LOGGER.info("TaCZ NeoForge 26.3 port R1 loading. modId={}", MOD_ID);
    }

    private static void registerDefaultExtraGunPack() {
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        ResourceManager.registerExportResource(GunMod.class, jarDefaultPackPath);
    }
}
