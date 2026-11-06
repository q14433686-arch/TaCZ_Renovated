package com.tacz.guns.entity.shooter;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.LuaError;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * 枪械脚本的兜底（1.2.0 引入）：脚本炸了不能把服务端一起扬了。
 *
 * <p>枪包脚本通过 {@code ModernKineticGunScriptAPI} 直接跑在服务端主线程上，
 * 一次 {@link LuaError} 以前会一路抛到 tick 循环外面（NeoForge 会当成 tick 异常，
 * 单人直接崩、服务端可能连坐）。这里把「一次枪械操作」整体包起来：
 *
 * <ul>
 *   <li>脚本抛 {@link LuaError} → 记一条 error、清掉这次操作的中间状态、把
 *       {@link ShooterDataHolder#gunScriptFailed} 置位，<b>本次操作按失败处理</b>；</li>
 *   <li>失败标记会一直保持到下一次 {@code initialData()} / 切枪，期间所有脚本动作
 *       直接短路（不重复刷日志、不反复走炸掉的路径）；</li>
 *   <li>短路的判定还要求「持枪没变」：切枪后旧枪的回调即使迟到也不会作用到新枪上。</li>
 * </ul>
 *
 * <p><b>适用范围（本仓当前）</b>：接入的是枪包脚本的两个执行入口 —— 射击循环
 * {@code ModernKineticGunScriptAPI#runShootCycle} 与 {@code safeAsyncTask} 的
 * {@code lua function} 回调。上游 1.2.0 还把它铺到 reload / bolt / melee / heat
 * 等每个操作入口上，那一层需要重构 {@code LivingEntityMixin} 的 tick 段（上游同时在
 * 该段引入了动画时钟 {@code GunTime}），属于独立的后续改动，未在本块完成。
 */
public final class GunScriptGuard {
    private GunScriptGuard() {
    }

    public static void run(ShooterDataHolder data, LivingEntity shooter, @Nullable Identifier gunId,
                           String operation, Runnable action) {
        run(data, shooter, gunId, operation, () -> {
            action.run();
            return null;
        }, null);
    }

    /**
     * @param fallback 脚本不可用、或本次操作抛错时返回的值
     */
    public static <T> T run(ShooterDataHolder data, LivingEntity shooter, @Nullable Identifier gunId,
                            String operation, Supplier<T> action, T fallback) {
        if (data.gunScriptFailed) {
            return fallback;
        }
        if (gunId == null || !gunId.equals(data.currentGunId)) {
            // 持枪已经变了：这次回调属于上一把枪，丢弃（不改任何状态）。
            return fallback;
        }
        ItemStack current = data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        if (!hasGunId(current, gunId) || !hasGunId(shooter.getMainHandItem(), gunId)) {
            // 缓存里的枪与手上的枪对不上：数据过期，中止这次操作并清掉中间状态。
            data.cancelGunScriptState();
            return fallback;
        }
        try {
            T result = action.get();
            return data.gunScriptFailed ? fallback : result;
        } catch (LuaError exception) {
            data.cancelGunScriptState();
            data.gunScriptFailed = true;
            GunMod.LOGGER.error("Gun script failed for {} during {} (entity {}); paused until next draw",
                    gunId, operation, shooter.getUUID(), exception);
            return fallback;
        }
    }

    private static boolean hasGunId(@Nullable ItemStack stack, Identifier gunId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        IGun gun = IGun.getIGunOrNull(stack);
        return gun != null && gunId.equals(gun.getGunId(stack));
    }
}
