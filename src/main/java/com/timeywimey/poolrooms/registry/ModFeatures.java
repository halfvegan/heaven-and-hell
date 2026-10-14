package com.timeywimey.poolrooms.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.world.RoomsFeature;

/** The world generation feature that builds the rooms (used by data/poolrooms/worldgen/feature/rooms.json). */
public final class ModFeatures {
	private ModFeatures() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Poolrooms.id("rooms"), RoomsFeature.CODEC);
	}
}
