package com.timeywimey.heavenhell.heaven;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.timeywimey.heavenhell.dialog.Dialogs;
import com.timeywimey.heavenhell.entity.AngelEntity;
import com.timeywimey.heavenhell.registry.ModBlocks;
import com.timeywimey.heavenhell.registry.ModItems;
import com.timeywimey.heavenhell.soul.Soul;
import com.timeywimey.heavenhell.soul.SoulData;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Scheduler;
import com.timeywimey.heavenhell.util.Txt;
import com.timeywimey.heavenhell.world.Layout;
import com.timeywimey.heavenhell.world.Realms;
import com.timeywimey.heavenhell.world.WorldState;

/** Life in Heaven: no hunger, gentle healing, flight with angel wings, and the way back home. */
public final class HeavenLife {
	private HeavenLife() {
	}

	private static final RandomSource RANDOM = RandomSource.create();
	/** Players we gave flight to (so we only take away flight we granted). */
	private static final Set<UUID> FLYERS = new HashSet<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(HeavenLife::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && Realms.isHeaven(player.level())) {
				// soft clouds and the peace of Heaven: no fall damage, no fire
				if (source.typeHolder().is(DamageTypes.FALL)) {
					return false;
				}
			}
			if (entity instanceof AngelEntity angel && angel.isGatekeeper()) {
				return false;
			}
			return true;
		});
	}

	private static void tick(MinecraftServer server) {
		int t = server.getTickCount();
		if (t % 10 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			boolean inHeaven = Realms.isHeaven(player.level());
			updateFlight(player, inHeaven);
			if (!inHeaven) {
				continue;
			}
			if (Layout.RETURN_GATE.contains(player.position())) {
				Dialogs.returnConfirm(player);
				// nudge them back out of the arch so the screen is not shown again immediately
				Realms.teleport(player, player.level(), player.position().add(1.6, 0.0, 0.0), player.getYRot() + 180.0F, 0.0F);
			}
			if (t % 40 == 0) {
				if (player.getHealth() < player.getMaxHealth()) {
					player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
				}
				if (player.getFoodData().needsFood()) {
					player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0, true, false, false));
				}
				ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
				if (head.is(ModItems.HALO)) {
					player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
				}
			}
		}
		if (t % 600 == 0) {
			ServerLevel heaven = Realms.heaven(server);
			if (heaven != null && WorldState.get(server).heavenBuilt && !heaven.players().isEmpty()
					&& heaven.isLoaded(BlockPos.containing(Layout.GATEKEEPER))) {
				Realms.spawnHeavenFolk(heaven);
			}
		}
	}

	/** Angel Wings let you fly freely in Heaven (and Demon Wings let a Prince of Hell fly in Hell). */
	private static void updateFlight(ServerPlayer player, boolean inHeaven) {
		if (player.isCreative() || player.isSpectator()) {
			FLYERS.remove(player.getUUID());
			return;
		}
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		boolean canFly = (inHeaven && chest.is(ModItems.ANGEL_WINGS))
				|| (Realms.isHell(player.level()) && chest.is(ModItems.DEMON_WINGS) && Soul.get(player).rank >= 5);
		boolean granted = FLYERS.contains(player.getUUID());
		if (canFly && !player.getAbilities().mayfly) {
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
			FLYERS.add(player.getUUID());
			Cmd.actionbar(player, Txt.of("Your wings carry you - double-tap jump to fly", "aqua").str());
		} else if (!canFly && granted) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			FLYERS.remove(player.getUUID());
		}
	}

	// --------------------------------------------------------------- actions

	public static void stay(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		if (!Realms.isHeaven(player.level())) {
			return;
		}
		if (!soul.has(SoulData.FLAG_HEAVEN_GIFTS)) {
			soul.set(SoulData.FLAG_HEAVEN_GIFTS);
			Soul.save(player, soul);
			give(player, new ItemStack(ModItems.HALO));
			give(player, new ItemStack(ModItems.MANNA, 6));
			give(player, new ItemStack(ModItems.FEATHER_OF_RETURN));
			Cmd.sound(player, "minecraft:entity.player.levelup", 1.0F, 1.0F);
			Cmd.title(player, Txt.of(" ").str(), Txt.of("The Gatekeeper gives you a Halo, Manna and a Feather of Return", "gold").str(), 10, 60, 20);
		} else {
			Cmd.actionbar(player, Txt.of("\"Rest well, child.\"", "gold").italic().str());
		}
	}

	public static void returnHome(ServerPlayer player) {
		if (!Realms.isHeaven(player.level())) {
			return;
		}
		Cmd.sound(player, "minecraft:block.beacon.power_select", 1.0F, 1.4F);
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
		Scheduler.after(10, () -> {
			Realms.returnToLiving(player);
			Cmd.title(player, Txt.of("You wake up...", "white").italic().str(),
					Txt.of("Heaven will wait for you.", "gold").str(), 10, 60, 30);
			Cmd.as(player, "advancement grant @s only heavenhell:second_chance");
		});
	}

	private static ItemStack randomBlessing() {
		return switch (RANDOM.nextInt(6)) {
			case 0 -> new ItemStack(ModItems.MANNA, 3);
			case 1 -> new ItemStack(ModItems.GOLDEN_FEATHER, 2);
			case 2 -> new ItemStack(ModItems.CLOUD_IN_A_BOTTLE, 2);
			case 3 -> new ItemStack(ModItems.FEATHER_OF_RETURN);
			case 4 -> new ItemStack(ModBlocks.ANGEL_LILY, 4);
			default -> new ItemStack(ModBlocks.HALO_LAMP, 2);
		};
	}

	public static void blessing(ServerPlayer player) {
		if (!Realms.isHeaven(player.level())) {
			return;
		}
		SoulData soul = Soul.get(player);
		long now = player.level().getGameTime();
		if (now < soul.blessingReady) {
			long secs = (soul.blessingReady - now) / 20;
			Cmd.actionbar(player, Txt.of("\"Patience, child. Ask again in " + (secs / 60 + 1) + " minutes.\"", "yellow").italic().str());
			return;
		}
		soul.blessingReady = now + 20 * 60 * 5;
		Soul.save(player, soul);
		ItemStack gift;
		float roll = RANDOM.nextFloat();
		if (roll < 0.05F) {
			gift = new ItemStack(ModItems.ANGEL_WINGS);
		} else if (roll < 0.10F) {
			gift = new ItemStack(ModItems.HARP);
		} else {
			gift = randomBlessing();
		}
		give(player, gift);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
		Cmd.as(player, "particle minecraft:end_rod ~ ~1.5 ~ 0.5 0.8 0.5 0.03 30");
		Cmd.sound(player, "minecraft:block.amethyst_block.chime", 1.0F, 1.0F);
		Cmd.actionbar(player, Txt.of("You have been blessed: ", "gold").then(gift.getHoverName().getString(), "white").str());
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}
}
