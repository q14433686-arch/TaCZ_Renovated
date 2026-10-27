package tschipp.carryon.common.carry;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class PickupHandler {
    private static boolean isBlockInDistance(ServerPlayer player, BlockPos pos) { return pos.equals(player.reachable); }
    private static boolean canCarryGeneral(ServerPlayer player) { return player.canCarry; }
    public static boolean tryPickUpBlock(ServerPlayer player, BlockPos pos, Level level, BiFunction<BlockState, BlockPos, Boolean> callback) {
        if (!isBlockInDistance(player, pos)) return false;
        if (!canCarryGeneral(player)) return false;
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        BlockState state = level.getBlockState(pos);
        return carry != null && (callback == null || callback.apply(state, pos));
    }
}
