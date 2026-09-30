package com.tacz.guns.compat.shouldersurfing;

import net.neoforged.fml.ModList;

/**
 * Optional Shoulder Surfing Reloaded compat: crosshair visibility under its camera.
 *
 * <p>【26.3：恢复启用（同步自姊妹仓 146aa42e）】SSR 上游已发布 26.3 构建
 * （Modrinth {@code 26.3-5.2.0+neoforge}，2026-09-30 核实），IMPL
 * （{@code ShoulderSurfingCompatInner} / {@code ShoulderSurfingPlugin}，
 * API 5.x）重新参与编译，{@code shouldersurfing_plugin.json} 已还原。
 * 未安装 SSR 时 {@code init()} 置 {@code INSTALLED = false}，
 * {@link #showCrosshair()} 回退 {@code false}（不干预准星，vanilla 行为）。</p>
 */
public final class ShoulderSurfingCompat {
    private static final String MOD_ID = "shouldersurfing";
    private static boolean INSTALLED = false;

    private ShoulderSurfingCompat() {
    }

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean showCrosshair() {
        return INSTALLED && ShoulderSurfingCompatInner.showCrosshair();
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}
