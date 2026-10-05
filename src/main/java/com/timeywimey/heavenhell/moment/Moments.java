package com.timeywimey.heavenhell.moment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.dialog.Dialogs;
import com.timeywimey.heavenhell.soul.Karma;
import com.timeywimey.heavenhell.soul.Soul;
import com.timeywimey.heavenhell.soul.SoulData;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Scheduler;
import com.timeywimey.heavenhell.util.Txt;
import com.timeywimey.heavenhell.world.WorldState;

/**
 * Moments of choice: every so often time freezes around a living player and a little scene asks them to choose
 * between good and evil.
 */
public final class Moments {
	private Moments() {
	}

	public static final String SUBJECT_TAG = "heavenhell_moment";
	private static final int FIRST_MOMENT_DELAY = 20 * 60 * 3;
	private static final int DIALOG_DELAY = 30;
	private static final int TIMEOUT = 20 * 60 * 5;
	private static final RandomSource RANDOM = RandomSource.create();
	private static final Map<UUID, Active> ACTIVE = new HashMap<>();
	private static final Map<UUID, MomentType> LAST = new HashMap<>();

	/** A moment in progress. */
	static final class Active {
		final UUID playerId;
		final MomentType type;
		final String token;
		final ServerLevel level;
		final List<Mob> subjects = new ArrayList<>();
		final Map<BlockPos, BlockState> restore = new LinkedHashMap<>();
		Vec3 spot;
		Direction side;
		BlockPos chest;
		int age;
		boolean shown;

		Active(ServerPlayer player, MomentType type, ServerLevel level) {
			this.playerId = player.getUUID();
			this.type = type;
			this.level = level;
			this.token = Integer.toHexString(RANDOM.nextInt(0xFFFFFF) | 0x100000);
		}

		void tick(MinecraftServer server) {
			age++;
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player == null) {
				release(this, false);
				return;
			}
			for (Mob mob : subjects) {
				mob.setDeltaMovement(Vec3.ZERO);
				mob.clearFire();
			}
			if (age % 6 == 0) {
				Vec3 c = center();
				Cmd.particle(level, "minecraft:end_rod", c.x, c.y + 0.8, c.z, 0.6, 0.6, 0.6, 0.01, 3);
				if (type == MomentType.DEVILS_BARGAIN) {
					Cmd.particle(level, "minecraft:soul_fire_flame", c.x, c.y + 0.3, c.z, 0.5, 0.2, 0.5, 0.01, 4);
				}
			}
			if (type == MomentType.TRAPPED_WOLF && age % 40 == 5) {
				Vec3 c = center();
				Cmd.soundAt(level, c.x, c.y, c.z, "minecraft:entity.wolf.whine", 1.0F, 1.0F);
			}
			if (!shown && age >= DIALOG_DELAY) {
				shown = true;
				Dialogs.moment(player, type, token);
			}
			if (age > TIMEOUT) {
				Cmd.actionbar(player, Txt.of("The moment passed...", "gray").italic().str());
				release(this, false);
			}
		}

