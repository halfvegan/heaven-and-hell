package com.timeywimey.poolrooms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

/** The Poolrooms: an endless maze of tiled pools. Find three wristbands, find the exit, and don't run. */
public class Poolrooms implements ModInitializer {
	public static final String MOD_ID = "poolrooms";
	public static final Logger LOGGER = LoggerFactory.getLogger("The Poolrooms");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("The water is lovely today.");
	}
}
