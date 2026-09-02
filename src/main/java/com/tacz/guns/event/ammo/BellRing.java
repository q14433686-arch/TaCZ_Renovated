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
 * 子弹命中钟时敲响它。{@link AmmoHitBlockEvent} 由
 * {@code EntityKineticBullet#onHitBlock} 发到 {@code NeoForge.EVENT_BUS}（仅服务端）。
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
