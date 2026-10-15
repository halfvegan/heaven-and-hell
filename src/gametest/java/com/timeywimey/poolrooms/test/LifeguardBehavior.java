package com.timeywimey.poolrooms.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Options;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.timeywimey.poolrooms.entity.LifeguardEntity;
import com.timeywimey.poolrooms.gameplay.LifeguardDirector;
import com.timeywimey.poolrooms.registry.ModEntities;
import com.timeywimey.poolrooms.world.Layout;
import com.timeywimey.poolrooms.world.Pool;

/**
 * Plays out every promise about the Lifeguard with a real player (real key presses) and checks each one: it appears
 * far away and out of sight, it whistles when it comes near, walking is fine (it even keeps its distance), running gets
 * noticed, it walks (never runs) to you and catches you without hurting you, it gives up after 10 seconds out of sight,
 * and it never follows you into the Entrance Pool.
 */
final class LifeguardBehavior {
	private static final Logger LOGGER = LoggerFactory.getLogger("LifeguardBehavior");
	private static final String IN_POOL = "execute in poolrooms:poolrooms run ";

	private final ClientGameTestContext context;
	private final TestSingleplayerContext sp;
	private final TestServerContext server;
	private final List<String> events = Collections.synchronizedList(new ArrayList<>());
	private final List<String> failures = new ArrayList<>();

	LifeguardBehavior(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server) {
		this.context = context;
		this.sp = sp;
		this.server = server;
	}

	void run() {
		LifeguardDirector.listener = (event, player) -> {
			events.add(event);
			LOGGER.info("[lifeguard] {} (nearest Lifeguard {} blocks away)", event, fmt(nearestGuard(player)));
		};
		try {
			cmd("gamemode survival @a");
			cmd("effect give @a minecraft:saturation infinite 0 true");
			appearsAndWhistles();
			cmd("poolrooms lifeguard off");
			walkingIsFine();
			runningIsNoticedAndCaught();
			losesInterest();
			waterIsSafe();
			entranceIsSafe();
		} finally {
			LifeguardDirector.listener = null;
			context.getInput().releaseKey(o -> o.keyUp);
			context.getInput().releaseKey(o -> o.keySprint);
			cmd("poolrooms lifeguard off");
			cmd(IN_POOL + "kill @e[type=poolrooms:lifeguard]");
			cmd("gamemode creative @a");
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("Lifeguard checks failed: " + failures);
		}
		LOGGER.info("[lifeguard] all checks passed");
	}

	// ------------------------------------------------------------------ the checks

