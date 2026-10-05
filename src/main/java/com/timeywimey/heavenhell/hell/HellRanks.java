package com.timeywimey.heavenhell.hell;

/** The ladder a damned soul climbs to live lavishly in Hell. */
public final class HellRanks {
	private HellRanks() {
	}

	public record Rank(int level, String name, String color, int cost, String perk, String reward, String quarters) {
	}

	public static final Rank[] RANKS = {
			new Rank(0, "Lost Soul", "gray", 0,
					"Endless hunger gnaws at you.", "", ""),
			new Rank(1, "Imp", "red", 8,
					"Fire Resistance - the flames no longer burn you, and the hunger fades.",
					"Infernal Pickaxe and the Palace Key", "a cramped cell in the Tower of Sin"),
			new Rank(2, "Fiend", "red", 20,
					"Haste - you dig through Hell with ease.",
					"Hellfire Sword", "a furnished chamber"),
			new Rank(3, "Demon", "dark_red", 40,
					"Strength - and the right to challenge Lucifer himself.",
					"Demon Wings", "a lavish suite with a view of the lava sea"),
			new Rank(4, "Archdemon", "dark_red", 80,
					"Speed and Regeneration.",
					"Infernal Crown", "a grand hall of gold and netherite"),
			new Rank(5, "Prince of Hell", "gold", 160,
					"Resistance - and true flight while wearing Demon Wings.",
					"The Prince's Treasury", "the penthouse at the top of the Tower of Sin")
	};

	public static final int MAX = RANKS.length - 1;
	public static final int CHALLENGE_RANK = 3;

	public static Rank get(int level) {
		return RANKS[Math.max(0, Math.min(MAX, level))];
	}
}
