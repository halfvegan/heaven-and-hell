package com.timeywimey.heavenhell.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.timeywimey.heavenhell.HeavenHell;

/** Server-wide settings and progress (saved with the world). */
public final class WorldState extends SavedData {
	public static final Codec<WorldState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("moments_enabled", true).forGetter(s -> s.momentsEnabled),
			Codec.INT.optionalFieldOf("moment_min_minutes", 8).forGetter(s -> s.momentMinMinutes),
			Codec.INT.optionalFieldOf("moment_max_minutes", 14).forGetter(s -> s.momentMaxMinutes),
			Codec.BOOL.optionalFieldOf("judgment_enabled", true).forGetter(s -> s.judgmentEnabled),
			Codec.BOOL.optionalFieldOf("heaven_built", false).forGetter(s -> s.heavenBuilt),
			Codec.BOOL.optionalFieldOf("hell_built", false).forGetter(s -> s.hellBuilt),
			Codec.BOOL.optionalFieldOf("lucifer_alive", false).forGetter(s -> s.luciferAlive),
			Codec.LONG.optionalFieldOf("lucifer_respawn_at", 0L).forGetter(s -> s.luciferRespawnAt),
			Codec.INT.optionalFieldOf("lucifer_defeats", 0).forGetter(s -> s.luciferDefeats),
			Codec.BOOL.optionalFieldOf("gate_open", false).forGetter(s -> s.gateOpen)
	).apply(i, WorldState::new));

	public static final SavedDataType<WorldState> TYPE = new SavedDataType<>(HeavenHell.id("world_state"), WorldState::new, CODEC, null);

	public boolean momentsEnabled = true;
	public int momentMinMinutes = 8;
	public int momentMaxMinutes = 14;
	public boolean judgmentEnabled = true;
	public boolean heavenBuilt;
	public boolean hellBuilt;
	public boolean luciferAlive;
	public long luciferRespawnAt;
	public int luciferDefeats;
	public boolean gateOpen;

	public WorldState() {
	}

	private WorldState(boolean momentsEnabled, int momentMinMinutes, int momentMaxMinutes, boolean judgmentEnabled,
			boolean heavenBuilt, boolean hellBuilt, boolean luciferAlive, long luciferRespawnAt, int luciferDefeats, boolean gateOpen) {
		this.momentsEnabled = momentsEnabled;
		this.momentMinMinutes = momentMinMinutes;
		this.momentMaxMinutes = momentMaxMinutes;
		this.judgmentEnabled = judgmentEnabled;
		this.heavenBuilt = heavenBuilt;
		this.hellBuilt = hellBuilt;
		this.luciferAlive = luciferAlive;
		this.luciferRespawnAt = luciferRespawnAt;
		this.luciferDefeats = luciferDefeats;
		this.gateOpen = gateOpen;
	}

	public static WorldState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public void changed() {
		setDirty();
	}
}
