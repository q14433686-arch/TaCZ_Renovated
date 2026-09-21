package com.tacz.guns.compat.shouldersurfing;

import net.neoforged.fml.ModList;

/**
 * Optional Shoulder Surfing Reloaded compat: crosshair visibility under its camera.
 *
 * <p>【26.3：禁用，非修复】SSR 上游暂无 26.3 构建（最新 26.2-5.0.7，2026-09-21
 * 核对），{@code ShoulderSurfingCompatInner} / {@code ShoulderSurfingPlugin}
 * （编译期依赖其 5.x API 的 IMPL 层）已随本轮移植从 sourceSets 排除，
 * {@code shouldersurfing_plugin.json} 摘至 {@code docs/patch/} 存档。门面保留：
 * {@link #showCrosshair()} 恒 {@code false}（= 不干预准星显示，回退 vanilla 行为），
 * {@link #isInstalled()} 恒 {@code false}。上游发布 26.3 构件后恢复 IMPL
 * （去掉 build.gradle 的两条 exclude、还原 plugin json）即可，调用点无需改动。</p>
 */
public final class ShoulderSurfingCompat {
    private static final String MOD_ID = "shouldersurfing";

    private ShoulderSurfingCompat() {
    }

    public static void init() {
        // 门面恒关：IMPL 已排除，无 API 可探（hasV5Api 连同类都不存在了）。
        // ModList 检测保留在这里只为让未来的「上游恢复发布」在日志里可见。
        if (ModList.get().isLoaded(MOD_ID)) {
            com.tacz.guns.GunMod.LOGGER.warn("[TACZ] Shoulder Surfing Reloaded detected, but its integration "
                    + "is disabled in this build (no SSR 26.3 artifact when this port was made).");
        }
    }

    /** 恒 false：不干预准星（回退 vanilla / TACZ 自身逻辑）。 */
    public static boolean showCrosshair() {
        return false;
    }

    /** 恒 false：兼容未启用。 */
    public static boolean isInstalled() {
        return false;
    }
}
