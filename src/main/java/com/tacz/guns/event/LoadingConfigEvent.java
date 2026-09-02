package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * 服务端配置（tacz-server.toml，含 {@code SyncConfig}）加载/热重载时，
 * 触发文本型配置的解析器：
 * <ul>
 * <li>{@link HeadShotAABBConfigRead}：第三方生物（女仆、自定义怪物等）的自定义爆头 AABB；</li>
 * <li>{@link InteractKeyConfigRead}：交互键的方块/实体黑白名单。</li>
 * </ul>
 *
 * <p>接线说明（NeoForge 26.1.x / FML 11，证据见 docs/records）：
 * {@code ModConfigEvent} 实现 {@code IModBusEvent}，26.1 的
 * {@code @EventBusSubscriber} 没有 Bus 参数，会自动把 {@code IModBusEvent}
 * 子类注册到本 mod 的 mod 事件总线（参照本仓 {@code ClientSetupEvent} 的同款接线）。
 * SERVER 类型配置在玩家登入远程服务器时会由 NeoForge 同步到客户端并在客户端触发
 * {@code Reloading}，因此客户端的白名单/爆头箱也随之刷新。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class LoadingConfigEvent {
    private static final String CONFIG_NAME = "tacz-server.toml";

    /**
     * 客户端和服务端启动时（配置首次加载），会触发此事件
     */
    @SubscribeEvent
    public static void onLoadingConfig(ModConfigEvent.Loading event) {
        initConfig(event.getConfig());
    }

    /**
     * 玩家进入服务端（服务端配置同步到客户端），或者配置文件热重载时，会触发此方法
     */
    @SubscribeEvent
    public static void onReloadingConfig(ModConfigEvent.Reloading event) {
        initConfig(event.getConfig());
    }

    private static void initConfig(ModConfig config) {
        String fileName = config.getFileName();
        if (CONFIG_NAME.equals(fileName)) {
            HeadShotAABBConfigRead.init();
            InteractKeyConfigRead.init();
        }
    }
}
