package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 主手持枪时禁止原版左键方块交互（服务端兜底）。
 *
 * <p>客户端侧的预防是另一个类：{@code ClientPreventGunClick}
 * （经 {@code ClientGameEvents#onClickInput} 取消
 * {@code InputEvent.InteractionKeyMappingTriggered}，已接线）。
 * 本类对齐官方 1.20.1：不过滤逻辑侧，双端触发时都取消。</p>
 *
 * <p>26.2 的 {@link PlayerInteractEvent.LeftClickBlock#setCanceled(boolean)}
 * 覆写会把 {@code useBlock}/{@code useItem} 同时置为 {@code TriState.FALSE}，
 * 即 {@code setCanceled(true)} 完整阻断本次左键方块行为。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class PreventGunClick {
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // 只要主手有枪，那么禁止交互
        ItemStack itemInHand = event.getEntity().getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            event.setCanceled(true);
        }
    }
}
