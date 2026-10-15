package com.timeywimey.poolrooms.entity;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import com.timeywimey.poolrooms.gameplay.LifeguardDirector;
import com.timeywimey.poolrooms.world.Layout;

/**
 * The Lifeguard. Very tall, blank-faced, in a faded red swimsuit. It walks slowly and never runs. It only comes after
 * someone it sees sprinting ("No running!"), it walks slower than you do, and it loses interest once it has not seen
 * you for 10 seconds (being in the water counts as unseen: it can't follow you in). If it reaches you it does no
 * damage: it sends you back to the Entrance Pool. It cannot be hurt, but the Pool Noodle knocks it back. When it is not
 * after anyone it keeps its distance, slowly wandering away from people it can see close by.
 */
public class LifeguardEntity extends PathfinderMob {
	/** Blocks per second ~= 43 * (0.25 * modifier)^2: about 1.4 when strolling and 3.0 when chasing (you walk at 4.3). */
	public static final double STROLL_SPEED = 0.72;
	public static final double CHASE_SPEED = 1.05;
	/** It notices running within this many blocks, if it can see you. */
	public static final double NOTICE_RANGE = 24.0;
	/** Out of sight for this long and it gives up (10 seconds). Being in the water counts as out of sight. */
	public static final int LOSE_INTEREST_TICKS = 200;
	/** When it isn't chasing anyone it wanders away from people closer than this. */
	public static final float KEEP_AWAY = 6.0F;
	/** How close it has to get to take you back to the Entrance Pool. */
	public static final double REACH = 3.0;

	private UUID chaseTarget;
	private long lastSeen;
	private int stunTicks;
	private boolean sized;

	public LifeguardEntity(EntityType<? extends LifeguardEntity> type, Level level) {
		super(type, level);
		setPathfindingMalus(PathType.WATER, -1.0F);
		setPathfindingMalus(PathType.WATER_BORDER, 6.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
				.add(Attributes.SCALE, 1.45)
				// a strong swimmer (it's a lifeguard): if it ends up in a pool it paddles out at walking pace
				.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0);
	}

	@Override
	protected void registerGoals() {
		// if it ends up in a pool (say, after a pool noodle), it floats and paddles to the nearest edge
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new ClimbOutGoal(this));
		goalSelector.addGoal(1, new FollowRunnerGoal(this));
		// when it isn't after anyone it keeps its distance: it slowly wanders away from people it can see close by
		goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, KEEP_AWAY, STROLL_SPEED, STROLL_SPEED,
				e -> !isChasing() && !e.isSpectator()));
		goalSelector.addGoal(5, new RandomStrollGoal(this, STROLL_SPEED, 60));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	// ------------------------------------------------------------------ chasing

	public UUID chaseTarget() {
		return chaseTarget;
	}

	public boolean isChasing() {
		return chaseTarget != null;
	}

	/** Knocked back by a pool noodle: stands still for a moment. */
	public void bonk() {
		stunTicks = 30;
		getNavigation().stop();
	}

	private ServerPlayer target(ServerLevel level) {
		if (chaseTarget == null) {
			return null;
		}
		Player player = level.getPlayerByUUID(chaseTarget);
		return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!sized) {
			sized = true;
			refreshDimensions();
		}
		if (stunTicks > 0) {
			stunTicks--;
			getNavigation().stop();
		}
		long now = level.getGameTime();
		if (chaseTarget != null) {
			ServerPlayer target = target(level);
			if (target == null || !target.isAlive() || target.level() != level || !LifeguardDirector.canBeChased(target)) {
				chaseTarget = null;
				return;
			}
			if (Layout.isEntrance(level, target.blockPosition())) {
				chaseTarget = null;
				LifeguardDirector.gaveUp(target, true);
				return;
			}
			// it can't follow you into the water, so swimming counts as out of sight
			if (tickCount % 5 == 0 && hasLineOfSight(target) && !target.isInWater()) {
				lastSeen = now;
			}
			if (now - lastSeen > LOSE_INTEREST_TICKS) {
				chaseTarget = null;
				LifeguardDirector.gaveUp(target, false);
				return;
			}
			if (stunTicks == 0 && reached(target)) {
				chaseTarget = null;
				LifeguardDirector.caught(target, this);
			}
		} else if (tickCount % 5 == 0 && stunTicks == 0) {
			for (ServerPlayer player : level.players()) {
				if (LifeguardDirector.isRunning(player) && distanceToSqr(player) < NOTICE_RANGE * NOTICE_RANGE
						&& !Layout.isEntrance(level, player.blockPosition()) && hasLineOfSight(player)) {
					chaseTarget = player.getUUID();
					lastSeen = now;
					LifeguardDirector.noticed(player, this);
					break;
				}
			}
		}
	}

	/**
	 * Close enough to "reach" you. It's tall with long arms, so this is about 3 blocks: the screen starts fading to
	 * white before it is right up in your face.
	 */
	private boolean reached(ServerPlayer target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		return dx * dx + dz * dz < REACH * REACH && Math.abs(target.getY() - getY()) < 2.5 && hasLineOfSight(target);
	}

	/** Walks (never runs) towards the person it saw running, re-planning its path twice a second. */
	static final class FollowRunnerGoal extends Goal {
		private final LifeguardEntity guard;
		private int repath;

		FollowRunnerGoal(LifeguardEntity guard) {
			this.guard = guard;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return guard.chaseTarget != null && guard.stunTicks == 0;
		}

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void stop() {
			guard.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (!(guard.level() instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer target = guard.target(level);
			if (target == null) {
				return;
			}
			guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (--repath <= 0) {
				repath = 10;
				guard.getNavigation().moveTo(target, CHASE_SPEED);
			}
		}
	}

	/** In the water: swim straight to the nearest dry floor (it never chooses to walk into water). */
	static final class ClimbOutGoal extends Goal {
		private final LifeguardEntity guard;
		private Vec3 target;
		private int retarget;

		ClimbOutGoal(LifeguardEntity guard) {
			this.guard = guard;
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return guard.isInWater();
		}

		@Override
		public void start() {
			retarget = 0;
			target = null;
			guard.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (--retarget <= 0) {
				retarget = 20;
				target = nearestDryFloor();
			}
			if (target != null) {
				guard.getMoveControl().setWantedPosition(target.x, target.y, target.z, STROLL_SPEED);
				guard.getLookControl().setLookAt(target.x, target.y + 1.0, target.z);
			}
		}

		private Vec3 nearestDryFloor() {
			Level level = guard.level();
			BlockPos here = guard.blockPosition();
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int dx = -10; dx <= 10; dx++) {
				for (int dz = -10; dz <= 10; dz++) {
					for (int dy = -2; dy <= 3; dy++) {
						BlockPos feet = here.offset(dx, dy, dz);
						BlockPos floor = feet.below();
						if (level.getFluidState(feet).isEmpty() && level.getFluidState(floor).isEmpty()
								&& level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
								&& level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
								&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
								&& level.getBlockState(feet.above(2)).getCollisionShape(level, feet.above(2)).isEmpty()) {
							double d = feet.distSqr(here);
							if (d < bestDistance) {
								bestDistance = d;
								best = feet;
							}
						}
					}
				}
			}
			return best == null ? null : Vec3.atBottomCenterOf(best);
		}
	}

	// ------------------------------------------------------------------ rules of the pool

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean shouldBeSaved() {
		return false; // the director brings a fresh Lifeguard each visit
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false; // the director handles this
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return null;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}
}
