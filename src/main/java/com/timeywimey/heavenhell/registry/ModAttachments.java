package com.timeywimey.heavenhell.registry;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.soul.SoulData;

public final class ModAttachments {
	private ModAttachments() {
	}

	public static final AttachmentType<SoulData> SOUL = AttachmentRegistry.create(HeavenHell.id("soul"),
			builder -> builder.persistent(SoulData.CODEC).copyOnDeath());

	public static void init() {
	}
}
