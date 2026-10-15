package com.timeywimey.poolrooms.world;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import com.timeywimey.poolrooms.Poolrooms;

/**
 * Builds the Poolrooms one chunk at a time. The dimension starts as solid tiles (a flat world); this feature carves
 * the room that covers each chunk. It only ever writes inside its own chunk.
 * <p>
 * The tiles must be in the {@code minecraft:blocks_motion_no_leaves} block tag: in 26.3 the flat generator places any
 * layer block outside that tag with a late "fill the air" feature, which would fill the rooms back in.
 */
public record RoomsFeature() implements Feature {
	public static final MapCodec<RoomsFeature> CODEC = MapCodec.unit(new RoomsFeature());

	@Override
	public MapCodec<RoomsFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		long seed = level.getSeed();
		Layout.Room room = Layout.roomAt(seed, chunkX, chunkZ);
		try {
			buildChunk(level, seed, room, chunkX, chunkZ);
		} catch (RuntimeException e) {
			Poolrooms.LOGGER.error("Could not build the {} at chunk {}, {}", room.type().title, chunkX, chunkZ, e);
		}
		return true;
	}

	/** Builds the part of {@code room} that lies in chunk (chunkX, chunkZ). Also used by the game tests. */
	public static void buildChunk(WorldGenLevel level, long seed, Layout.Room room, int chunkX, int chunkZ) {
		Rooms.build(new Canvas(level, seed, room, chunkX, chunkZ));
	}
}
