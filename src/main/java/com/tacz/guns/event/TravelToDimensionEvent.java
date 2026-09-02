package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 修正跨维度时，枪械数据不刷新的问题，这是服务端的刷新。
 *
 * <h2>症状：跨维度后「客户端演完整套换弹动画、但服务端直接早退不加子弹」</h2>
 *
 * <p>玩家跨维度时是<b>同一个 {@code ServerPlayer} 实例被物理移动</b>过去的
 * （见 NeoForge 26.1.x patch：{@code ServerPlayer#teleport(TeleportTransition)}
 * 跨维度分支成功后调用
 * {@code EventHooks.firePlayerChangedDimensionEvent(this, lastDimension, ...)}），
 * 因此挂在 {@code LivingEntityMixin} 上的 {@code @Unique ShooterDataHolder}
 * 原样保留：{@code reloadStateType}/{@code reloadTimestamp}/{@code isBolting}/
 * {@code shootTimestamp} 全是旧维度的残值。
 * 服务端 {@code LivingEntityReload#reload} 首道门禁
 * {@code data.reloadStateType.isReloading()} 因残留而成立，直接 return；
 * 而客户端那侧的刷新走的是另一条路（{@code RefreshClonePlayerDataEvent#onClientTick}
 * 轮询 {@code Minecraft#player} 实例变化，跨维度会换 {@code LocalPlayer}，
 * 故<b>客户端照常执行</b>），于是换弹动画、音效一切正常，服务端却压根不认。
 * <b>客户端与服务端的不对称</b>正是该症状的成因。</p>
 *
 * <h2>事件选型（NeoForge 26.1.x，证据见 docs/records）</h2>
 *
 * <ul>
 * <li><b>玩家</b>：{@link PlayerEvent.PlayerChangedDimensionEvent} ——
 * 仅在服务端、跨维度传送<b>完成后</b>对同一实例触发，正是需要重置状态机的时机。</li>
 * <li><b>非玩家生物</b>（如持枪僵尸）：NeoForge <b>没有</b> Fabric
 * {@code AFTER_ENTITY_CHANGE_LEVEL} 那样的「实体跨维度完成后」事件。
 * 跨维度时非玩家实体是<b>复制</b>的（vanilla {@code Entity#teleportCrossDimension}
 * 新建实体 + NBT 复制），而枪械状态机存在 mixin 的 {@code @Unique} 字段里、
 * <b>不进 NBT</b>，新实体天然是干净的，并由 {@code LivingEntityMixin} 的
 * tick 兜底（{@code currentGunItem == null} 时自动 {@code initialData()}）完成初始化。
 * 这里仍监听 {@link EntityTravelToDimensionEvent}（传送<b>前</b>、旧实体上触发，
 * cancellable，见其 javadoc）做防御性重置：若未来版本改为复用实例，
 * 状态机也不会把残值带进新维度。仅在维度确实改变时处理，
 * 避免同维度传送（该事件对同维度也会触发）误伤正在换弹的生物。</li>
 * </ul>
 *
 * <h2>为什么这么改是安全的</h2>
 *
 * <p>{@code initialData()}（{@code LivingEntityMixin} 实现）只重置状态机字段，并把
 * {@code currentGunItem} 重新绑定为 {@code () -> getMainHandItem()}、
 * 刷新配件缓存；它<b>不触碰 {@code drawTimestamp}</b>，
 * 因此不会产生切枪冷却 —— 不会重蹈 {@code ServerPlayerMixin} 注释里记录的那次回归
 * （那次是把 {@code currentGunItem} 清成 null，导致
 * {@code shoot} 直接返回 {@code NOT_DRAW}）。玩家死亡/重生走
 * {@code ServerPlayer#restoreFrom}，由 {@code ServerPlayerMixin} 负责，与本类无关。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class TravelToDimensionEvent {
    /**
     * 玩家跨维度：同一个 {@code ServerPlayer} 实例被物理移动，必须显式重置服务端状态机。
     * 该事件仅在逻辑服务端触发。
     */
    @SubscribeEvent
    public static void onPlayerTravelToDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getMainHandItem().getItem() instanceof IGun) {
            IGunOperator.fromLivingEntity(player).initialData();
        }
    }

    /**
     * 非玩家生物跨维度：传送前对旧实体做防御性重置（详见类注释——当前版本实体是复制的，
     * 新实体本就干净；此处防的是「未来改为复用实例」的情况）。
     * 玩家不走这里（上面的事件已覆盖，且玩家传送完成前重置会被后续流程覆盖不到）。
     */
    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 该事件对同维度传送也会触发（javadoc：may be the same dimension），只处理真正跨维度的情况
        if (serverLevel.dimension() == event.getDimension()) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity livingEntity && livingEntity.getMainHandItem().getItem() instanceof IGun) {
            IGunOperator.fromLivingEntity(livingEntity).initialData();
        }
    }
}
