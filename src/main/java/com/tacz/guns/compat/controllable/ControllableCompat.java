package com.tacz.guns.compat.controllable;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Optional Controllable (MrCrayfish) compat: controller bindings + gun-fire rumble.
 *
 * <p>【26.3：禁用，非修复】Controllable 上游暂无 26.3 构建（最新 0.26.1+26.2，
 * 2026-09-21 核对），{@code ControllableInner}（编译期依赖其 API 的 IMPL 层）已随
 * 本轮移植从 sourceSets 排除。门面保留：所有调用点继续编译，行为为 no-op；
 * Controllable 的控制器输入在 26.3 上本就装不上，无功能损失。上游发布 26.3 构件后
 * 恢复 IMPL（去掉 build.gradle 的 exclude，重新挂 {@code controllable_neoforge_file}）
 * 即可，调用点无需改动。</p>
 */
public class ControllableCompat {
    private static final String MOD_ID = "controllable";
    private static boolean loggedDisabled;

    public static void init() {
        // 门面恒关：IMPL 已排除。检测到 mod 在场（理论上不可能：无 26.3 构建）时
        // 打一行说明，方便上游恢复发布后第一时间在日志里看到。
        if (ModList.get().isLoaded(MOD_ID) && !loggedDisabled) {
            loggedDisabled = true;
            GunMod.LOGGER.warn("[TACZ] Controllable detected, but its integration is disabled in this build "
                    + "(no Controllable 26.3 artifact when this port was made).");
        }
    }

    public static void onGunShoot(ItemStack gunItem, FireMode fireMode) {
        // no-op：见类注释。
    }
}
