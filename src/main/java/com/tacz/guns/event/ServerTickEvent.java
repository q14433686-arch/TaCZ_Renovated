package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.util.CycleTaskHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 服务端 tick 驱动 {@link CycleTaskHelper}。
 *
 * <p>【grep 陷阱】本类与 NeoForge 的
 * {@code net.neoforged.neoforge.event.tick.ServerTickEvent} 同名，
 * 全仓 grep 时注意区分（NeoForge 的在 {@code event/tick/} 包，
 * 不是 {@code event/server/}）。因类名冲突，事件类型用全限定名指认。</p>
 *
 * <p>官方 1.20.1 挂的是旧 {@code TickEvent.ServerTickEvent}（pre/post 两相都触发，
 * 即每 tick 跑两次）；26.x 拆分为 {@code Pre}/{@code Post} 两个独立事件，
 * 这里取 {@code Post}：每 tick 一次、在服务端完成本 tick 工作之后驱动循环任务，
 * 与 {@code CycleTaskTicker} 的毫秒时间戳节流模型匹配。</p>
 *
 * <p>不接线本类的后果：BURST 连发只出第一发、Lua {@code safeAsyncTask}
 * 永不执行（{@code ModernKineticGunScriptAPI} 的循环任务全部注册在
 * {@code CycleTaskHelper} 中，无人驱动）。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class ServerTickEvent {
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        // 更新 CycleTaskHelper 中的任务
        CycleTaskHelper.tick();
    }
}
