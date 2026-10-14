package com.timeywimey.poolrooms.registry;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.gameplay.PoolPlayer;

public final class ModAttachments {
	private ModAttachments() {
	}

	public static final AttachmentType<PoolPlayer> PLAYER = AttachmentRegistry.create(Poolrooms.id("player"),
			builder -> builder.persistent(PoolPlayer.CODEC).copyOnDeath());

	public static void init() {
	}
}
