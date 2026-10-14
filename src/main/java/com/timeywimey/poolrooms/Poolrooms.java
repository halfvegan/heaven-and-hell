package com.timeywimey.poolrooms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import com.timeywimey.poolrooms.command.PoolCommands;
import com.timeywimey.poolrooms.gameplay.LifeguardDirector;
import com.timeywimey.poolrooms.gameplay.PoolLife;
import com.timeywimey.poolrooms.gameplay.PoolTravel;
import com.timeywimey.poolrooms.registry.ModAttachments;
import com.timeywimey.poolrooms.registry.ModBlocks;
import com.timeywimey.poolrooms.registry.ModEntities;
import com.timeywimey.poolrooms.registry.ModFeatures;
import com.timeywimey.poolrooms.registry.ModItems;
import com.timeywimey.poolrooms.registry.ModSounds;
import com.timeywimey.poolrooms.registry.ModTabs;
import com.timeywimey.poolrooms.util.Scheduler;

/** The Poolrooms: an endless maze of tiled pools. Find three wristbands, find the exit, and don't run. */
public class Poolrooms implements ModInitializer {
	public static final String MOD_ID = "poolrooms";
	public static final Logger LOGGER = LoggerFactory.getLogger("The Poolrooms");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.init();
		ModBlocks.init();
		ModItems.init();
		ModEntities.init();
		ModTabs.init();
		ModAttachments.init();
		ModFeatures.init();

		Scheduler.init();
		PoolCommands.init();
		PoolTravel.init();
		PoolLife.init();
		LifeguardDirector.init();

		LOGGER.info("The water is lovely today.");
	}
}
