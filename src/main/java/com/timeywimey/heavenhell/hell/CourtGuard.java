package com.timeywimey.heavenhell.hell;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import com.timeywimey.heavenhell.entity.LuciferEntity;
import com.timeywimey.heavenhell.util.Scheduler;
import com.timeywimey.heavenhell.world.Realms;

/**
 * Lucifer's court is his house: wild monsters (ghasts, magma cubes, imps...) do not spawn inside its walls. Only the
 * imps Lucifer summons in battle are allowed in. The wilds of Hell outside stay as dangerous as ever.
 */
public final class CourtGuard {
	private CourtGuard() {
	}

	/** Entities with this tag were brought into the court on purpose. */
	public static final String SUMMONED_TAG = "heavenhell_summoned";

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!Realms.isHell(level) || !(entity instanceof Enemy) || entity instanceof LuciferEntity) {
				return;
			}
			if (entity.entityTags().contains(SUMMONED_TAG) || entity instanceof Mob mob && mob.isPersistenceRequired()) {
				return;
			}
			if (insideCourt(entity.position())) {
				Scheduler.after(1, () -> {
					if (!entity.isRemoved()) {
						entity.discard();
					}
				});
			}
		});
	}

	public static boolean insideCourt(Vec3 pos) {
		return Math.abs(pos.x) <= 45.0 && Math.abs(pos.z) <= 47.0 && pos.y >= 44.0 && pos.y <= 100.0;
	}

	public static void markSummoned(Entity entity) {
		entity.addTag(SUMMONED_TAG);
	}
}
