package com.timeywimey.poolrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.timeywimey.poolrooms.entity.LifeguardEntity;

/**
 * Invisible and walk-through for everyone except the Lifeguard. It sits in the doorways of the Entrance Pool, which is
 * why the Lifeguard can never go in there.
 */
public class LifeguardBarrierBlock extends Block {
	public LifeguardBarrierBlock(Properties properties) {
		super(properties);
	}

	private static boolean isLifeguard(CollisionContext context) {
		return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LifeguardEntity;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return isLifeguard(context) ? Shapes.block() : Shapes.empty();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}
}