		Vec3 center() {
			if (!subjects.isEmpty()) {
				return subjects.get(0).position();
			}
			return spot;
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Moments::tick);
		ServerPlayerEvents.LEAVE.register(player -> {
			Active a = ACTIVE.get(player.getUUID());
			if (a != null) {
				release(a, false);
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register(Moments::cleanupStray);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Active a : new ArrayList<>(ACTIVE.values())) {
				release(a, false);
			}
			ACTIVE.clear();
		});
	}

	public static boolean isBusy(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		for (Active a : new ArrayList<>(ACTIVE.values())) {
			a.tick(server);
		}
		if (server.getTickCount() % 20 != 7) {
			return;
		}
		WorldState state = WorldState.get(server);
		if (!state.momentsEnabled) {
			return;
		}
		long now = server.overworld().getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (isBusy(player)) {
				continue;
			}
			SoulData soul = Soul.get(player);
			if (soul.nextMoment <= 0L) {
				soul.nextMoment = now + (soul.has(SoulData.FLAG_FIRST_MOMENT_DONE) ? interval(state) : FIRST_MOMENT_DELAY);
				Soul.save(player, soul);
				continue;
			}
			if (now < soul.nextMoment) {
				continue;
			}
			if (!canHaveMoment(player)) {
				soul.nextMoment = now + 20 * 15;
			} else if (start(player, pick(player))) {
				soul.nextMoment = now + interval(state);
			} else {
				soul.nextMoment = now + 20 * 20;
			}
			Soul.save(player, soul);
		}
	}

	private static long interval(WorldState state) {
		int min = Math.max(1, state.momentMinMinutes);
		int max = Math.max(min, state.momentMaxMinutes);
		return 20L * 60L * (min + RANDOM.nextInt(max - min + 1));
	}

	private static MomentType pick(ServerPlayer player) {
		MomentType[] all = MomentType.values();
		MomentType last = LAST.get(player.getUUID());
		SoulData soul = Soul.get(player);
		if (!soul.has(SoulData.FLAG_FIRST_MOMENT_DONE)) {
			return MomentType.LAVA_ANIMAL; // the classic first
		}
		MomentType t;
		do {
			t = all[RANDOM.nextInt(all.length)];
		} while (t == last && all.length > 1);
		return t;
	}

	public static boolean canHaveMoment(ServerPlayer player) {
		if (player.level().dimension() != Level.OVERWORLD || player.isSpectator() || player.isSleeping()
				|| player.isPassenger() || !player.onGround() || player.isInWater() || player.isInLava()
				|| player.getHealth() < 6.0F || player.isFallFlying()) {
			return false;
		}
		return player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10.0), e -> true).isEmpty();
	}

	// ----------------------------------------------------------------- start

	/** Tries to stage a moment in front of the player. Returns false if there is no room. */
	public static boolean start(ServerPlayer player, MomentType type) {
		if (isBusy(player)) {
			return false;
		}
		ServerLevel level = player.level();
		Active a = new Active(player, type, level);
		boolean ok;
		try {
			ok = switch (type) {
				case LAVA_ANIMAL -> stageLava(player, a);
				case ZOMBIE_AMBUSH -> stageZombie(player, a);
				case TRAPPED_WOLF -> stageWolf(player, a);
				case HUNGRY_TRAVELER -> stageTraveler(player, a);
				case LOST_SATCHEL -> stageSatchel(player, a);
				case DEVILS_BARGAIN -> stageStranger(player, a);
			};
		} catch (Exception e) {
			HeavenHell.LOGGER.error("Could not stage moment {}", type.id, e);
			ok = false;
		}
		if (!ok) {
			release(a, false);
			return false;
		}
		ACTIVE.put(player.getUUID(), a);
		LAST.put(player.getUUID(), type);
		SoulData soul = Soul.get(player);
		soul.moments++;
		soul.set(SoulData.FLAG_FIRST_MOMENT_DONE);
		Soul.save(player, soul);
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DIALOG_DELAY + 10, 6, false, false));
		Cmd.sound(player, "minecraft:block.bell.resonate", 1.0F, 1.2F);
		Cmd.sound(player, "minecraft:block.beacon.deactivate", 0.8F, 0.6F);
		Cmd.title(player, Txt.of("Time freezes...", "aqua").italic().str(), Txt.of("A choice must be made", "gray").str(), 5, 30, 10);
		Cmd.as(player, "advancement grant @s only heavenhell:moment");
		return true;
	}

	/** Finds open, solid ground a few blocks in front of the player. */
	private static BlockPos groundAhead(ServerPlayer player, double distance, double yawOffset) {
		ServerLevel level = player.level();
		double yaw = Math.toRadians(player.getYRot() + yawOffset);
		int x = (int) Math.floor(player.getX() - Math.sin(yaw) * distance);
		int z = (int) Math.floor(player.getZ() + Math.cos(yaw) * distance);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (Math.abs(y - player.getY()) > 3.0) {
			return null;
		}
		BlockPos feet = new BlockPos(x, y, z);
		if (!isStandable(level, feet)) {
			return null;
		}
		return feet;
	}

	private static boolean isStandable(ServerLevel level, BlockPos feet) {
		BlockState ground = level.getBlockState(feet.below());
		if (!ground.isFaceSturdy(level, feet.below(), Direction.UP) || !level.getFluidState(feet.below()).isEmpty()) {
			return false;
		}
		return isClear(level, feet) && isClear(level, feet.above());
	}

	private static boolean isClear(ServerLevel level, BlockPos pos) {
		BlockState s = level.getBlockState(pos);
		return (s.isAir() || s.canBeReplaced()) && level.getFluidState(pos).isEmpty();
	}

	private static BlockPos findSpot(ServerPlayer player) {
		double[] offsets = {0, 25, -25, 50, -50, 80, -80};
		for (double dist : new double[] {5.0, 6.5, 4.0}) {
			for (double off : offsets) {
				BlockPos p = groundAhead(player, dist, off);
				if (p != null) {
					return p;
				}
			}
		}
		return null;
	}

	private static void setBlock(Active a, BlockPos pos, BlockState state) {
		a.restore.putIfAbsent(pos.immutable(), a.level.getBlockState(pos));
		a.level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static <T extends Mob> T spawn(Active a, EntityType<T> type, Vec3 pos, float yaw) {
		T mob = type.create(a.level, EntitySpawnReason.EVENT);
		if (mob == null) {
			return null;
		}
		mob.snapTo(pos);
		mob.setYRot(yaw);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.setPersistenceRequired();
		a.level.addFreshEntity(mob);
		freeze(mob);
		a.subjects.add(mob);
		return mob;
	}

	public static void freeze(Mob mob) {
		mob.setNoAi(true);
		mob.setNoGravity(true);
		mob.setPermanentlyInvulnerable(true);
		mob.setGlowingTag(true);
		mob.setDeltaMovement(Vec3.ZERO);
		mob.addTag(SUBJECT_TAG);
	}

	public static void unfreeze(Mob mob) {
		mob.setNoAi(false);
		mob.setNoGravity(false);
		mob.setPermanentlyInvulnerable(false);
		mob.setGlowingTag(false);
		mob.removeTag(SUBJECT_TAG);
	}

	private static float yawTowards(Vec3 from, Vec3 to) {
		return (float) (Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0);
	}

	private static Direction sideOf(ServerPlayer player) {
		return Direction.fromYRot(player.getYRot()).getClockWise();
	}

	private static boolean stageLava(ServerPlayer player, Active a) {
		ServerLevel level = a.level;
		Direction forward = Direction.fromYRot(player.getYRot());
		Direction side = forward.getClockWise();
		for (double off : new double[] {0, 30, -30, 60, -60}) {
			for (double dist : new double[] {5.0, 6.0, 4.0}) {
				BlockPos feet = groundAhead(player, dist, off);
				if (feet == null) {
					continue;
				}
				for (Direction dir : new Direction[] {side, side.getOpposite()}) {
					List<BlockPos> pool = new ArrayList<>();
					boolean ok = true;
					for (int k = 1; k <= 2 && ok; k++) {
						for (int j = -1; j <= 1 && ok; j++) {
							BlockPos cell = feet.below().relative(dir, k).relative(forward, j);
							pool.add(cell);
							ok = level.getBlockState(cell).isFaceSturdy(level, cell, Direction.UP)
									&& level.getFluidState(cell).isEmpty()
									&& level.getBlockState(cell).getBlock() != Blocks.CHEST
									&& isClear(level, cell.above()) && isClear(level, cell.above(2))
									&& !level.getBlockState(cell.below()).isAir();
						}
					}
					if (!ok) {
						continue;
					}
					// every neighbour of the pool must hold the lava in
					for (BlockPos cell : pool) {
						for (Direction d : Direction.Plane.HORIZONTAL) {
							BlockPos n = cell.relative(d);
							if (!pool.contains(n) && (level.getBlockState(n).isAir() || !level.getFluidState(n).isEmpty())) {
								ok = false;
							}
						}
					}
					if (!ok) {
						continue;
					}
					for (BlockPos cell : pool) {
						if (!level.getBlockState(cell.above()).isAir()) {
							setBlock(a, cell.above(), Blocks.AIR.defaultBlockState());
						}
						setBlock(a, cell, Blocks.LAVA.defaultBlockState());
					}
					a.side = dir;
					a.spot = Vec3.atBottomCenterOf(feet);
					Vec3 edge = a.spot.add(dir.getStepX() * 0.62, 0.0, dir.getStepZ() * 0.62);
					Mob cow = spawn(a, EntityTypes.COW, edge, dir.toYRot());
					if (cow == null) {
						return false;
					}
					cow.setXRot(20.0F);
					return true;
				}
			}
		}
		return false;
	}

	private static boolean stageZombie(ServerPlayer player, Active a) {
		BlockPos feet = findSpot(player);
		if (feet == null) {
			return false;
		}
		Direction side = sideOf(player);
		BlockPos zombieFeet = feet.relative(side, 2);
		if (!isStandable(a.level, zombieFeet)) {
			side = side.getOpposite();
			zombieFeet = feet.relative(side, 2);
			if (!isStandable(a.level, zombieFeet)) {
				return false;
			}
		}
		a.spot = Vec3.atBottomCenterOf(feet);
		Vec3 zPos = Vec3.atBottomCenterOf(zombieFeet).add(side.getOpposite().getStepX() * 0.5, 0, side.getOpposite().getStepZ() * 0.5);
		Mob villager = spawn(a, EntityTypes.VILLAGER, a.spot, yawTowards(a.spot, zPos));
		Mob zombie = spawn(a, EntityTypes.ZOMBIE, zPos, yawTowards(zPos, a.spot));
		if (villager == null || zombie == null) {
			return false;
		}
		zombie.setAggressive(true);
		return true;
	}

	private static boolean stageWolf(ServerPlayer player, Active a) {
		BlockPos feet = findSpot(player);
		if (feet == null) {
			return false;
		}
		Direction side = sideOf(player);
		a.spot = Vec3.atBottomCenterOf(feet);
		setBlock(a, feet, Blocks.COBWEB.defaultBlockState());
		for (Direction d : new Direction[] {side, side.getOpposite()}) {
			BlockPos bush = feet.relative(d);
			if (isClear(a.level, bush) && a.level.getBlockState(bush.below()).isFaceSturdy(a.level, bush.below(), Direction.UP)) {
				setBlock(a, bush, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
			}
		}
		Mob wolf = spawn(a, EntityTypes.WOLF, a.spot, yawTowards(a.spot, player.position()));
		return wolf != null;
	}

	private static boolean stageTraveler(ServerPlayer player, Active a) {
		BlockPos feet = findSpot(player);
		if (feet == null) {
			return false;
		}
		a.spot = Vec3.atBottomCenterOf(feet);
		Mob traveler = spawn(a, EntityTypes.WANDERING_TRADER, a.spot, yawTowards(a.spot, player.position()));
		if (traveler == null) {
			return false;
		}
		traveler.setCustomName(Component.literal("Weary Traveler"));
		traveler.setCustomNameVisible(true);
		return true;
	}

	private static boolean stageSatchel(ServerPlayer player, Active a) {
		BlockPos feet = findSpot(player);
		if (feet == null) {
			return false;
		}
		a.spot = Vec3.atBottomCenterOf(feet);
		a.chest = feet;
		Direction facing = Direction.fromYRot(player.getYRot()).getOpposite();
		setBlock(a, feet, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		return true;
	}

	private static boolean stageStranger(ServerPlayer player, Active a) {
		BlockPos feet = findSpot(player);
		if (feet == null) {
			return false;
		}
		a.spot = Vec3.atBottomCenterOf(feet);
		Mob stranger = spawn(a, EntityTypes.EVOKER, a.spot, yawTowards(a.spot, player.position()));
		if (stranger == null) {
			return false;
		}
		stranger.setCustomName(Component.literal("Hooded Stranger"));
		stranger.setCustomNameVisible(true);
		Cmd.soundAt(a.level, a.spot.x, a.spot.y, a.spot.z, "minecraft:ambient.soul_sand_valley.mood", 1.0F, 0.7F);
		return true;
	}

	// ---------------------------------------------------------------- choose

	/** Called by {@code /heavenhell choose <token> good|evil} from the dialog buttons. */
	public static boolean choose(ServerPlayer player, String token, boolean good) {
		Active a = ACTIVE.get(player.getUUID());
		if (a == null || !a.token.equals(token)) {
			return false;
		}
		ACTIVE.remove(player.getUUID());
		MomentType t = a.type;
		if (good) {
			Karma.add(player, t.goodKarma, t.goodDeed);
		} else {
			Karma.add(player, t.evilKarma, t.evilDeed);
		}
		try {
			switch (t) {
				case LAVA_ANIMAL -> resolveLava(player, a, good);
				case ZOMBIE_AMBUSH -> resolveZombie(player, a, good);
				case TRAPPED_WOLF -> resolveWolf(player, a, good);
				case HUNGRY_TRAVELER -> resolveTraveler(player, a, good);
				case LOST_SATCHEL -> resolveSatchel(player, a, good);
				case DEVILS_BARGAIN -> resolveStranger(player, a, good);
			}
		} catch (Exception e) {
			HeavenHell.LOGGER.error("Moment {} failed to resolve", t.id, e);
			release(a, false);
		}
		return true;
	}

	/** Undoes a moment without a choice (player left, timed out, server stopping). */
	private static void release(Active a, boolean keepSubjects) {
		ACTIVE.remove(a.playerId);
		restoreBlocks(a);
		for (Mob mob : a.subjects) {
			if (keepSubjects) {
				unfreeze(mob);
			} else {
				mob.discard();
			}
		}
	}

	private static void restoreBlocks(Active a) {
		for (Map.Entry<BlockPos, BlockState> e : a.restore.entrySet()) {
			a.level.setBlock(e.getKey(), e.getValue(), Block.UPDATE_ALL);
		}
		a.restore.clear();
	}

	private static void say(ServerPlayer player, String text, String color) {
		player.sendSystemMessage(Component.literal(text).withStyle(style -> style.withItalic(true)));
		Cmd.actionbar(player, Txt.of(text, color).italic().str());
	}

	private static void give(ServerLevel level, Vec3 at, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
		level.addFreshEntity(item);
	}

	private static void resolveLava(ServerPlayer player, Active a, boolean good) {
		Mob cow = a.subjects.isEmpty() ? null : a.subjects.get(0);
		if (cow == null) {
			restoreBlocks(a);
			return;
		}
		Direction dir = a.side;
		if (good) {
			cow.snapTo(a.spot.add(-dir.getStepX() * 1.4, 0.0, -dir.getStepZ() * 1.4));
			unfreeze(cow);
			Cmd.particle(a.level, "minecraft:heart", cow.getX(), cow.getY() + 1.2, cow.getZ(), 0.4, 0.3, 0.4, 0.1, 6);
			Cmd.soundAt(a.level, cow.getX(), cow.getY(), cow.getZ(), "minecraft:entity.cow.ambient", 1.0F, 1.2F);
			Scheduler.after(20, () -> {
				Vec3 p = a.spot.add(dir.getStepX() * 1.5, 0.0, dir.getStepZ() * 1.5);
				Cmd.particle(a.level, "minecraft:large_smoke", p.x, p.y, p.z, 1.0, 0.2, 1.0, 0.02, 30);
				Cmd.soundAt(a.level, p.x, p.y, p.z, "minecraft:block.lava.extinguish", 1.0F, 1.0F);
				restoreBlocks(a);
			});
			say(player, "You pulled the cow back from the edge. It nuzzles you gratefully.", "green");
		} else {
			unfreeze(cow);
			cow.snapTo(a.spot.add(a.side.getStepX() * 1.6, -0.6, a.side.getStepZ() * 1.6));
			say(player, "You looked away. The lava hisses.", "red");
			Scheduler.after(140, () -> {
				Vec3 p = a.spot.add(dir.getStepX() * 1.5, 0.0, dir.getStepZ() * 1.5);
				Cmd.particle(a.level, "minecraft:large_smoke", p.x, p.y, p.z, 1.0, 0.2, 1.0, 0.02, 30);
				restoreBlocks(a);
			});
		}
	}

	private static void resolveZombie(ServerPlayer player, Active a, boolean good) {
		if (a.subjects.size() < 2) {
			release(a, false);
			return;
		}
		Mob villager = a.subjects.get(0);
		Mob zombie = a.subjects.get(1);
		unfreeze(villager);
		if (good) {
			Cmd.particle(a.level, "minecraft:end_rod", zombie.getX(), zombie.getY() + 3.0, zombie.getZ(), 0.1, 3.0, 0.1, 0.05, 80);
			Cmd.particle(a.level, "minecraft:flash", zombie.getX(), zombie.getY() + 1.0, zombie.getZ(), 0.0, 0.0, 0.0, 0.0, 1);
			Cmd.soundAt(a.level, zombie.getX(), zombie.getY(), zombie.getZ(), "minecraft:item.trident.thunder", 0.8F, 1.4F);
			Cmd.soundAt(a.level, zombie.getX(), zombie.getY(), zombie.getZ(), "minecraft:block.bell.use", 1.0F, 1.0F);
			zombie.discard();
			Cmd.particle(a.level, "minecraft:happy_villager", villager.getX(), villager.getY() + 1.5, villager.getZ(), 0.4, 0.4, 0.4, 0.1, 15);
			give(a.level, villager.position(), new ItemStack(Items.EMERALD, 2 + RANDOM.nextInt(3)));
			say(player, "Holy light strikes the zombie down. The villager presses emeralds into your hand.", "green");
		} else {
			unfreeze(zombie);
			zombie.setTarget(villager);
			villager.hurtServer(a.level, a.level.damageSources().mobAttack(zombie), 1000.0F);
			say(player, "You turned your back. Behind you, a scream is cut short...", "red");
		}
	}

	private static void resolveWolf(ServerPlayer player, Active a, boolean good) {
		Mob wolf = a.subjects.isEmpty() ? null : a.subjects.get(0);
		if (good) {
			restoreBlocks(a);
			if (wolf != null) {
				unfreeze(wolf);
				if (wolf instanceof net.minecraft.world.entity.TamableAnimal tame) {
					tame.tame(player);
				}
				Cmd.particle(a.level, "minecraft:heart", wolf.getX(), wolf.getY() + 1.0, wolf.getZ(), 0.4, 0.3, 0.4, 0.1, 8);
				Cmd.soundAt(a.level, wolf.getX(), wolf.getY(), wolf.getZ(), "minecraft:entity.wolf.ambient", 1.0F, 1.2F);
			}
			say(player, "You cut the wolf free. It licks your hand - it's yours now.", "green");
		} else {
			if (wolf != null) {
				unfreeze(wolf);
				wolf.setNoAi(true);
			}
			say(player, "You walk away. Its whimpers fade behind you.", "red");
			Scheduler.after(20 * 20, () -> {
				restoreBlocks(a);
				if (wolf != null && wolf.isAlive()) {
					Cmd.particle(a.level, "minecraft:poof", wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 0.3, 0.3, 0.3, 0.02, 10);
					wolf.discard();
				}
			});
		}
	}

	private static void resolveTraveler(ServerPlayer player, Active a, boolean good) {
		Mob traveler = a.subjects.isEmpty() ? null : a.subjects.get(0);
		if (traveler == null) {
			return;
		}
		if (good) {
			boolean fed = takeFood(player);
			Cmd.soundAt(a.level, traveler.getX(), traveler.getY(), traveler.getZ(), "minecraft:entity.generic.eat", 1.0F, 1.0F);
			Cmd.particle(a.level, "minecraft:happy_villager", traveler.getX(), traveler.getY() + 1.5, traveler.getZ(), 0.4, 0.4, 0.4, 0.1, 15);
			player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 60 * 5, 0));
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
			if (fed) {
				say(player, "He eats gratefully and blesses you. You feel lucky.", "green");
			} else {
				say(player, "You had no food, so you sat with him a while and shared your water. He blesses you anyway.", "green");
			}
			unfreeze(traveler);
			Scheduler.after(20 * 25, traveler::discard);
		} else {
			give(a.level, player.position(), new ItemStack(Items.EMERALD, 4 + RANDOM.nextInt(4)));
			Cmd.soundAt(a.level, traveler.getX(), traveler.getY(), traveler.getZ(), "minecraft:entity.wandering_trader.hurt", 1.0F, 0.8F);
			say(player, "You took his emeralds. He limps away into the trees, weeping.", "red");
			unfreeze(traveler);
			Scheduler.after(50, () -> {
				Cmd.particle(a.level, "minecraft:poof", traveler.getX(), traveler.getY() + 0.5, traveler.getZ(), 0.3, 0.5, 0.3, 0.02, 15);
				traveler.discard();
			});
		}
	}

	private static boolean takeFood(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	private static void resolveSatchel(ServerPlayer player, Active a, boolean good) {
		Vec3 at = a.spot;
		Cmd.particle(a.level, good ? "minecraft:wax_on" : "minecraft:smoke", at.x, at.y + 0.5, at.z, 0.4, 0.4, 0.4, 0.05, 20);
		restoreBlocks(a);
		if (good) {
			player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 10, 0));
			Cmd.soundAt(a.level, at.x, at.y, at.z, "minecraft:block.amethyst_block.chime", 1.0F, 1.0F);
			say(player, "The satchel vanishes in a shimmer. Somewhere, a farmer weeps with relief. (Villagers will love you for a while.)", "green");
		} else {
			give(a.level, at, new ItemStack(Items.EMERALD, 2 + RANDOM.nextInt(3)));
			give(a.level, at, new ItemStack(Items.GOLD_INGOT, 1 + RANDOM.nextInt(2)));
			give(a.level, at, new ItemStack(Items.BREAD, 3));
			if (RANDOM.nextFloat() < 0.2F) {
				give(a.level, at, new ItemStack(Items.DIAMOND));
			}
			Cmd.soundAt(a.level, at.x, at.y, at.z, "minecraft:block.chest.open", 1.0F, 1.0F);
			say(player, "You pocket a farmer's life savings.", "red");
		}
	}

	private static void resolveStranger(ServerPlayer player, Active a, boolean good) {
		Mob stranger = a.subjects.isEmpty() ? null : a.subjects.get(0);
		Vec3 at = stranger != null ? stranger.position() : a.spot;
		if (good) {
			Cmd.soundAt(a.level, at.x, at.y, at.z, "minecraft:entity.evoker.prepare_wololo", 1.0F, 0.6F);
			say(player, "\"You'll change your mind... below.\" The stranger dissolves into smoke.", "green");
		} else {
			give(a.level, player.position(), new ItemStack(Items.DIAMOND, 3));
			Cmd.soundAt(a.level, at.x, at.y, at.z, "minecraft:entity.witch.celebrate", 1.0F, 0.5F);
			say(player, "The diamonds are ice cold. A heavy weight settles in your chest.", "red");
		}
		Cmd.particle(a.level, "minecraft:large_smoke", at.x, at.y + 1.0, at.z, 0.4, 0.8, 0.4, 0.02, 40);
		Cmd.particle(a.level, "minecraft:soul", at.x, at.y + 1.0, at.z, 0.4, 0.8, 0.4, 0.02, 15);
		if (stranger != null) {
			stranger.discard();
		}
	}

	/** Cleans up mobs left frozen by a crash or restart. */
	public static void cleanupStray(Entity entity, ServerLevel level) {
		if (entity.entityTags().contains(SUBJECT_TAG) && entity instanceof Mob mob) {
			boolean owned = ACTIVE.values().stream().anyMatch(a -> a.subjects.contains(mob));
			if (!owned) {
				mob.discard();
			}
		}
	}
}
