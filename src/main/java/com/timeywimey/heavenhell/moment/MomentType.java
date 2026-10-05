package com.timeywimey.heavenhell.moment;

/** The moral choices that can interrupt your adventures. */
public enum MomentType {
	LAVA_ANIMAL("lava_animal", "vision_lava_cow",
			"A cow has wandered to the very edge of a lava pit. One more step and it will fall in.",
			"❤ Save the cow", "Pull it back from the edge",
			"☠ Let it fall", "Look away and let it happen",
			12, "Saved a cow from the lava", -12, "Let a cow burn in lava"),
	ZOMBIE_AMBUSH("zombie_ambush", "vision_zombie_ambush",
			"A zombie has cornered a terrified villager. Without help, the villager will not survive.",
			"⚔ Save the villager", "Strike the zombie down with holy light",
			"☠ Walk away", "Not your problem",
			12, "Saved a villager from a zombie", -12, "Let a villager be slain"),
	TRAPPED_WOLF("trapped_wolf", "vision_trapped_wolf",
			"A wolf is tangled in cobwebs and thorns, whimpering. It looks at you with pleading eyes.",
			"❤ Free the wolf", "Cut it loose - it might follow you home",
			"☠ Leave it", "Keep walking",
			10, "Freed a trapped wolf", -8, "Abandoned a trapped wolf"),
	HUNGRY_TRAVELER("hungry_traveler", "vision_hungry_traveler",
			"A weary traveler stumbles toward you. \"Please... I haven't eaten in days.\"",
			"❤ Share your food", "Give him something to eat",
			"☠ Rob him", "His pack is full of emeralds...",
			10, "Fed a starving traveler", -12, "Robbed a starving traveler"),
	LOST_SATCHEL("lost_satchel", "vision_lost_satchel",
			"You find a satchel stitched with the name \"Ellie\". It is heavy with a farmer's life savings.",
			"❤ Return it", "Send it back to its owner",
			"☠ Keep it", "Finders keepers",
			10, "Returned a lost satchel", -10, "Kept a farmer's savings"),
	DEVILS_BARGAIN("devils_bargain", "vision_devils_bargain",
			"A hooded stranger steps out of the shadows. \"Three diamonds,\" he whispers, \"for just a little piece of your soul.\"",
			"✦ Refuse", "Your soul is not for sale",
			"☠ Take the diamonds", "What's a little piece?",
			8, "Refused the Devil's bargain", -20, "Sold a piece of your soul");

	public final String id;
	public final String vision;
	public final String story;
	public final String goodLabel;
	public final String goodTip;
	public final String evilLabel;
	public final String evilTip;
	public final int goodKarma;
	public final String goodDeed;
	public final int evilKarma;
	public final String evilDeed;

	MomentType(String id, String vision, String story, String goodLabel, String goodTip, String evilLabel, String evilTip,
			int goodKarma, String goodDeed, int evilKarma, String evilDeed) {
		this.id = id;
		this.vision = "heavenhell:" + vision;
		this.story = story;
		this.goodLabel = goodLabel;
		this.goodTip = goodTip;
		this.evilLabel = evilLabel;
		this.evilTip = evilTip;
		this.goodKarma = goodKarma;
		this.goodDeed = goodDeed;
		this.evilKarma = evilKarma;
		this.evilDeed = evilDeed;
	}

	public static MomentType byId(String id) {
		for (MomentType t : values()) {
			if (t.id.equals(id)) {
				return t;
			}
		}
		return null;
	}
}
