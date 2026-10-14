package com.timeywimey.poolrooms.gameplay;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

import com.timeywimey.poolrooms.Poolrooms;

/** Grants the advancements that have an "impossible" criterion named "granted". */
public final class Advancements {
	private Advancements() {
	}

	public static void award(ServerPlayer player, String name) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(Poolrooms.id(name));
		if (holder != null) {
			player.getAdvancements().award(holder, "granted");
		}
	}
}
