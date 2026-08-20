package com.tacz.guns.block;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.resource.CommonAssetsManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * Common interaction and multi-block lifecycle for the gun-smith tables.
 *
 * <p>The menu carries the data-pack index id rather than the physical block id. A single
 * registered workbench block can represent several gun-pack table definitions, so the id is
 * resolved from the placed item when possible and otherwise from the block-index mapping.</p>
 */
public abstract class AbstractGunSmithTableBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AbstractGunSmithTableBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack usedStack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return openMenu(state, level, pos, player);
    }

    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockPos rootPos = getRootPos(pos, state);
            BlockState rootState = level.getBlockState(rootPos);
            Identifier tableId = resolveTableId(level, rootPos, rootState);
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, ignoredPlayer) -> new GunSmithTableMenu(containerId, inventory, tableId),
                    Component.translatable("block.tacz.gun_smith_table")
            );
            serverPlayer.openMenu(provider, buffer -> buffer.writeIdentifier(tableId));
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /**
     * Preserve the data-pack id carried by a workbench item when the root block is placed.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && isRoot(state)
                && level.getBlockEntity(pos) instanceof GunSmithTableBlockEntity table
                && stack.getItem() instanceof BlockItemDataAccessor accessor) {
            Identifier tableId = accessor.getBlockId(stack);
            if (!DefaultAssets.EMPTY_BLOCK_ID.equals(tableId)) {
                table.setId(tableId);
            }
        }
    }

    private Identifier resolveTableId(Level level, BlockPos rootPos, BlockState rootState) {
        if (level.getBlockEntity(rootPos) instanceof GunSmithTableBlockEntity table && table.getId() != null
                && CommonAssetsManager.get().getBlockIndex(table.getId()) != null) {
            return table.getId();
        }

        Identifier physicalId = BuiltInRegistries.BLOCK.getKey(rootState.getBlock());
        return CommonAssetsManager.get().getAllBlocks().stream()
                .filter(entry -> physicalId.equals(entry.getValue().getPojo().getId()))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(physicalId);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState blockState) {
        return isRoot(blockState) ? new GunSmithTableBlockEntity(pos, blockState) : null;
    }

    @Nullable
    public BlockPos getCompanionPos(BlockPos rootPos, BlockState rootState) {
        return null;
    }

    @Nullable
    public BlockState getCompanionState(BlockState rootState) {
        return null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide() || state.is(oldState.getBlock()) || !isRoot(state)) {
            return;
        }
        BlockPos companionPos = getCompanionPos(pos, state);
        BlockState companionState = getCompanionState(state);
        if (companionPos != null && companionState != null
                && level.getWorldBorder().isWithinBounds(companionPos)
                && !level.isOutsideBuildHeight(companionPos)
                && level.getBlockState(companionPos).canBeReplaced()) {
            level.setBlock(companionPos, companionState, Block.UPDATE_ALL);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    public abstract boolean isRoot(BlockState blockState);

    public float parseRotation(Direction direction) {
        return 90.0F * (3 - direction.get2DDataValue()) - 90;
    }

    public abstract BlockPos getRootPos(BlockPos pos, BlockState blockState);
}
