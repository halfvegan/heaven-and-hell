package com.timeywimey.poolrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.timeywimey.poolrooms.registry.ModBlocks;

/**
 * An open EXIT door: a doorway full of soft light. Walking into it takes you home. It closes again (turns back into
 * the door) 30 seconds after it opened.
 */
public class ExitLightBlock extends FacingBlock {
	public static final int OPEN_TICKS = 20 * 30;

	public ExitLightBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ExitDoorBlock.PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ExitDoorBlock.PART);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	/** Time is up: become the closed door again. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.setBlock(pos, ModBlocks.EXIT_DOOR.defaultBlockState()
				.setValue(FACING, state.getValue(FACING))
				.setValue(ExitDoorBlock.PART, state.getValue(ExitDoorBlock.PART)), Block.UPDATE_ALL);
	}
}
