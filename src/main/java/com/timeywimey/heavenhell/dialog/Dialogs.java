package com.timeywimey.heavenhell.dialog;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import com.timeywimey.heavenhell.hell.HellLife;
import com.timeywimey.heavenhell.hell.HellRanks;
import com.timeywimey.heavenhell.moment.MomentType;
import com.timeywimey.heavenhell.soul.Karma;
import com.timeywimey.heavenhell.soul.Soul;
import com.timeywimey.heavenhell.soul.SoulData;
import com.timeywimey.heavenhell.util.Txt;

/** All the screens the mod shows. */
public final class Dialogs {
	private Dialogs() {
	}

	private static final RandomSource RANDOM = RandomSource.create();

	// ------------------------------------------------------------- moments

	public static void moment(ServerPlayer player, MomentType type, String token) {
		DialogBuilder.of(Txt.of("✧ A Moment of Choice ✧", "aqua").bold())
				.picture(type.vision, 128)
				.text(Txt.of(type.story, "white"), 300)
				.text(Txt.of("Time stands still. Whatever you choose will be remembered.", "gray").italic(), 300)
				.buttonWidth(150)
				.button(Txt.of(type.goodLabel, "green").bold(), Txt.of(type.goodTip), "heavenhell choose " + token + " good")
				.button(Txt.of(type.evilLabel, "red").bold(), Txt.of(type.evilTip), "heavenhell choose " + token + " evil")
				.mustChoose()
				.show(player);
	}

	// ------------------------------------------------------------ judgment

	public static void judgment(ServerPlayer player, String dest, boolean died, boolean firstTime) {
		SoulData soul = Soul.get(player);
		boolean heaven = SoulData.HEAVEN.equals(dest);
		DialogBuilder d = DialogBuilder.of(heaven ? Txt.of("✦ Judgment ✦", "gold").bold() : Txt.of("☠ Judgment ☠", "dark_red").bold())
				.picture(heaven ? "heavenhell:vision_heaven" : "heavenhell:vision_hell", 64)
				.text(Txt.of("Your soul has been weighed. ", "white").then(Karma.describe(soul))
						.then("   Good deeds: " + soul.goodDeeds + "   Sins: " + soul.sins, "gray"));
		if (heaven) {
			d.text(Txt.of(firstTime
					? "The scales tip toward the light. Welcome to Heaven - a land of peace, wonders and endless day."
					: "You wake once more beneath the Pearly Gates.", "yellow"));
			d.text(Txt.of("Explore the Hall of Wonders. The Gatekeeper can send you back to the living world.", "gray"));
			d.button(Txt.of("Enter Paradise", "gold").bold(), null, null);
		} else {
			d.text(Txt.of(firstTime
					? "The scales sink into darkness. You have been cast into Hell."
					: "Death is no escape. You wake again at the Gates of Hell.", "red"));
			d.text(Txt.of("Offer Soul Shards to Lucifer to rise through the ranks and live lavishly... then challenge him for your freedom.", "gray"));
			d.button(Txt.of("Enter the Inferno", "red").bold(), null, null);
		}
		d.columns(1).show(player);
	}

	// -------------------------------------------------------------- heaven

	public static void gatekeeper(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		DialogBuilder d = DialogBuilder.of(Txt.of("The Gatekeeper", "gold").bold())
				.picture("heavenhell:vision_gatekeeper", 64)
				.text(Txt.of("\"Welcome, child, to the Gates of Heaven. Here there is no hunger, no pain and no night. "
						+ "Rest a while. Explore the Hall of Wonders. Walk in the gardens.\"", "white"))
				.text(Txt.of("\"But if your heart still longs for the living world, I can send you back.\"", "white"))
				.button(Txt.of("✦ Stay in Paradise", "gold"), Txt.of(soul.has(SoulData.FLAG_HEAVEN_GIFTS)
						? "Stay and rest" : "The Gatekeeper has a gift for newcomers"), "heavenhell action stay")
				.button(Txt.of("↩ Return to the living world", "aqua"), Txt.of("Wake up back in the Overworld"),
						"heavenhell action return_confirm")
				.button(Txt.of("? Tell me about Heaven", "yellow"), null, "heavenhell action heaven_info")
				.button(Txt.of("Farewell", "gray"), null, null);
		d.show(player);
	}