	/** Appears 40-64 blocks away where you can't see it; two whistle blasts once it is within 30 blocks. */
	private void appearsAndWhistles() {
		cmd("poolrooms lifeguard on");
		tp(8.5, 8.5, 0);
		context.waitTicks(20);
		server.runOnServer(s -> LifeguardDirector.hurry(player(s)));
		int mark = events.size();
		if (!waitFor(mark, "appeared", 120)) {
			fail("the Lifeguard never appeared");
			return;
		}
		AtomicReference<String> where = new AtomicReference<>();
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			LifeguardEntity guard = closestGuard(p);
			if (guard == null) {
				where.set("gone");
				return;
			}
			double d = guard.distanceTo(p);
			boolean visible = canSee(p, guard.position().add(0, 2.2, 0)) || canSee(p, guard.position().add(0, 1.0, 0));
			where.set(String.format(Locale.ROOT, "%.1f blocks away, %s", d, visible ? "VISIBLE" : "out of sight"));
			if (d < 39.0 || d > 65.0 || visible) {
				fail("the Lifeguard appeared " + where.get());
			}
		});
		LOGGER.info("[lifeguard] appeared {}", where.get());
		context.waitTicks(10);
		// bring it within 30 blocks (onto the Pool Hall deck next door): two whistle blasts
		mark = events.size();
		cmd(IN_POOL + "tp @e[type=poolrooms:lifeguard] 24.5 49 1.5");
		if (!waitFor(mark, "whistle", 40)) {
			fail("no whistle when the Lifeguard came within 30 blocks");
		}
	}

	/** Walking right in front of it is fine: it doesn't come after you. */
	private void walkingIsFine() {
		// in the middle of the Lounge, so it has room to back away
		summonGuard(24.5, 24.5, 90);
		tp(17.5, 24.5, -90);
		context.waitTicks(10);
		int mark = events.size();
		context.getInput().holdKeyFor(o -> o.keyUp, 15);
		double near = distanceToGuard();
		context.waitTicks(80);
		double later = distanceToGuard();
		LOGGER.info("[lifeguard] walked up to it: {} blocks away, 4 seconds later {} blocks away", fmt(near), fmt(later));
		if (seen(mark, "noticed")) {
			fail("the Lifeguard came after someone who was only walking");
		}
		if (later < near + 1.0) {
			fail("the Lifeguard did not keep its distance (" + fmt(near) + " -> " + fmt(later) + ")");
		}
		cmd(IN_POOL + "kill @e[type=poolrooms:lifeguard]");
	}

	/** Running in its sight: a long whistle, then it walks over (slower than you walk) and takes you back. */
	private void runningIsNoticedAndCaught() {
		summonGuard(28.5, 23.5, 90);
		tp(19.5, 23.5, -90);
		context.waitTicks(10);
		AtomicReference<String> before = new AtomicReference<>();
		server.runOnServer(s -> before.set(inventory(player(s))));
		int mark = events.size();
		sprint(10);
		if (!waitFor(mark, "noticed", 20)) {
			fail("running in front of the Lifeguard was not noticed");
			return;
		}
		// it walks over: track its speed until it reaches you
		double fastest = 0;
		Vec3 last = guardPos();
		for (int t = 0; t < 300 && !seen(mark, "caught"); t += 5) {
			context.waitTicks(5);
			Vec3 now = guardPos();
			if (last != null && now != null) {
				fastest = Math.max(fastest, Math.hypot(now.x - last.x, now.z - last.z) * 4.0);
			}
			last = now;
		}
		LOGGER.info("[lifeguard] fastest Lifeguard speed while coming over: {} blocks/s", fmt(fastest));
		if (fastest >= 4.3) {
			fail("the Lifeguard moved at " + fmt(fastest) + " blocks/s (walking is 4.3)");
		}
		if (!seen(mark, "caught")) {
			fail("the Lifeguard never reached the player");
			return;
		}
		context.waitTicks(4);
		context.takeScreenshot("60_caught_fade");
		context.waitTicks(30);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			double d = p.position().distanceTo(Pool.ARRIVAL);
			if (!Pool.isPoolrooms(p.level()) || d > 2.0) {
				fail("after being caught the player is not at the Entrance Pool (" + p.position() + ")");
			}
			if (p.getHealth() < p.getMaxHealth()) {
				fail("being caught did damage (health " + p.getHealth() + ")");
			}
			if (!inventory(p).equals(before.get())) {
				fail("being caught changed the inventory: " + before.get() + " -> " + inventory(p));
			}
		});
		context.takeScreenshot("61_back_at_entrance");
	}

	/** Out of its sight for 10 seconds and it loses interest. */
	private void losesInterest() {
		summonGuard(28.5, 23.5, 90);
		tp(19.5, 23.5, -90);
		context.waitTicks(10);
		int mark = events.size();
		sprint(10);
		if (!waitFor(mark, "noticed", 20)) {
			fail("running was not noticed (lose-interest check)");
			return;
		}
		// hide far away, in the Exit Room
		tp(-40.5, -8.5, 0);
		if (!waitFor(mark, "gave_up", 300)) {
			fail("the Lifeguard did not lose interest after 10 seconds out of sight");
		}
		cmd(IN_POOL + "kill @e[type=poolrooms:lifeguard]");
	}

	/** It can't follow you into the water: swimming counts as out of sight, so it gives up after 10 seconds. */
	private void waterIsSafe() {
		cmd("item replace entity @a armor.head with poolrooms:snorkel_mask");
		// the north deck of the big Pool Hall
		summonGuard(47.5, -29.5, 90);
		tp(40.5, -29.5, -90);
		context.waitTicks(10);
		int mark = events.size();
		sprint(8);
		if (!waitFor(mark, "noticed", 20)) {
			fail("running was not noticed (water check)");
			return;
		}
		// into the middle of the pool, in plain view of the deck
		cmd(IN_POOL + "tp @a 47.5 46 -16.5");
		if (!waitFor(mark, "gave_up", 320)) {
			fail("the Lifeguard did not give up on someone swimming in a pool");
		}
		if (seen(mark, "caught")) {
			fail("the Lifeguard caught someone in the middle of a pool");
		}
		cmd(IN_POOL + "kill @e[type=poolrooms:lifeguard]");
	}

	/** It can never follow you into the Entrance Pool. */
	private void entranceIsSafe() {
		// the Flooded Corridors, just west of the entrance
		summonGuard(-2.5, 7.5, 90);
		tp(-10.5, 7.5, -90);
		context.waitTicks(10);
		int mark = events.size();
		sprint(8);
		if (!waitFor(mark, "noticed", 20)) {
			fail("running was not noticed (entrance check)");
			return;
		}
		tp(8.5, 8.5, 0);
		if (!waitFor(mark, "gave_up_entrance", 40)) {
			fail("the Lifeguard did not give up when the player went into the Entrance Pool");
		}
		context.waitTicks(100);
		server.runOnServer(s -> {
			ServerLevel level = Pool.level(s);
			for (LifeguardEntity guard : level.getEntities(ModEntities.LIFEGUARD, e -> true)) {
				if (Layout.isEntrance(level, guard.blockPosition())) {
					fail("a Lifeguard got into the Entrance Pool at " + guard.blockPosition());
				}
			}
		});
		cmd(IN_POOL + "kill @e[type=poolrooms:lifeguard]");
	}

	// ------------------------------------------------------------------ helpers

	private void sprint(int ticks) {
		context.getInput().holdKey(o -> o.keySprint);
		context.getInput().holdKeyFor(o -> o.keyUp, ticks);
		context.getInput().releaseKey(o -> o.keySprint);
	}

	private void summonGuard(double x, double z, float yaw) {
		cmd(IN_POOL + String.format(Locale.ROOT, "summon poolrooms:lifeguard %.1f 49 %.1f {Rotation:[%.1ff,0f]}", x, z, yaw));
	}

	private void tp(double x, double z, float yaw) {
		cmd(IN_POOL + String.format(Locale.ROOT, "tp @a %.2f 49 %.2f %.1f 0", x, z, yaw));
	}

	private void cmd(String command) {
		server.runCommand(command);
	}

	private boolean seen(int from, String event) {
		synchronized (events) {
			return events.subList(Math.min(from, events.size()), events.size()).contains(event);
		}
	}

	private boolean waitFor(int from, String event, int maxTicks) {
		for (int t = 0; t < maxTicks; t += 2) {
			if (seen(from, event)) {
				return true;
			}
			context.waitTicks(2);
		}
		return seen(from, event);
	}

	private double distanceToGuard() {
		AtomicReference<Double> d = new AtomicReference<>(-1.0);
		server.runOnServer(s -> d.set(nearestGuard(player(s))));
		return d.get();
	}

	private Vec3 guardPos() {
		AtomicReference<Vec3> pos = new AtomicReference<>();
		server.runOnServer(s -> {
			LifeguardEntity guard = closestGuard(player(s));
			pos.set(guard == null ? null : guard.position());
		});
		return pos.get();
	}

	private void fail(String message) {
		LOGGER.error("[lifeguard] FAILED: {}", message);
		synchronized (failures) {
			failures.add(message);
		}
	}

	private static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	private static LifeguardEntity closestGuard(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return null;
		}
		LifeguardEntity best = null;
		for (LifeguardEntity guard : level.getEntities(ModEntities.LIFEGUARD, e -> true)) {
			if (best == null || guard.distanceTo(player) < best.distanceTo(player)) {
				best = guard;
			}
		}
		return best;
	}

	private static double nearestGuard(ServerPlayer player) {
		LifeguardEntity guard = closestGuard(player);
		return guard == null ? -1 : guard.distanceTo(player);
	}

	private static boolean canSee(ServerPlayer player, Vec3 target) {
		return player.level().clip(new ClipContext(player.getEyePosition(), target, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
	}

	private static String inventory(ServerPlayer player) {
		Inventory inv = player.getInventory();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (!inv.getItem(i).isEmpty()) {
				sb.append(inv.getItem(i).getItem()).append('x').append(inv.getItem(i).getCount()).append(' ');
			}
		}
		return sb.toString().trim();
	}

	private static String fmt(double d) {
		return String.format(Locale.ROOT, "%.1f", d);
	}
}
