package com.timeywimey.poolrooms.gameplay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.timeywimey.poolrooms.Poolrooms;

/** World settings (saved with the world). */
public final class PoolState extends SavedData {
	public static final Codec<PoolState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("lifeguard", true).forGetter(s -> s.lifeguard)
	).apply(i, PoolState::new));

	public static final SavedDataType<PoolState> TYPE = new SavedDataType<>(Poolrooms.id("settings"), PoolState::new, CODEC, null);

	/** Whether the Lifeguard is on duty (on by default). */
	public boolean lifeguard = true;

	public PoolState() {
	}

	private PoolState(boolean lifeguard) {
		this.lifeguard = lifeguard;
	}

	public static PoolState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public void changed() {
		setDirty();
	}
}
