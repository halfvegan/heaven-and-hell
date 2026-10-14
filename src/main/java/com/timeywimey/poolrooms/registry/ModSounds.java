package com.timeywimey.poolrooms.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

import com.timeywimey.poolrooms.Poolrooms;

/** Every sound the mod adds. The audio files are made by tools/audio/make_sounds.py. */
public final class ModSounds {
	private ModSounds() {
	}

	/** Two short whistle blasts: the Lifeguard is within about 30 blocks. Heard up to 48 blocks away. */
	public static final SoundEvent WHISTLE_SHORT = fixed("entity.lifeguard.whistle_short", 48.0F);
	/** One long blast: the Lifeguard saw someone running. */
	public static final SoundEvent WHISTLE_LONG = fixed("entity.lifeguard.whistle_long", 48.0F);
	public static final SoundEvent HUM = variable("ambient.poolrooms.hum");
	public static final SoundEvent DRIP = variable("ambient.poolrooms.drip");
	public static final SoundEvent SPLASH_STEP = variable("block.shallow_water.step");
	public static final SoundEvent DRAIN_SWIRL = variable("block.strange_drain.swirl");
	public static final SoundEvent NOODLE_BOING = variable("item.pool_noodle.boing");
	public static final SoundEvent EXIT_OPEN = variable("block.exit_door.open");
	public static final SoundEvent FADE = variable("event.fade");

	/** Shallow water: soft splashy footsteps. */
	public static final SoundType SHALLOW_WATER = new SoundType(0.35F, 1.0F, SPLASH_STEP, SPLASH_STEP, SPLASH_STEP,
			SPLASH_STEP, SPLASH_STEP);
	/** Pool tiles: a crisp ceramic clink. */
	public static final SoundType TILES = new SoundType(1.0F, 1.25F, SoundEvents.CALCITE_BREAK, SoundEvents.CALCITE_STEP,
			SoundEvents.CALCITE_PLACE, SoundEvents.CALCITE_HIT, SoundEvents.CALCITE_FALL);

	private static SoundEvent variable(String name) {
		Identifier id = Poolrooms.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private static SoundEvent fixed(String name, float range) {
		Identifier id = Poolrooms.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createFixedRangeEvent(id, range));
	}

	public static void init() {
	}
}
