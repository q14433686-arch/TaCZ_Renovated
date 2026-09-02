package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 重生自动装弹（配置 {@code AutoReloadWhenRespawn}）。
 *
 * <p>NeoForge 的 {@link PlayerEvent.PlayerRespawnEvent} 在
 * {@code PlayerList#respawn} 中、新 {@code ServerPlayer} 完成
 * {@code restoreFrom}（含背包恢复）之后触发，{@code event.getEntity()}
 * 即为重生后的新玩家实例；上游 Fabric 侧的 {@code alive} 参数
 * （对应此处 {@code isEndConquered()}）在原实现中即未使用，保持不过滤。
 * 枪械状态机本身的重置由 {@code ServerPlayerMixin#restoreFrom} 负责，与本类无关。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class PlayerRespawnEvent {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 重生自动换弹
        if (!GunConfig.AUTO_RELOAD_WHEN_RESPAWN.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        newPlayer.getInventory().getNonEquipmentItems().forEach(itemStack -> {
            if (!(itemStack.getItem() instanceof IGun)) return;

            var api = new ModernKineticGunScriptAPI();
            api.setItemStack(itemStack);
            api.setShooter(newPlayer);

            // getGunIndex() 可能为 null（枪包未加载/ID 不匹配），必须防御
            var gunIndex = api.getGunIndex();
            if (gunIndex == null) return;

            // 针对背包直读特殊处理
            var reloadType = gunIndex.getGunData().getReloadData().getType();
            var useInventoryAmmo = reloadType == FeedType.INVENTORY;
            // 如果为背包直读则不进行换弹
            if (useInventoryAmmo) {
                return;
            }

            // 针对燃料类型特殊处理
            var isFuel = reloadType == FeedType.FUEL;
            int needAmmoCount = api.getNeededAmmoAmount();

            if (newPlayer.isCreative()) {
                api.putAmmoInMagazine(needAmmoCount);
            } else {
                int consumedAmount = api.consumeAmmoFromPlayer(isFuel ? 1 : needAmmoCount);
                api.putAmmoInMagazine(isFuel ? (needAmmoCount * consumedAmount) : consumedAmount);
            }
        });
    }
}