	public static void returnConfirm(ServerPlayer player) {
		DialogBuilder.of(Txt.of("Return to the living world?", "aqua").bold())
				.text(Txt.of("You will wake where you last rested. Your karma comes with you.", "white"))
				.text(Txt.of("The only way back to Heaven is to live a good life... and die again.", "gray").italic())
				.button(Txt.of("Yes, take me back", "aqua").bold(), null, "heavenhell action return")
				.button(Txt.of("Stay in Heaven", "gold"), null, null)
				.show(player);
	}

	public static void heavenInfo(ServerPlayer player) {
		DialogBuilder.of(Txt.of("Wonders of Heaven", "gold").bold())
				.itemLine("heavenhell:angel_wings", Txt.of("Angel Wings - fly freely in Heaven, glide like an elytra anywhere else."))
				.itemLine("heavenhell:halo", Txt.of("Halo - wear it to heal over time and see in the dark."))
				.itemLine("heavenhell:harp", Txt.of("Harp of Peace - play it to heal friends and calm monsters."))
				.itemLine("heavenhell:manna", Txt.of("Manna - bread of Heaven. Fills you up and heals."))
				.itemLine("heavenhell:seraph_blade", Txt.of("Seraph Blade - a holy sword that burns demons and the undead."))
				.itemLine("heavenhell:cloud_in_a_bottle", Txt.of("Cloud in a Bottle - throw down a platform of cloud anywhere."))
				.itemLine("heavenhell:feather_of_return", Txt.of("Feather of Return - carries you back to the living world."))
				.text(Txt.of("Find them in the Hall of Wonders, or ask an angel for a blessing.", "gray").italic())
				.button(Txt.of("Thank you", "gold"), null, null)
				.columns(1)
				.show(player);
	}

	private static final String[] ANGEL_LINES = {
			"Peace be with you, traveler.",
			"The gardens are lovely this time of... well, it is always this time.",
			"Have you seen the Hall of Wonders? The wings there were made for souls like yours.",
			"Even the smallest kindness echoes forever here.",
			"No night, no hunger, no fear. Rest. You have earned it.",
			"Sometimes I watch the living world through the clouds. It looks so busy."
	};

	public static void angel(ServerPlayer player) {
		String line = ANGEL_LINES[RANDOM.nextInt(ANGEL_LINES.length)];
		DialogBuilder.of(Txt.of("Angel", "white").bold())
				.text(Txt.of("\"" + line + "\"", "white"))
				.button(Txt.of("✦ Ask for a blessing", "gold"), Txt.of("Angels can bless you once in a while"), "heavenhell action blessing")
				.button(Txt.of("Farewell", "gray"), null, null)
				.noPause()
				.show(player);
	}

	// ---------------------------------------------------------------- hell

	public static void lucifer(ServerPlayer player, boolean battleReady) {
		SoulData soul = Soul.get(player);
		HellRanks.Rank rank = HellRanks.get(soul.rank);
		int shards = HellLife.countShards(player);
		DialogBuilder d = DialogBuilder.of(Txt.of("Lucifer, the Fallen Morningstar", "dark_red").bold())
				.picture("heavenhell:vision_lucifer", 64)
				.text(Txt.of("\"" + luciferGreeting(soul) + "\"", "white"))
				.text(Txt.of("Your rank: ", "gray").then(Txt.of(rank.name(), rank.color()).bold())
						.then("   (" + soul.rank + "/" + HellRanks.MAX + ")", "gray")
						.then("   Soul Shards: ", "gray").then(Txt.of(String.valueOf(shards), "aqua").bold()));
		if (soul.rank < HellRanks.MAX) {
			HellRanks.Rank next = HellRanks.get(soul.rank + 1);
			d.text(Txt.of("Next: ", "gray").then(Txt.of(next.name(), next.color()).bold())
					.then(" - " + next.perk(), "white").then("  Reward: " + next.reward() + ".", "yellow"));
			d.button(Txt.of("⬆ Offer " + next.cost() + " Soul Shards", shards >= next.cost() ? "green" : "dark_gray").bold(),
					Txt.of(shards >= next.cost() ? "Rise to " + next.name() : "You need " + (next.cost() - shards) + " more shards"),
					"heavenhell action rankup");
		}
		if (soul.rank >= 1) {
			d.button(Txt.of("⌂ Go to my quarters", "gold"), Txt.of("Your room in the Tower of Sin"), "heavenhell action quarters");
		}
		if (soul.rank >= HellRanks.CHALLENGE_RANK && battleReady) {
			d.button(Txt.of("⚔ Challenge Lucifer", "dark_red").bold(), Txt.of("Defeat him to open the Redemption Gate"),
					"heavenhell action challenge_confirm");
		} else if (soul.rank < HellRanks.CHALLENGE_RANK) {
			d.text(Txt.of("Only a Demon or higher may challenge the throne.", "dark_gray").italic());
		}
		d.button(Txt.of("Leave", "gray"), null, null);
		d.show(player);
	}

