package com.timeywimey.heavenhell.soul;

import net.minecraft.server.level.ServerPlayer;

import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Txt;

/** Adds and describes karma. */
public final class Karma {
	private Karma() {
	}

	public static final int SAINT = 50;
	public static final int VIRTUOUS = 20;
	public static final int WICKED = -20;
	public static final int DAMNED = -50;

	public static String tier(int karma) {
		if (karma >= SAINT) {
			return "Saint";
		}
		if (karma >= VIRTUOUS) {
			return "Virtuous";
		}
		if (karma >= 0) {
			return "Honest Soul";
		}
		if (karma > WICKED) {
			return "Sinner";
		}
		if (karma > DAMNED) {
			return "Wicked";
		}
		return "Damned";
	}

	public static String tierColor(int karma) {
		if (karma >= SAINT) {
			return "gold";
		}
		if (karma >= VIRTUOUS) {
			return "yellow";
		}
		if (karma >= 0) {
			return "white";
		}
		if (karma > WICKED) {
			return "red";
		}
		if (karma > DAMNED) {
			return "dark_red";
		}
		return "dark_purple";
	}

	/** Where this soul would go if it died right now. */
	public static String fate(int karma) {
		return karma >= 0 ? "Heaven" : "Hell";
	}

	/**
	 * Changes a player's karma and tells them about it.
	 *
	 * @param deed short description shown in the Book of Deeds, e.g. "Saved a cow from lava"
	 */
	public static void add(ServerPlayer player, int delta, String deed) {
		if (delta == 0) {
			return;
		}
		SoulData soul = Soul.get(player);
		String oldTier = tier(soul.karma);
		soul.karma += delta;
		if (delta > 0) {
			soul.goodDeeds++;
		} else {
			soul.sins++;
		}
		soul.remember((delta > 0 ? "+" : "") + delta + "  " + deed);
		Soul.save(player, soul);

		if (delta > 0) {
			Cmd.actionbar(player, Txt.of("☀ +" + delta + " Karma", "gold").bold().then("  " + deed, "yellow").str());
			Cmd.sound(player, "minecraft:entity.player.levelup", 0.6F, 1.6F);
		} else {
			Cmd.actionbar(player, Txt.of("☠ " + delta + " Karma", "red").bold().then("  " + deed, "gray").str());
			Cmd.sound(player, "minecraft:entity.wither.ambient", 0.35F, 1.4F);
		}
		String newTier = tier(soul.karma);
		if (!newTier.equals(oldTier)) {
			Cmd.title(player, Txt.of(" ").str(),
					Txt.of("Your soul is now ", "gray").then(Txt.of(newTier, tierColor(soul.karma)).bold()).str(), 10, 50, 20);
		}
		if (soul.karma >= SAINT) {
			Cmd.as(player, "advancement grant @s only heavenhell:saint");
		}
		if (soul.karma <= DAMNED) {
			Cmd.as(player, "advancement grant @s only heavenhell:damned");
		}
	}

	public static Txt describe(SoulData soul) {
		return Txt.of("Karma: ", "gray").then(Txt.of(String.valueOf(soul.karma), tierColor(soul.karma)).bold())
				.then("  (" + tier(soul.karma) + ")", tierColor(soul.karma));
	}
}
