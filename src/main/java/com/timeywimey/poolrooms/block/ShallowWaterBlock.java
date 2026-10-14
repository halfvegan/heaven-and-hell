package com.timeywimey.poolrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ankle-deep water on the floor of the flooded rooms. It only looks (and sounds) like water: you walk through it at
 * normal speed, it never spreads, and it splashes a little around your feet.
 */
public class ShallowWaterBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);

	public ShallowWaterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return true;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects,
			boolean intersects) {
		if (!level.isClientSide()) {
			return;
		}
		Vec3 motion = entity.getDeltaMovement();
		if (motion.horizontalDistanceSqr() > 0.002 && level.getRandom().nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SPLASH, entity.getX() + (level.getRandom().nextDouble() - 0.5) * 0.6, pos.getY() + 0.2,
					entity.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.6, 0.0, 0.05, 0.0);
		}
	}
}
