package com.timeywimey.poolrooms.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The glowing green EXIT sign above an exit door. Two blocks wide: a left half and a right half. */
public class ExitSignBlock extends FacingBlock {
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");

	public ExitSignBlock(Properties properties) {
		super(properties, new double[] {0.0, 3.0, 14.0, 16.0, 13.0, 16.0});
		registerDefaultState(defaultBlockState().setValue(RIGHT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(RIGHT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(RIGHT, false);
	}
}
