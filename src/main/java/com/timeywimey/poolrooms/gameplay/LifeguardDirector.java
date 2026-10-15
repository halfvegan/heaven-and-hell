package com.timeywimey.poolrooms.gameplay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.timeywimey.poolrooms.entity.LifeguardEntity;
import com.timeywimey.poolrooms.registry.ModEntities;
import com.timeywimey.poolrooms.registry.ModSounds;
import com.timeywimey.poolrooms.util.Cmd;
import com.timeywimey.poolrooms.util.Scheduler;
import com.timeywimey.poolrooms.util.Txt;
import com.timeywimey.poolrooms.world.Layout;
import com.timeywimey.poolrooms.world.Pool;

/**
 * Looks after the Lifeguard. Every player in the Poolrooms has one Lifeguard walking somewhere nearby. It appears 40 to
 * 64 blocks away where you cannot see it (never in the Entrance Pool), and you always hear two short whistle blasts
 * when it comes within about 30 blocks (or into view), before you see it.
 */
public final class LifeguardDirector {
	private LifeguardDirector() {
	}

	/** First Lifeguard after you arrive: 45 seconds, so there is time to read the rules and open the Lost & Found. */
	public static final int FIRST_DELAY = 20 * 45;
	/** After it catches you it takes a minute to come back. */
	public static final int AFTER_CATCH_DELAY = 20 * 60;
	public static final double WHISTLE_DISTANCE = 30.0;

	private static final class Watch {
		UUID guard;
		long nextSpawn;
		boolean whistled;
	}

	private static final Map<UUID, Watch> WATCHES = new HashMap<>();
	private static final Set<UUID> CATCHING = new HashSet<>();
	private static final RandomSource RANDOM = RandomSource.create();

	/** Only for the game tests: hears about everything the Lifeguard does (appeared, whistle, noticed, ...). */
	public static volatile BiConsumer<String, ServerPlayer> listener;

	private static void report(String event, ServerPlayer player) {
		BiConsumer<String, ServerPlayer> l = listener;
		if (l != null) {
			l.accept(event, player);
		}
	}

	/** Only for the game tests: the Lifeguard appears as soon as it can instead of after 45 seconds. */
	public static void hurry(ServerPlayer player) {
		ServerLevel pool = Pool.level(player.level().getServer());
		if (pool != null) {
			watch(player, pool.getGameTime()).nextSpawn = pool.getGameTime();
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(LifeguardDirector::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			WATCHES.clear();
			CATCHING.clear();
		});
	}

	public static boolean enabled(MinecraftServer server) {
		return PoolState.get(server).lifeguard;
	}

	/** Someone the Lifeguard may notice: not in creative or spectator mode. */
	public static boolean canBeChased(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && !player.isCreative();
	}

	/** "No running": sprinting on your feet (swimming fast does not count). */
	public static boolean isRunning(ServerPlayer player) {
		return canBeChased(player) && player.isSprinting() && !player.isSwimming() && !player.isPassenger()
				&& !player.isFallFlying();
	}

	private static Watch watch(ServerPlayer player, long now) {
		return WATCHES.computeIfAbsent(player.getUUID(), id -> {
			Watch w = new Watch();
			w.nextSpawn = now + FIRST_DELAY;
			return w;
		});
	}

	public static void onEnter(ServerPlayer player) {
		ServerLevel pool = Pool.level(player.level().getServer());
		if (pool == null) {
			return;
		}
		Watch w = watch(player, pool.getGameTime());
		w.nextSpawn = Math.max(w.nextSpawn, pool.getGameTime() + FIRST_DELAY);
	}

	public static void onLeave(ServerPlayer player) {
		Watch w = WATCHES.remove(player.getUUID());
		ServerLevel pool = Pool.level(player.level().getServer());
		if (w != null && pool != null) {
			discard(pool, w);
		}
	}

	private static LifeguardEntity guardOf(ServerLevel level, Watch w) {
		if (w.guard == null) {
			return null;
		}
		Entity e = level.getEntity(w.guard);
		return e instanceof LifeguardEntity guard && guard.isAlive() ? guard : null;
	}

	private static void discard(ServerLevel level, Watch w) {
		LifeguardEntity guard = guardOf(level, w);
		if (guard != null) {
			guard.discard();
		}
		w.guard = null;
	}

