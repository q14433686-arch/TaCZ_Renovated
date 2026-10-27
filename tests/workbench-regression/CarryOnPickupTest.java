package com.tacz.guns.mixin.carryon;

import com.tacz.guns.block.AbstractGunSmithTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

/** Tests the actual mixin handlers with stubs, not a transformed Carry On class. */
public final class CarryOnPickupTest {
    private static final String CARRY_DATA_CALL =
            "Ltschipp/carryon/common/carry/CarryOnDataManager;getCarryData(Lnet/minecraft/world/entity/player/Player;)Ltschipp/carryon/common/carry/CarryOnData;";
    private static final ServerPlayer PLAYER = new ServerPlayer();
    private static final Method REDIRECT;
    private static final Method REJECT;

    static {
        try {
            REDIRECT = CarryOnPickupHandlerMixin.class.getDeclaredMethod("tacz$resolveRootTablePart",
                    BlockPos.class, ServerPlayer.class, BlockPos.class, Level.class);
            REDIRECT.setAccessible(true);
            REJECT = CarryOnPickupHandlerMixin.class.getDeclaredMethod("tacz$rejectOrphanTablePart",
                    ServerPlayer.class, BlockPos.class, Level.class, BiFunction.class, CallbackInfoReturnable.class);
            REJECT.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static BlockPos redirect(Level level, BlockPos clicked) throws Exception {
        return (BlockPos) REDIRECT.invoke(null, clicked, PLAYER, clicked, level);
    }

    private static CallbackInfoReturnable<Boolean> reject(Level level, BlockPos clicked) throws Exception {
        CallbackInfoReturnable<Boolean> callback = new CallbackInfoReturnable<>();
        REJECT.invoke(null, PLAYER, clicked, level, null, callback);
        return callback;
    }

    private static void testCompleteTable(int dx, int dy) throws Exception {
        FakeTable table = new FakeTable(dx, dy);
        Level level = new Level();
        BlockPos root = new BlockPos(1, 0, 0);
        BlockPos companion = new BlockPos(root.x() + dx, root.y() + dy, root.z());
        level.setBlockState(root, new BlockState(table, true, "north"));
        level.setBlockState(companion, new BlockState(table, false, "north"));
        check(redirect(level, root).equals(root), "root click remains root");
        check(redirect(level, companion).equals(root), "companion redirects to matching root");
        check(!reject(level, root).isCancelled(), "root permitted");
        check(!reject(level, companion).isCancelled(), "matching companion permitted");
        // Both possible injector orders are safe: after redirect the guard observes root.
        check(!reject(level, redirect(level, companion)).isCancelled(), "guard after redirect permitted");
    }

    private static void testBrokenTable() throws Exception {
        FakeTable table = new FakeTable(1, 0);
        Level level = new Level();
        BlockPos root = new BlockPos(0, 0, 0);
        BlockPos companion = new BlockPos(1, 0, 0);
        level.setBlockState(companion, new BlockState(table, false, "north"));
        check(redirect(level, companion).equals(companion), "orphan must not redirect to air");
        CallbackInfoReturnable<Boolean> orphan = reject(level, companion);
        check(orphan.isCancelled() && Boolean.FALSE.equals(orphan.getReturnValue()), "orphan pickup denied");
        level.setBlockState(root, new BlockState(new FakeTable(1, 0), true, "north"));
        check(reject(level, companion).isCancelled(), "unrelated table is not taken as root");
        level.setBlockState(root, new BlockState(table, true, "south"));
        check(reject(level, companion).isCancelled(), "mismatched orientation is denied");
        level.setBlockState(companion, new BlockState(new Block(), false, "other"));
        check(redirect(level, companion).equals(companion) && !reject(level, companion).isCancelled(),
                "other mods' blocks are unchanged");
    }

    private static void testSelector() {
        ModifyVariable modify = REDIRECT.getAnnotation(ModifyVariable.class);
        Inject inject = REJECT.getAnnotation(Inject.class);
        check(modify.argsOnly() && modify.ordinal() == 0, "target the BlockPos argument");
        check(modify.at().value().equals("INVOKE") && modify.at().target().equals(CARRY_DATA_CALL)
                        && modify.at().ordinal() == 0,
                "redirect selector targets the first post-reach getCarryData call");
        check(inject.cancellable() && inject.at().target().equals(CARRY_DATA_CALL) && inject.at().ordinal() == 0,
                "orphan guard uses the same post-reach boundary");
    }

    public static void main(String[] args) throws Exception {
        testSelector();
        testCompleteTable(1, 0); // horizontal B
        testCompleteTable(0, 1); // vertical C
        testBrokenTable();
        System.out.println("Carry On pickup handler regression passed (no mixin transformer/game)");
    }

    private static final class FakeTable extends AbstractGunSmithTableBlock {
        private final int dx;
        private final int dy;
        private FakeTable(int dx, int dy) { this.dx = dx; this.dy = dy; }
        @Override public boolean isRoot(BlockState state) { return state.root; }
        @Override public BlockPos getRootPos(BlockPos pos, BlockState state) {
            return state.root ? pos : new BlockPos(pos.x() - dx, pos.y() - dy, pos.z());
        }
        @Override public BlockPos getCompanionPos(BlockPos pos, BlockState state) {
            return new BlockPos(pos.x() + dx, pos.y() + dy, pos.z());
        }
        @Override public BlockState getCompanionState(BlockState state) {
            return new BlockState(this, false, state.facing);
        }
    }
}
