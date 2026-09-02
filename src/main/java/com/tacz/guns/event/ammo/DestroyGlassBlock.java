package com.tacz.guns.event.ammo;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 子弹命中玻璃类方块时按配置 {@code DestroyGlass} 击碎它。
 *
 * <p>{@code AmmoHitBlockEvent} 发布点与本仓 {@link BellRing} 相同
 * （{@code EntityKineticBullet#onHitBlock}，游戏总线、仅服务端）。
 * {@code BlockState#instrument()} 返回 {@code NoteBlockInstrument} 按本线
 * Mojang 官方 mappings 命名；本文件在 CI 编译门已通过为该签名存在的直接证据。</p>
 */
@EventBusSubscriber(modid = GunMod.MOD_ID)
public class DestroyGlassBlock {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        Level level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getHitResult().getBlockPos();
        EntityKineticBullet ammo = event.getAmmo();
        Block stateBlock = state.getBlock();
        NoteBlockInstrument instrument = state.instrument();
        if (AmmoConfig.DESTROY_GLASS.get() && (stateBlock instanceof HalfTransparentBlock ||
                stateBlock instanceof StainedGlassPaneBlock ||
                (stateBlock instanceof IronBarsBlock && instrument.equals(NoteBlockInstrument.HAT)))) {
            level.destroyBlock(pos, false, ammo.getOwner());
        }
    }
}
