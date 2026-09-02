package com.tacz.guns.event.ammo;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 子弹命中钟块时敲响它。
 *
 * <p>{@code AmmoHitBlockEvent} 是本 mod 自有事件，由
 * {@code EntityKineticBullet#onHitBlock} 发布到 {@code NeoForge#EVENT_BUS}
 * （仅服务端；本线确认：{@code EntityKineticBullet.java} L490-491
 * {@code NeoForge.EVENT_BUS.post(ammoHitBlockEvent)}），与客户端无关。
 * {@code BellBlock#attemptToRing(Level, BlockPos, Direction)} 按本线
 * Mojang 官方 mappings（ModDevGradle 自动接线）命名；本文件在 CI 编译门
 * 已通过为该签名存在的直接证据。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class BellRing {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        Level level = event.getLevel();
        BlockState state = event.getState();
        BlockHitResult hitResult = event.getHitResult();
        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(level, hitResult.getBlockPos(), hitResult.getDirection());
        }
    }
}
