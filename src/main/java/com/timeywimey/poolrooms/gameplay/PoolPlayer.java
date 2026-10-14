package com.timeywimey.poolrooms.gameplay;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;

import com.timeywimey.poolrooms.registry.ModAttachments;

/** What the mod remembers about a player (saved with the player). */
public final class PoolPlayer {
	public static final int FLAG_WELCOMED = 1;

	/** Where to go when you leave the Poolrooms: the water surface above the drain you came in through. */
	public record ReturnPoint(String dimension, double x, double y, double z) {
		public static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("dimension").forGetter(ReturnPoint::dimension),
				Codec.DOUBLE.fieldOf("x").forGetter(ReturnPoint::x),
				Codec.DOUBLE.fieldOf("y").forGetter(ReturnPoint::y),
				Codec.DOUBLE.fieldOf("z").forGetter(ReturnPoint::z)
		).apply(i, ReturnPoint::new));
	}

	public static final Codec<PoolPlayer> CODEC = RecordCodecBuilder.create(i -> i.group(
			ReturnPoint.CODEC.optionalFieldOf("return_point").forGetter(d -> Optional.ofNullable(d.returnPoint)),
			Codec.INT.optionalFieldOf("flags", 0).forGetter(d -> d.flags),
			Codec.INT.optionalFieldOf("visits", 0).forGetter(d -> d.visits)
	).apply(i, PoolPlayer::new));

	public ReturnPoint returnPoint;
	public int flags;
	public int visits;

	public PoolPlayer() {
		this(Optional.empty(), 0, 0);
	}

	private PoolPlayer(Optional<ReturnPoint> returnPoint, int flags, int visits) {
		this.returnPoint = returnPoint.orElse(null);
		this.flags = flags;
		this.visits = visits;
	}

	public boolean has(int flag) {
		return (flags & flag) != 0;
	}

	public static PoolPlayer get(ServerPlayer player) {
		PoolPlayer data = player.getAttached(ModAttachments.PLAYER);
		if (data == null) {
			data = new PoolPlayer();
			player.setAttached(ModAttachments.PLAYER, data);
		}
		return data;
	}

	/** Call after changing a player's data so it is saved. */
	public static void save(ServerPlayer player, PoolPlayer data) {
		player.setAttached(ModAttachments.PLAYER, data);
	}
}