	/** Removes every Lifeguard (used when the Lifeguard is switched off). */
	public static void dismissAll(MinecraftServer server) {
		ServerLevel level = Pool.level(server);
		if (level == null) {
			return;
		}
		for (LifeguardEntity guard : level.getEntities(ModEntities.LIFEGUARD, e -> true)) {
			guard.discard();
		}
		for (Watch w : WATCHES.values()) {
			w.guard = null;
			w.nextSpawn = level.getGameTime() + FIRST_DELAY;
		}
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 5 != 0) {
			return;
		}
		ServerLevel level = Pool.level(server);
		if (level == null) {
			return;
		}
		long now = level.getGameTime();
		boolean enabled = enabled(server);
		WATCHES.entrySet().removeIf(e -> {
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || p.level() != level) {
				discard(level, e.getValue());
				return true;
			}
			return false;
		});
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			Watch w = watch(player, now);
			LifeguardEntity guard = guardOf(level, w);
			if (!enabled) {
				if (guard != null) {
					guard.discard();
				}
				w.guard = null;
				continue;
			}
			if (guard == null) {
				w.guard = null;
				if (now >= w.nextSpawn && server.getTickCount() % 20 == 0 && !CATCHING.contains(player.getUUID())) {
					guard = spawnNear(level, player);
					if (guard != null) {
						w.guard = guard.getUUID();
						w.whistled = false;
					} else {
						w.nextSpawn = now + 100;
					}
				}
				continue;
			}
			double distance = guard.distanceTo(player);
			if (distance > 112.0) {
				guard.discard();
				w.guard = null;
				w.nextSpawn = now + 20 * 20;
				continue;
			}
			boolean sees = distance <= 64.0 && guard.hasLineOfSight(player);
			if (!w.whistled && (distance <= WHISTLE_DISTANCE || sees)) {
				w.whistled = true;
				whistle(level, guard, player);
			} else if (w.whistled && distance > 44.0 && !sees) {
				w.whistled = false;
			}
		}
	}

	/** Two short blasts, from where the Lifeguard is, so you can tell which way it is. */
	private static void whistle(ServerLevel level, LifeguardEntity guard, ServerPlayer player) {
		level.playSound(null, guard.getX(), guard.getEyeY(), guard.getZ(), ModSounds.WHISTLE_SHORT, SoundSource.HOSTILE, 1.0F, 1.0F);
		Cmd.actionbar(player, Txt.of("Two whistle blasts: ", "aqua").then("the Lifeguard is nearby. Walk, don't run.", "white").str());
		report("whistle", player);
	}

	// ------------------------------------------------------------------ appearing

	private static boolean clear(ServerLevel level, BlockPos p) {
		BlockState state = level.getBlockState(p);
		return state.getCollisionShape(level, p).isEmpty() && level.getFluidState(p).isEmpty();
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockPos floor = feet.below();
		return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP) && level.getFluidState(floor).isEmpty()
				&& clear(level, feet) && clear(level, feet.above()) && clear(level, feet.above(2));
	}

	/** Finds a floor spot 40-64 blocks away that the player cannot see, outside the Entrance Pool. */
	private static LifeguardEntity spawnNear(ServerLevel level, ServerPlayer player) {
		long seed = level.getSeed();
		Vec3 eye = player.getEyePosition();
		for (int attempt = 0; attempt < 48; attempt++) {
			double angle = RANDOM.nextDouble() * Math.PI * 2.0;
			double dist = 40.0 + RANDOM.nextDouble() * 24.0;
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
			BlockPos feet = new BlockPos(x, Pool.WALK, z);
			if (!level.isLoaded(feet) || Layout.roomAtBlock(seed, x, z).type() == Layout.RoomType.ENTRANCE
					|| !standable(level, feet)) {
				continue;
			}
			Vec3 head = new Vec3(x + 0.5, Pool.WALK + 2.2, z + 0.5);
			Vec3 middle = new Vec3(x + 0.5, Pool.WALK + 1.0, z + 0.5);
			if (visible(level, player, eye, head) || visible(level, player, eye, middle)) {
				continue;
			}
			LifeguardEntity guard = ModEntities.LIFEGUARD.create(level, EntitySpawnReason.EVENT);
			if (guard == null) {
				return null;
			}
			guard.snapTo(x + 0.5, Pool.WALK, z + 0.5, RANDOM.nextFloat() * 360.0F, 0.0F);
			level.addFreshEntity(guard);
			report("appeared", player);
			return guard;
		}
		return null;
	}

	private static boolean visible(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
				.getType() == HitResult.Type.MISS;
	}

	// ------------------------------------------------------------------ chasing (called by the Lifeguard)

	public static void noticed(ServerPlayer player, LifeguardEntity guard) {
		player.level().playSound(null, guard.getX(), guard.getEyeY(), guard.getZ(), ModSounds.WHISTLE_LONG, SoundSource.HOSTILE,
				1.0F, 1.0F);
		Cmd.actionbar(player, Txt.of("NO RUNNING! ", "red").bold().then("The Lifeguard saw you. Walk away and stay out of sight.",
				"white").str());
		report("noticed", player);
	}

	public static void gaveUp(ServerPlayer player, boolean entrance) {
		Cmd.actionbar(player, entrance
				? Txt.of("The Lifeguard can't come into the Entrance Pool.", "aqua").str()
				: Txt.of("The Lifeguard lost interest.", "aqua").str());
		report(entrance ? "gave_up_entrance" : "gave_up", player);
	}

	/** It reached you: no damage, the screen fades to white and you are back at the Entrance Pool with all your things. */
	public static void caught(ServerPlayer player, LifeguardEntity guard) {
		UUID id = player.getUUID();
		if (CATCHING.contains(id) || PoolTravel.isMoving(player)) {
			return;
		}
		CATCHING.add(id);
		report("caught", player);
		guard.getNavigation().stop();
		PoolTravel.fade(player);
		PoolTravel.setMoving(player, true);
		Scheduler.after(10, () -> {
			CATCHING.remove(id);
			PoolTravel.setMoving(player, false);
			ServerLevel level = Pool.level(player.level().getServer());
			if (level == null || !player.isAlive() || player.level() != level) {
				return;
			}
			PoolTravel.teleport(player, level, Pool.ARRIVAL, Pool.ARRIVAL_YAW, 0.0F);
			guard.discard();
			Watch w = watch(player, level.getGameTime());
			w.guard = null;
			w.whistled = false;
			w.nextSpawn = level.getGameTime() + AFTER_CATCH_DELAY;
			Advancements.award(player, "caught");
			Scheduler.after(20, () -> Cmd.actionbar(player, Txt.of("The Lifeguard walked you back to the Entrance Pool. ", "aqua")
					.then("You kept all your things.", "white").str()));
		});
	}
}
