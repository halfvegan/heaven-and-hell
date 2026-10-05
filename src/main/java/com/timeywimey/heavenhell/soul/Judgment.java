package com.timeywimey.heavenhell.soul;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import com.timeywimey.heavenhell.dialog.Dialogs;
import com.timeywimey.heavenhell.hell.HellLife;
import com.timeywimey.heavenhell.registry.ModItems;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Scheduler;
import com.timeywimey.heavenhell.util.Txt;
import com.timeywimey.heavenhell.world.Layout;
import com.timeywimey.heavenhell.world.Realms;
import com.timeywimey.heavenhell.world.WorldState;

/** When you die your soul is weighed: good souls wake in Heaven, wicked ones in Hell. */
public final class Judgment {
	private Judgment() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				onDeath(player);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				onRespawn(newPlayer);
			}
		});
	}

	/** Which realm a soul belongs in right now. */
	public static String verdict(ServerPlayer player, SoulData soul) {
		if (Realms.isHeaven(player.level())) {
			return SoulData.HEAVEN; // nobody falls out of Heaven
		}
		if (Realms.isHell(player.level()) && !soul.freed) {
			return SoulData.HELL; // the only way out of Hell is through Lucifer
		}
		return soul.karma >= 0 ? SoulData.HEAVEN : SoulData.HELL;
	}

	private static void onDeath(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (!WorldState.get(server).judgmentEnabled) {
			return;
		}
		SoulData soul = Soul.get(player);
		soul.pending = verdict(player, soul);
		Soul.save(player, soul);
	}

	private static void onRespawn(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		if (soul.pending.isEmpty()) {
			return;
		}
		String dest = soul.pending;
		if (SoulData.LIVING.equals(soul.realm)) {
			// the spot vanilla just respawned them at is where they will return to later
			Realms.rememberReturnPoint(player);
		}
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
		Scheduler.after(5, () -> deliver(player, dest, true));
	}

	/** Moves a soul into Heaven or Hell with a little ceremony. */
	public static void deliver(ServerPlayer player, String dest, boolean died) {
		MinecraftServer server = player.level().getServer();
		ServerPlayer p = server.getPlayerList().getPlayer(player.getUUID());
		if (p == null) {
			return; // logged out
		}
		SoulData soul = Soul.get(p);
		soul.pending = "";
		boolean firstTime;
		Realms.ensureBuilt(server, dest);
		if (SoulData.HEAVEN.equals(dest)) {
			ServerLevel heaven = Realms.heaven(server);
			if (heaven == null) {
				return;
			}
			firstTime = !soul.has(SoulData.FLAG_SEEN_HEAVEN);
			soul.set(SoulData.FLAG_SEEN_HEAVEN);
			soul.realm = SoulData.HEAVEN;
			Soul.save(p, soul);
			HellLife.leaveHell(p);
			Realms.teleport(p, heaven, Layout.HEAVEN_ARRIVAL, Layout.HEAVEN_ARRIVAL_YAW, 10.0F);
			p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, false));
			Cmd.title(p, Txt.of("✦ HEAVEN ✦", "gold").bold().str(),
					Txt.of(died ? "Your good deeds have carried you here." : "You have ascended.", "white").italic().str(), 20, 90, 30);
			Cmd.sound(p, "minecraft:block.beacon.activate", 1.0F, 1.3F);
			Cmd.sound(p, "minecraft:block.amethyst_block.resonate", 1.0F, 0.8F);
			Scheduler.after(40, () -> Cmd.sound(p, "minecraft:ui.toast.challenge_complete", 0.6F, 1.2F));
		} else {
			ServerLevel hell = Realms.hell(server);
			if (hell == null) {
				return;
			}
			firstTime = !soul.has(SoulData.FLAG_SEEN_HELL);
			soul.set(SoulData.FLAG_SEEN_HELL);
			soul.realm = SoulData.HELL;
			soul.freed = false;
			Soul.save(p, soul);
			Realms.teleport(p, hell, Layout.HELL_ARRIVAL, Layout.HELL_ARRIVAL_YAW, 0.0F);
			p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false));
			Cmd.title(p, Txt.of("HELL", "dark_red").bold().str(),
					Txt.of(died ? "Your sins have dragged you down." : "Abandon all hope.", "red").italic().str(), 10, 90, 30);
			Cmd.sound(p, "minecraft:entity.wither.spawn", 0.7F, 0.6F);
			Cmd.sound(p, "minecraft:ambient.basalt_deltas.mood", 1.0F, 0.8F);
			HellLife.applyRankTeam(p);
		}
		if (!soul.has(SoulData.FLAG_BOOK)) {
			soul.set(SoulData.FLAG_BOOK);
			Soul.save(p, soul);
			ItemStack book = new ItemStack(ModItems.BOOK_OF_DEEDS);
			if (!p.getInventory().add(book)) {
				p.drop(book, false);
			}
		}
		final boolean first = firstTime;
		Scheduler.after(70, () -> Dialogs.judgment(p, dest, died, first));
		Cmd.as(p, "advancement grant @s only heavenhell:" + (SoulData.HEAVEN.equals(dest) ? "heaven" : "hell"));
	}
}
