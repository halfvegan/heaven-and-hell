package com.timeywimey.poolrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block that faces the player who placed it. Optionally has a custom shape, given for the block facing north and
 * rotated for the other directions. Used for pool chairs, life rings and the EXIT sign.
 */
public class FacingBlock extends HorizontalDirectionalBlock {
	private final VoxelShape north;
	private final VoxelShape east;
	private final VoxelShape south;
	private final VoxelShape west;

	/**
	 * @param box the shape in pixels for the block facing north (x0, y0, z0, x1, y1, z1), or null for a full cube.
	 */
	public FacingBlock(Properties properties, double[] box) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
		if (box == null) {
			north = east = south = west = Shapes.block();
		} else {
			north = Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
			south = Block.box(16 - box[3], box[1], 16 - box[5], 16 - box[0], box[4], 16 - box[2]);
			east = Block.box(16 - box[5], box[1], box[0], 16 - box[2], box[4], box[3]);
			west = Block.box(box[2], box[1], 16 - box[3], box[5], box[4], 16 - box[0]);
		}
	}

	public FacingBlock(Properties properties) {
		this(properties, null);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case EAST -> east;
			case SOUTH -> south;
			case WEST -> west;
			default -> north;
		};
	}
}
