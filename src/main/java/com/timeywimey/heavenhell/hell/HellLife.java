package com.timeywimey.heavenhell.hell;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.timeywimey.heavenhell.dialog.Dialogs;
import com.timeywimey.heavenhell.entity.LuciferEntity;
import com.timeywimey.heavenhell.registry.ModItems;
import com.timeywimey.heavenhell.soul.Soul;
import com.timeywimey.heavenhell.soul.SoulData;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Give;
import com.timeywimey.heavenhell.util.Scheduler;
import com.timeywimey.heavenhell.util.Txt;
import com.timeywimey.heavenhell.world.Layout;
import com.timeywimey.heavenhell.world.Realms;
import com.timeywimey.heavenhell.world.WorldState;

/** Life in Hell: rank perks, rising through the ranks, your quarters, and the Redemption Gate. */
public final class HellLife {
	private HellLife() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(HellLife::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(HellLife::setupTeams);
	}

	private static void tick(MinecraftServer server) {
		int t = server.getTickCount();
		if (t % 10 != 3) {
			return;
		}
		WorldState state = WorldState.get(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!Realms.isHell(player.level())) {
				continue;
			}
			SoulData soul = Soul.get(player);
			if (t % 100 == 3) {
				applyPerks(player, soul.rank);
			}
			if (Layout.REDEMPTION_GATE.contains(player.position())) {
				if (state.gateOpen && soul.freed) {
					escape(player);
				} else {
					Realms.teleport(player, player.level(), new Vec3(player.getX(), player.getY(), -27.0), 180.0F, 0.0F);
					Cmd.actionbar(player, Txt.of("The Redemption Gate will not open for you. Only those who defeat Lucifer may pass.", "red").str());
					Cmd.sound(player, "minecraft:block.respawn_anchor.deplete", 1.0F, 0.6F);
				}
			}
		}
	}

	private static void applyPerks(ServerPlayer player, int rank) {
		if (rank <= 0) {
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0, true, false, true));
			return;
		}
		player.addEffect(effect(MobEffects.FIRE_RESISTANCE, 0));
		if (rank >= 2) {
			player.addEffect(effect(MobEffects.HASTE, 0));
		}
		if (rank >= 3) {
			player.addEffect(effect(MobEffects.STRENGTH, 0));
		}
		if (rank >= 4) {
			player.addEffect(effect(MobEffects.SPEED, 0));
			if (player.getHealth() < player.getMaxHealth()) {
				player.addEffect(effect(MobEffects.REGENERATION, 0));
			}
		}
		if (rank >= 5) {
			player.addEffect(effect(MobEffects.RESISTANCE, 0));
		}
	}

	private static MobEffectInstance effect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type, int amp) {
		return new MobEffectInstance(type, 260, amp, true, false, true);
	}

	// ------------------------------------------------------------ shards

	public static int countShards(ServerPlayer player) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(ModItems.SOUL_SHARD)) {
				n += stack.getCount();
			}
		}
		return n;
	}

	private static void takeShards(ServerPlayer player, int amount) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(ModItems.SOUL_SHARD)) {
				int take = Math.min(amount, stack.getCount());
				stack.shrink(take);
				amount -= take;
			}
		}
	}

	// ------------------------------------------------------------- ranks

	public static boolean nearThrone(ServerPlayer player) {
		return Realms.isHell(player.level()) && player.position().distanceTo(Layout.THRONE_SEAT) < 14.0;
	}

	public static void talkToLucifer(ServerPlayer player, LuciferEntity lucifer) {
		if (!Realms.isHell(player.level())) {
			Cmd.actionbar(player, Txt.of("\"Not here. Come and see me in my court.\"", "dark_red").italic().str());
			return;
		}
		Dialogs.lucifer(player, LuciferBattle.canStart(player.level()));
	}

	public static void rankUp(ServerPlayer player) {
		if (!nearThrone(player)) {
			Cmd.actionbar(player, Txt.of("Only Lucifer, on his throne, can raise your rank.", "red").str());
			return;
		}
		SoulData soul = Soul.get(player);
		if (soul.rank >= HellRanks.MAX) {
			Cmd.actionbar(player, Txt.of("There is no rank higher than yours - except his.", "gold").str());
			return;
		}
		HellRanks.Rank next = HellRanks.get(soul.rank + 1);
		int have = countShards(player);
		if (have < next.cost()) {
			Cmd.actionbar(player, Txt.of("\"" + next.cost() + " Soul Shards, not " + have + ". Come back when you can pay.\"", "red").italic().str());
			Cmd.sound(player, "minecraft:entity.villager.no", 1.0F, 0.5F);
			return;
		}
		takeShards(player, next.cost());
		soul.rank = next.level();
		Soul.save(player, soul);
		reward(player, next.level());
		restockQuarters(player.level().getServer(), next.level());
		applyRankTeam(player);
		applyPerks(player, soul.rank);
		Cmd.title(player, Txt.of(next.name(), next.color()).bold().str(), Txt.of("You have risen in the court of Lucifer", "gold").str(), 10, 70, 20);
		Cmd.sound(player, "minecraft:entity.wither.spawn", 0.4F, 1.6F);
		Cmd.sound(player, "minecraft:ui.toast.challenge_complete", 1.0F, 0.8F);
		Cmd.as(player, "particle minecraft:flame ~ ~1 ~ 0.6 1.0 0.6 0.05 60");
		Cmd.as(player, "advancement grant @s only heavenhell:rank_" + next.level());
		Scheduler.after(30, () -> Dialogs.rankUp(player, next));
	}

	private static void reward(ServerPlayer player, int rank) {
		switch (rank) {
			case 1 -> {
				give(player, new ItemStack(ModItems.INFERNAL_PICKAXE));
				giveKey(player);
			}
			case 2 -> give(player, new ItemStack(ModItems.HELLFIRE_SWORD));
			case 3 -> give(player, new ItemStack(ModItems.DEMON_WINGS));
			case 4 -> give(player, new ItemStack(ModItems.INFERNAL_CROWN));
			case 5 -> {
				give(player, new ItemStack(Items.NETHERITE_INGOT, 2));
				give(player, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2));
				give(player, new ItemStack(Items.DIAMOND, 12));
				give(player, new ItemStack(Items.GOLD_BLOCK, 8));
			}
			default -> {
			}
		}
	}

	private static void giveKey(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		if (!soul.has(SoulData.FLAG_KEY)) {
			soul.set(SoulData.FLAG_KEY);
			Soul.save(player, soul);
			give(player, new ItemStack(ModItems.PALACE_KEY));
		}
	}

	private static void restockQuarters(MinecraftServer server, int rank) {
		ServerLevel hell = Realms.hell(server);
		BlockPos chest = Layout.QUARTER_CHESTS[rank];
		if (hell == null || chest == null) {
			return;
		}
		Cmd.in(hell, "loot insert " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " loot heavenhell:chests/quarters_rank_" + rank);
	}

	public static void quarters(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		if (!Realms.isHell(player.level())) {
			Cmd.actionbar(player, Txt.of("Your quarters are in Hell.", "gray").str());
			return;
		}
		if (soul.rank < 1) {
			Cmd.actionbar(player, Txt.of("Lost Souls have no quarters. Rise in rank first.", "red").str());
			return;
		}
		Vec3 room = Layout.QUARTERS[Math.min(soul.rank, HellRanks.MAX)];
		Cmd.sound(player, "minecraft:entity.enderman.teleport", 1.0F, 0.8F);
		Realms.teleport(player, player.level(), room, 180.0F, 0.0F);
		HellRanks.Rank r = HellRanks.get(soul.rank);
		Scheduler.after(5, () -> Cmd.title(player, Txt.of("Your Quarters", "gold").bold().str(), Txt.of(r.quarters(), "yellow").str(), 10, 50, 20));
	}

	// ------------------------------------------------------------- teams

	private static void setupTeams(MinecraftServer server) {
		for (int i = 1; i <= HellRanks.MAX; i++) {
			HellRanks.Rank r = HellRanks.get(i);
			String team = "heavenhell_rank" + i;
			Cmd.run(server, "team add " + team + " " + Txt.of(r.name(), r.color()).str());
			Cmd.run(server, "team modify " + team + " prefix " + Txt.of("[" + r.name() + "] ", r.color()).str());
		}
	}

	public static void applyRankTeam(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		if (soul.rank >= 1) {
			Cmd.run(player.level().getServer(), "team join heavenhell_rank" + Math.min(soul.rank, HellRanks.MAX) + " " + player.getStringUUID());
		}
	}

	public static void leaveHell(ServerPlayer player) {
		Cmd.run(player.level().getServer(), "team leave " + player.getStringUUID());
	}

	// ------------------------------------------------------------ escape

	public static void escape(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		soul.karma = Math.max(soul.karma, 0);
		soul.remember("+0  Escaped Hell through the Redemption Gate");
		Soul.save(player, soul);
		leaveHell(player);
		Cmd.sound(player, "minecraft:block.end_portal.spawn", 0.8F, 1.4F);
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
		Scheduler.after(10, () -> {
			Realms.returnToLiving(player);
			Cmd.title(player, Txt.of("REDEMPTION", "gold").bold().str(), Txt.of("Your soul is free. Your karma has been wiped clean.", "white").str(), 10, 80, 30);
			Cmd.as(player, "advancement grant @s only heavenhell:redemption");
		});
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		Give.give(player, stack);
	}
}
