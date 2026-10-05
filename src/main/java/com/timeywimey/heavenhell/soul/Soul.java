package com.timeywimey.heavenhell.soul;

import net.minecraft.server.level.ServerPlayer;

import com.timeywimey.heavenhell.registry.ModAttachments;

/** Access to a player's {@link SoulData}. */
public final class Soul {
	private Soul() {
	}

	public static SoulData get(ServerPlayer player) {
		SoulData data = player.getAttached(ModAttachments.SOUL);
		if (data == null) {
			data = new SoulData();
			player.setAttached(ModAttachments.SOUL, data);
		}
		return data;
	}

	/** Call after changing a player's data so it is saved. */
	public static void save(ServerPlayer player, SoulData data) {
		player.setAttached(ModAttachments.SOUL, data);
	}
}
