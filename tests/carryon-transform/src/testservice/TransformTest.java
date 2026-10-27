package testservice;

import com.tacz.guns.block.AbstractGunSmithTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;
import org.spongepowered.asm.service.MixinService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/** Runs the real Mixin transformer on a minimal Carry On-shaped target; not a game test. */
public final class TransformTest {
    private static final class Table extends AbstractGunSmithTableBlock {
        private final int dx;
        private final int dy;
        private Table(int dx, int dy) { this.dx = dx; this.dy = dy; }
        public boolean isRoot(BlockState state) { return state.root; }
        public BlockPos getRootPos(BlockPos pos, BlockState state) {
            return state.root ? pos : new BlockPos(pos.x() - dx, pos.y() - dy, pos.z());
        }
        public BlockPos getCompanionPos(BlockPos pos, BlockState state) {
            return new BlockPos(pos.x() + dx, pos.y() + dy, pos.z());
        }
        public BlockState getCompanionState(BlockState state) { return new BlockState(this, false, state.facing); }
    }

    private static final class Loader extends ClassLoader {
        private Loader() { super(TransformTest.class.getClassLoader()); }
        private Class<?> make(String name, byte[] bytes) { return defineClass(name, bytes, 0, bytes.length); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void testPickup(Method call, int dx, int dy) throws Exception {
        Table table = new Table(dx, dy);
        Level level = new Level();
        BlockPos root = new BlockPos(0, 0, 0);
        BlockPos companion = new BlockPos(dx, dy, 0);
        level.setBlockState(root, new BlockState(table, true, "north"));
        level.setBlockState(companion, new BlockState(table, false, "north"));
        ServerPlayer player = new ServerPlayer();
        player.reachable = companion;
        AtomicReference<BlockPos> accepted = new AtomicReference<>();
        BiFunction<BlockState, BlockPos, Boolean> callback = (state, pos) -> { accepted.set(pos); return true; };

        check((Boolean) call.invoke(null, player, companion, level, callback), "reachable companion failed");
        check(root.equals(accepted.get()), "callback must receive the matching root");
        check(!(Boolean) call.invoke(null, player, root, level, callback), "reach is checked against clicked part");
        player.reachable = root;
        accepted.set(null);
        check((Boolean) call.invoke(null, player, root, level, callback) && root.equals(accepted.get()), "root pickup");
        player.reachable = companion;
        player.canCarry = false;
        check(!(Boolean) call.invoke(null, player, companion, level, callback), "general conditions still apply");
        player.canCarry = true;
        BiFunction<BlockState, BlockPos, Boolean> denied = (state, pos) -> false;
        check(!(Boolean) call.invoke(null, player, companion, level, denied), "root permission callback still applies");

        Level orphan = new Level();
        orphan.setBlockState(companion, new BlockState(table, false, "north"));
        accepted.set(null);
        check(!(Boolean) call.invoke(null, player, companion, orphan, callback), "orphan must be rejected");
        check(accepted.get() == null, "orphan callback must not run");
        orphan.setBlockState(root, new BlockState(table, true, "south"));
        check(!(Boolean) call.invoke(null, player, companion, orphan, callback), "mismatched pair must be rejected");
        orphan.setBlockState(companion, new BlockState(new Block(), false, "other"));
        check((Boolean) call.invoke(null, player, companion, orphan, callback)
                && companion.equals(accepted.get()), "ordinary blocks remain unchanged");
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("mixin.service", "testservice.TestService");
        MixinBootstrap.init();
        Mixins.addConfiguration("pickup-test.mixins.json", (IMixinConfigSource) null);
        String name = "tschipp.carryon.common.carry.PickupHandler";
        byte[] raw = TransformTest.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class").readAllBytes();
        byte[] bytes = ((TestService) MixinService.getService()).transformer().transformClassBytes(name, name, raw);
        check(!Arrays.equals(raw, bytes), "Mixin did not transform the target");
        Class<?> transformed = new Loader().make(name, bytes);
        Method call = transformed.getMethod("tryPickUpBlock", ServerPlayer.class, BlockPos.class, Level.class, BiFunction.class);
        testPickup(call, 1, 0); // horizontal B
        testPickup(call, 0, 1); // vertical C
        System.out.println("Mixin transformer mock pickup test passed (no NeoForge or Carry On runtime)");
    }
}
