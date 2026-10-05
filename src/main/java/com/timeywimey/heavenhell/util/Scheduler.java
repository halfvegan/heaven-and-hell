package com.timeywimey.heavenhell.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.MinecraftServer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.timeywimey.heavenhell.HeavenHell;

/** Runs small jobs a number of server ticks later, on the server thread. */
public final class Scheduler {
	private Scheduler() {
	}

	private record Job(long due, Runnable task) {
	}

	private static final List<Job> JOBS = new ArrayList<>();
	private static final List<Job> PENDING = new ArrayList<>();
	private static long now = 0L;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Scheduler::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			JOBS.clear();
			PENDING.clear();
		});
	}

	public static long now() {
		return now;
	}

	/** Runs {@code task} after {@code ticks} server ticks (20 ticks = 1 second). */
	public static void after(int ticks, Runnable task) {
		PENDING.add(new Job(now + Math.max(1, ticks), task));
	}

	private static void tick(MinecraftServer server) {
		now++;
		if (!PENDING.isEmpty()) {
			JOBS.addAll(PENDING);
			PENDING.clear();
		}
		Iterator<Job> it = JOBS.iterator();
		List<Runnable> run = new ArrayList<>();
		while (it.hasNext()) {
			Job job = it.next();
			if (job.due() <= now) {
				run.add(job.task());
				it.remove();
			}
		}
		for (Runnable r : run) {
			try {
				r.run();
			} catch (Exception e) {
				HeavenHell.LOGGER.error("Scheduled task failed", e);
			}
		}
	}
}
