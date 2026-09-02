package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * 读取 {@code tacz-server.toml}（{@code GunMod} 以 {@code ModConfig.Type.SERVER}
 * 注册的 {@code ServerConfig.spec}）中的列表型配置到查找缓存：
 * 爆头 AABB（{@link HeadShotAABBConfigRead}）与交互键黑白名单
 * （{@link InteractKeyConfigRead}）。
 *
 * <p>26.x 的 {@code @EventBusSubscriber} 无 {@code bus} 参数；
 * {@link ModConfigEvent} 实现 {@code IModBusEvent}，订阅方法自动路由到 mod 总线。</p>
 *
 * <p>不接线本类的后果：两个缓存永远为空 —— 爆头 AABB 配置失效
 * （{@code EntityUtil} 命中判定拿不到 AABB）、交互键黑白名单失效
 * （{@code InteractKey} 与悬浮提示全部按默认放行）。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class LoadingConfigEvent {
    private static final String CONFIG_NAME = "tacz-server.toml";

    /**
     * 客户端和服务端启动时，会触发此事件
     */
    @SubscribeEvent
    public static void onLoadingConfig(ModConfigEvent.Loading event) {
        onConfigLoaded(event.getConfig());
    }

    /**
     * 玩家进入服务端，或者服务端自动重置配置时，会触发此方法
     */
    @SubscribeEvent
    public static void onReloadingConfig(ModConfigEvent.Reloading event) {
        onConfigLoaded(event.getConfig());
    }

    private static void onConfigLoaded(ModConfig config) {
        String fileName = config.getFileName();
        if (CONFIG_NAME.equals(fileName)) {
            HeadShotAABBConfigRead.init();
            InteractKeyConfigRead.init();
        }
    }
}
