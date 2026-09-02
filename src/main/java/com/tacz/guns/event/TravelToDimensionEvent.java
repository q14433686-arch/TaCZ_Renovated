package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 修正跨纬度时，枪械数据不刷新的问题，这是服务端的刷新
 *
 * <h2>【重要】玩家与生物在 NeoForge 26.2 下挂两个不同事件</h2>
 *
 * <p>refab 谱系的结论（保留，作为语义依据）：生物跨维度时实体是<b>复制</b>到目标世界的，
 * 而玩家是<b>物理移动</b>（同一 {@code ServerPlayer} 实例），Fabric 官方的
 * {@code AFTER_ENTITY_CHANGE_LEVEL} 明写不适用于 {@code ServerPlayer}。上游 1.20.1
 * 只挂 {@code EntityTravelToDimensionEvent}，对玩家一次都没执行过（上游继承缺陷）。</p>
 *
 * <p>本移植线（NeoForge 26.2）的接线对应关系：</p>
 * <ul>
 *   <li>生物：{@link EntityTravelToDimensionEvent}（游戏总线）。注意它是<b>传送前</b>事件，
 *       由 {@code Entity#teleport(TeleportTransition)} 首行经
 *       {@code CommonHooks#onTravelToDimension(Entity, ResourceKey)} 发布，事件里拿到的
 *       就是原实体实例；需过滤「目标维度 == 当前维度」的同维度传送（该路径存在，
 *       NeoForge 侧 {@code Entity#teleport} patch 自身就计算 {@code otherDimension}）。
 *       玩家走下面的 {@link PlayerEvent.PlayerChangedDimensionEvent}，故此处排除玩家，
 *       避免同一次跨维度双触发。</li>
 *   <li>玩家：{@link PlayerEvent.PlayerChangedDimensionEvent}（游戏总线，传送后触发，
 *       {@code EventHooks#firePlayerChangedDimensionEvent} 发布），
 *       与 refab 的 {@code AFTER_PLAYER_CHANGE_LEVEL}（传送后）时序一致。</li>
 * </ul>
 *
 * <h2>造成的可见症状：跨维度后换弹动作连贯、但子弹不变</h2>
 *
 * <p>客户端那侧的刷新走的是另一条路
 * （{@code RefreshClonePlayerDataEvent#onClientTick} 轮询
 * {@code Minecraft#player} 实例变化，跨维度会换 {@code LocalPlayer}，
 * 故<b>客户端照常执行</b>），于是换弹动画、音效一切正常；
 * 而服务端 {@code initialData()} 从未执行，
 * {@code reloadStateType}/{@code reloadTimestamp}/{@code isBolting}/
 * {@code shootTimestamp} 全部保留旧维度的值。
 * 服务端 {@code LivingEntityReload#reload} 首道门禁
 * {@code data.reloadStateType.isReloading()} 因残留而成立，直接 return，
 * 于是「客户端演完整套换弹、服务端压根不认」。
 * <b>客户端与服务端的不对称</b>正是该症状的成因。</p>
 *
 * <h2>为什么这么改是安全的</h2>
 *
 * <p>{@code initialData()} 只重置状态机字段，并把
 * {@code currentGunItem} 重新绑定为 {@code () -> getMainHandItem()}、
 * 刷新配件缓存；它<b>不触碰 {@code drawTimestamp}</b>，
 * 因此不会产生切枪冷却 —— 不会重蹈
 * {@code ServerPlayerMixin} 注释里记录的那次回归
 * （那次是把 {@code currentGunItem} 清成 null，导致
 * {@code shoot} 直接返回 {@code NOT_DRAW}）。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class TravelToDimensionEvent {
    /**
     * 非玩家生物（如持枪僵尸）跨维度：传送前事件，实体尚未复制，
     * 先重置状态机，复制体继承的即为重置后的状态。
     */
    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        // 玩家由 onPlayerChangedDimension 处理（传送后时序），避免双触发
        if (entity instanceof Player) {
            return;
        }
        // 同维度传送（传送门指向本维度等路径）：没有维度切换，不存在跨维度残留
        if (event.getDimension() == entity.level().dimension()) {
            return;
        }
        if (entity instanceof LivingEntity livingEntity && livingEntity.getMainHandItem().getItem() instanceof IGun) {
            IGunOperator.fromLivingEntity(livingEntity).initialData();
        }
    }

    /**
     * 玩家跨维度。玩家是<b>被物理移动</b>过去的（同一个 {@code ServerPlayer} 实例），
     * NeoForge 在传送完成后发布 {@link PlayerEvent.PlayerChangedDimensionEvent}。
     *
     * <p>注意：玩家在<b>异维度重生</b>时不会走到这里（重生走
     * {@code PlayerRespawnEvent}，见 {@link PlayerRespawnEvent}）；重生后的新玩家
     * 本就是全新状态机，无需在此处理。</p>
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getMainHandItem().getItem() instanceof IGun) {
            IGunOperator.fromLivingEntity(player).initialData();
        }
    }
}
