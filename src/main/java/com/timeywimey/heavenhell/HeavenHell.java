package com.timeywimey.heavenhell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import com.timeywimey.heavenhell.command.HHCommands;
import com.timeywimey.heavenhell.hell.HellLife;
import com.timeywimey.heavenhell.hell.LuciferBattle;
import com.timeywimey.heavenhell.heaven.HeavenLife;
import com.timeywimey.heavenhell.moment.Moments;
import com.timeywimey.heavenhell.registry.ModAttachments;
import com.timeywimey.heavenhell.registry.ModBlocks;
import com.timeywimey.heavenhell.registry.ModEntities;
import com.timeywimey.heavenhell.registry.ModItems;
import com.timeywimey.heavenhell.registry.ModTabs;
import com.timeywimey.heavenhell.soul.Judgment;
import com.timeywimey.heavenhell.soul.KarmaEvents;
import com.timeywimey.heavenhell.util.Scheduler;

/**
 * Heaven &amp; Hell: moral choices change your karma, and karma decides where your soul goes when you die.
 */
public class HeavenHell implements ModInitializer {
	public static final String MOD_ID = "heavenhell";
	public static final Logger LOGGER = LoggerFactory.getLogger("Heaven & Hell");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModBlocks.init();
		ModItems.init();
		ModEntities.init();
		ModTabs.init();
		ModAttachments.init();

		Scheduler.init();
		HHCommands.init();
		KarmaEvents.init();
		Moments.init();
		Judgment.init();
		HeavenLife.init();
		HellLife.init();
		LuciferBattle.init();

		LOGGER.info("Heaven & Hell is watching your deeds.");
	}
}
