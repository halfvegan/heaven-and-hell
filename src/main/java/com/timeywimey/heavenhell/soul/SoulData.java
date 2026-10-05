package com.timeywimey.heavenhell.soul;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Everything the mod remembers about one player's soul. Stored on the player (survives death).
 */
public final class SoulData {
	public static final String LIVING = "living";
	public static final String HEAVEN = "heaven";
	public static final String HELL = "hell";

	public static final int FLAG_HEAVEN_GIFTS = 1;
	public static final int FLAG_BOOK = 2;
	public static final int FLAG_SEEN_HEAVEN = 4;
	public static final int FLAG_SEEN_HELL = 8;
	public static final int FLAG_FIRST_MOMENT_DONE = 16;
	public static final int FLAG_KEY = 32;

	/** Where to send the player when they go back to the living world. */
	public record ReturnPoint(String dimension, double x, double y, double z) {
		public static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("dimension").forGetter(ReturnPoint::dimension),
				Codec.DOUBLE.fieldOf("x").forGetter(ReturnPoint::x),
				Codec.DOUBLE.fieldOf("y").forGetter(ReturnPoint::y),
				Codec.DOUBLE.fieldOf("z").forGetter(ReturnPoint::z)
		).apply(i, ReturnPoint::new));
	}

	public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("karma", 0).forGetter(d -> d.karma),
			Codec.INT.optionalFieldOf("good_deeds", 0).forGetter(d -> d.goodDeeds),
			Codec.INT.optionalFieldOf("sins", 0).forGetter(d -> d.sins),
			Codec.STRING.optionalFieldOf("realm", LIVING).forGetter(d -> d.realm),
			Codec.STRING.optionalFieldOf("pending", "").forGetter(d -> d.pending),
			Codec.INT.optionalFieldOf("rank", 0).forGetter(d -> d.rank),
			Codec.BOOL.optionalFieldOf("freed", false).forGetter(d -> d.freed),
			Codec.LONG.optionalFieldOf("next_moment", 0L).forGetter(d -> d.nextMoment),
			Codec.INT.optionalFieldOf("moments", 0).forGetter(d -> d.moments),
			Codec.INT.optionalFieldOf("flags", 0).forGetter(d -> d.flags),
			ReturnPoint.CODEC.optionalFieldOf("return_point").forGetter(d -> Optional.ofNullable(d.returnPoint)),
			Codec.LONG.optionalFieldOf("blessing_ready", 0L).forGetter(d -> d.blessingReady),
			Codec.STRING.listOf().optionalFieldOf("deeds", List.of()).forGetter(d -> d.deeds)
	).apply(i, SoulData::new));

	public int karma;
	public int goodDeeds;
	public int sins;
	public String realm;
	public String pending;
	public int rank;
	public boolean freed;
	public long nextMoment;
	public int moments;
	public int flags;
	public ReturnPoint returnPoint;
	public long blessingReady;
	public List<String> deeds;

	public SoulData() {
		this(0, 0, 0, LIVING, "", 0, false, 0L, 0, 0, Optional.empty(), 0L, List.of());
	}

	private SoulData(int karma, int goodDeeds, int sins, String realm, String pending, int rank, boolean freed,
			long nextMoment, int moments, int flags, Optional<ReturnPoint> returnPoint, long blessingReady, List<String> deeds) {
		this.karma = karma;
		this.goodDeeds = goodDeeds;
		this.sins = sins;
		this.realm = realm;
		this.pending = pending;
		this.rank = rank;
		this.freed = freed;
		this.nextMoment = nextMoment;
		this.moments = moments;
		this.flags = flags;
		this.returnPoint = returnPoint.orElse(null);
		this.blessingReady = blessingReady;
		this.deeds = new ArrayList<>(deeds);
	}

	public boolean has(int flag) {
		return (flags & flag) != 0;
	}

	public void set(int flag) {
		flags |= flag;
	}

	public boolean inHeaven() {
		return HEAVEN.equals(realm);
	}

	public boolean inHell() {
		return HELL.equals(realm);
	}

	/** Remembers a deed for the Book of Deeds (newest first, at most 10). */
	public void remember(String deed) {
		deeds.add(0, deed);
		while (deeds.size() > 10) {
			deeds.remove(deeds.size() - 1);
		}
	}
}
