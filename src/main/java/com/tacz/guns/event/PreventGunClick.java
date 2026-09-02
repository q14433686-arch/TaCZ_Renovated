package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 主手持枪时禁止攻击/挖掘方块（服务端权威侧）。
 *
 * <p>客户端侧的输入拦截见 {@code ClientPreventGunClick}
 * （{@code InputEvent.InteractionKeyMappingTriggered}）；本类是它的服务端兜底：
 * {@code PlayerInteractEvent.LeftClickBlock} 在
 * {@code ServerPlayerGameMode#handleBlockBreakAction} 开头触发
 * （本线证据：NeoForge 1.21.11 分支
 * {@code patches/net/minecraft/server/level/ServerPlayerGameMode.java.patch} →
 * {@code CommonHooks.onLeftClickBlock(...)}，{@code event.isCanceled()} 时方法
 * 直接 return），因此取消后攻击/挖掘流程不会进入。注意 javadoc 明示：
 * 创造模式直接破坏方块、不走 use 逻辑，与客户端拦截共同覆盖即可。</p>
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
