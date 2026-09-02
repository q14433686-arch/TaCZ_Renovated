package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.util.CycleTaskHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 服务端每 tick 结束时推进 {@link CycleTaskHelper} 的延迟/循环任务队列。
 *
 * <p><b>没有这条接线的可见症状</b>：{@code CycleTaskHelper.addCycleTask} 只会在入队时
 * 立刻执行第一次（见其实现），后续循环全部依赖 {@code tick()} —— 即
 * <b>BURST（连发）模式服务端只打出第一发</b>（{@code ModernKineticGunScriptAPI}
 * 的射击循环）、Lua 脚本的 {@code safeAsyncTask} 延迟任务永不执行。</p>
 *
 * <p>事件为 {@code net.neoforged.neoforge.event.tick.ServerTickEvent.Post}
 * （NeoForge 21.11 @ 1.21.11 分支该文件确认：Post 每服务端 tick 末触发一次、
 * 仅逻辑服务端；对应上游 Forge {@code TickEvent.Phase.END} / Fabric
 * {@code END_SERVER_TICK}；本仓先例：{@code SyncedEntityDataEvent#onServerTick}
 * 同款）。任务为墙钟驱动，Post 相位每 tick 一次即可。
 * 与本类同名的 NeoForge 事件类以全限定名引用，避免混淆。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class ServerTickEvent {
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        // 更新 CycleTaskHelper 中的任务
        CycleTaskHelper.tick();
    }
}
