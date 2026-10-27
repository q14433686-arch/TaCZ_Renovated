package com.tacz.guns.mixin.carryon;

import com.tacz.guns.block.AbstractGunSmithTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

/** Resolves valid invisible companion halves to their root during Carry On pickup. */
@Pseudo
@Mixin(targets = "tschipp.carryon.common.carry.PickupHandler", remap = false)
public abstract class CarryOnPickupHandlerMixin {
    // Carry On checks reach and calls its general-eligibility helper before this first direct
    // getCarryData call. Preserve the clicked position for reach, then redirect block state,
    // block-entity, permission and removal work to the root.
    @ModifyVariable(method = "tryPickUpBlock", at = @At(value = "INVOKE",
            target = "Ltschipp/carryon/common/carry/CarryOnDataManager;getCarryData(Lnet/minecraft/world/entity/player/Player;)Ltschipp/carryon/common/carry/CarryOnData;",
            ordinal = 0, remap = false), argsOnly = true, ordinal = 0, require = 0)
    private static BlockPos tacz$resolveRootTablePart(BlockPos pos, ServerPlayer player,
                                                       BlockPos originalPos, Level level) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractGunSmithTableBlock table && !table.isRoot(state)
                && tacz$hasMatchingRoot(level, pos, state, table)) {
            return table.getRootPos(pos, state);
        }
        return pos;
    }

    // pickupAllBlocks can bypass Carry On's block-entity requirement. Reject a detached companion
    // instead of carrying it as an unrelated single block.
    @Inject(method = "tryPickUpBlock", at = @At(value = "INVOKE",
            target = "Ltschipp/carryon/common/carry/CarryOnDataManager;getCarryData(Lnet/minecraft/world/entity/player/Player;)Ltschipp/carryon/common/carry/CarryOnData;",
            ordinal = 0, remap = false), cancellable = true, require = 0)
    private static void tacz$rejectOrphanTablePart(ServerPlayer player, BlockPos pos, Level level,
                                                    BiFunction<BlockState, BlockPos, Boolean> pickupCallback,
                                                    CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractGunSmithTableBlock table && !table.isRoot(state)
                && !tacz$hasMatchingRoot(level, pos, state, table)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean tacz$hasMatchingRoot(Level level, BlockPos pos, BlockState state,
                                                 AbstractGunSmithTableBlock table) {
        BlockPos rootPos = table.getRootPos(pos, state);
        BlockState rootState = level.getBlockState(rootPos);
        return rootState.is(table) && table.isRoot(rootState)
                && pos.equals(table.getCompanionPos(rootPos, rootState))
                && state.equals(table.getCompanionState(rootState));
    }
}
