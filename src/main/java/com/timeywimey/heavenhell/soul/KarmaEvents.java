package com.timeywimey.heavenhell.soul;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.timeywimey.heavenhell.entity.AngelEntity;

/** Everyday deeds that change karma. */
public final class KarmaEvents {
	private KarmaEvents() {
	}

	public static void init() {
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, killed, source) -> {
			if (killer instanceof ServerPlayer player) {
				onKill(player, killed);
			}
		});
		ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, keepEquipment) -> {
			// a zombie villager being cured: thank the nearest player
			if (converted instanceof Villager && previous.getType() == EntityTypes.ZOMBIE_VILLAGER) {
				Player near = previous.level().getNearestPlayer(previous, 16.0);
				if (near instanceof ServerPlayer player) {
					Karma.add(player, 12, "Cured a zombie villager");
				}
			}
		});
	}

	private static void onKill(ServerPlayer player, LivingEntity killed) {
		if (killed instanceof AngelEntity) {
			Karma.add(player, -20, "Struck down an angel");
		} else if (killed instanceof AbstractVillager villager) {
			Karma.add(player, villager instanceof Villager ? -10 : -6, "Killed an innocent " + (villager instanceof Villager ? "villager" : "trader"));
		} else if (killed instanceof TamableAnimal pet && pet.isTame() && pet.isOwnedBy(player)) {
			Karma.add(player, -15, "Killed your own pet");
		} else if (killed instanceof TamableAnimal pet && pet.isTame()) {
			Karma.add(player, -8, "Killed someone's pet");
		} else if (killed instanceof AbstractGolem) {
			Karma.add(player, -4, "Destroyed a village guardian");
		} else if (killed instanceof Animal animal && animal.isBaby()) {
			Karma.add(player, -2, "Killed a baby animal");
		} else if (killed instanceof Monster && killed instanceof Mob mob && mob.getTarget() instanceof AbstractVillager) {
			Karma.add(player, 3, "Protected a villager");
		}
	}
}
