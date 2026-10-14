package com.timeywimey.poolrooms.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.timeywimey.poolrooms.Poolrooms;

/** The Poolrooms dimension and its fixed heights. */
public final class Pool {
	private Pool() {
	}

	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, Poolrooms.id("poolrooms"));

	/** The floor tiles are at this height; you stand at {@link #WALK}. */
	public static final int FLOOR = 48;
	public static final int WALK = 49;

	/** Where you arrive: the Entrance Pool (cell 0, 0), facing the welcome sign and the pool rules. */
	public static final Vec3 ARRIVAL = new Vec3(8.5, WALK, 4.5);
	public static final float ARRIVAL_YAW = 180.0F;

	public static ServerLevel level(MinecraftServer server) {
		return server.getLevel(DIMENSION);
	}

	public static boolean isPoolrooms(Level level) {
		return DIMENSION.equals(level.dimension());
	}
}
