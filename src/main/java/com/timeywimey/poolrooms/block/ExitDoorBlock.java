package com.timeywimey.poolrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.timeywimey.poolrooms.gameplay.PoolTravel;

/**
 * The EXIT door: 2 blocks wide and 3 tall. {@link #PART} is column + 2 * row, counted from the bottom-left as you look
 * at the door. Right-click it while carrying the blue, yellow and red wristbands to open it.
 */
public class ExitDoorBlock extends FacingBlock {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

	public ExitDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(PART);
	}

	/** The bottom-left block of the door this part belongs to. */
	public static BlockPos origin(BlockPos pos, BlockState state) {
		int part = state.getValue(PART);
		Direction facing = state.getValue(FACING);
		return pos.relative(facing.getCounterClockWise(), -(part % 2)).below(part / 2);
	}

	/** Where part {@code part} of a door with this origin and facing goes. */
	public static BlockPos partPos(BlockPos origin, Direction facing, int part) {
		return origin.relative(facing.getCounterClockWise(), part % 2).above(part / 2);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			PoolTravel.useExitDoor(serverPlayer, pos, state);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return useWithoutItem(state, level, pos, player, hit);
	}
}