	private static String luciferGreeting(SoulData soul) {
		if (soul.freed) {
			return "You again? The gate is open, little rebel. Leave... or stay and serve me.";
		}
		return switch (soul.rank) {
			case 0 -> "Another lost soul for my collection. Bring me Soul Shards and perhaps I will make something of you.";
			case 1 -> "An imp. Barely worth my notice. Barely.";
			case 2 -> "A fiend with ambition. I like ambition. It burns so brightly.";
			case 3 -> "A demon now. Careful, little one. Ambition is how I ended up here.";
			case 4 -> "An archdemon at my court. You almost remind me of myself.";
			default -> "A Prince of Hell. Shall we see if the student can surpass the master?";
		};
	}

	public static void challengeConfirm(ServerPlayer player) {
		DialogBuilder.of(Txt.of("Challenge Lucifer?", "dark_red").bold())
				.picture("heavenhell:vision_lucifer", 64)
				.text(Txt.of("\"You would fight me? In my own court?\" Lucifer rises from his throne, wings unfolding.", "white"))
				.text(Txt.of("If you fall, you wake at the Gates of Hell. If you win, the Redemption Gate behind the throne will open.", "gray"))
				.button(Txt.of("⚔ Fight!", "red").bold(), null, "heavenhell action challenge")
				.button(Txt.of("Not yet", "gray"), null, null)
				.show(player);
	}

	public static void rankUp(ServerPlayer player, HellRanks.Rank rank) {
		DialogBuilder.of(Txt.of("You have risen: ", "gold").then(Txt.of(rank.name(), rank.color()).bold()))
				.picture("heavenhell:vision_lucifer", 64)
				.text(Txt.of("\"Rise, " + rank.name() + ". Enjoy the comforts of my court.\"", "white"))
				.text(Txt.of("Perk: ", "gold").then(rank.perk(), "white"))
				.text(Txt.of("Reward: ", "gold").then(rank.reward(), "white"))
				.text(Txt.of("Your quarters: ", "gold").then(rank.quarters() + ". Use the Palace Key or ask Lucifer to go there.", "white"))
				.button(Txt.of("Excellent", "gold").bold(), null, null)
				.columns(1)
				.show(player);
	}

	// --------------------------------------------------------------- deeds

	public static void deeds(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		DialogBuilder d = DialogBuilder.of(Txt.of("Book of Deeds", "gold").bold())
				.picture("heavenhell:vision_scales", 64)
				.text(Karma.describe(soul))
				.text(Txt.of("Good deeds: " + soul.goodDeeds + "   Sins: " + soul.sins, "gray"));
		if (SoulData.LIVING.equals(soul.realm)) {
			String fate = Karma.fate(soul.karma);
			d.text(Txt.of("If you died right now, your soul would go to ", "gray")
					.then(Txt.of(fate, "Heaven".equals(fate) ? "gold" : "dark_red").bold()).then(".", "gray"));
		} else if (soul.inHeaven()) {
			d.text(Txt.of("You rest in Heaven.", "gold"));
		} else if (soul.inHell()) {
			HellRanks.Rank rank = HellRanks.get(soul.rank);
			d.text(Txt.of("You are in Hell. Rank: ", "red").then(Txt.of(rank.name(), rank.color()).bold())
					.then(soul.freed ? "   (Lucifer defeated - the gate is open to you)" : "", "yellow"));
		}
		if (!soul.deeds.isEmpty()) {
			Txt list = Txt.of("Recent deeds:", "white");
			for (String deed : soul.deeds) {
				list.then("\n" + deed, deed.startsWith("+") ? "green" : "red");
			}
			d.text(list, 260);
		} else {
			d.text(Txt.of("No deeds recorded yet. Choices will find you.", "gray").italic());
		}
		d.button(Txt.of("Close", "gray"), null, null).columns(1).noPause().show(player);
	}
}
