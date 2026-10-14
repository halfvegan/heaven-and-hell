package com.timeywimey.poolrooms.entity;

import java.util.EnumSet;
import java.util.UUID;

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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

import com.timeywimey.poolrooms.gameplay.LifeguardDirector;
import com.timeywimey.poolrooms.world.Layout;

/**
 * The Lifeguard. Very tall, blank-faced, in a faded red swimsuit. It walks slowly and never runs. It only comes after
 * someone it sees sprinting ("No running!"), it walks slower than you do, and it loses interest once it has not seen
 * you for 10 seconds. If it reaches you it does no damage: it sends you back to the Entrance Pool. It cannot be hurt,
 * but the Pool Noodle knocks it back.
 */
public class LifeguardEntity extends PathfinderMob {
	/** Blocks per second ~= 43 * (0.25 * modifier)^2: about 1.4 when strolling and 3.0 when chasing (you walk at 4.3). */
	public static final double STROLL_SPEED = 0.72;
	public static final double CHASE_SPEED = 1.05;
	/** It notices running within this many blocks, if it can see you. */
	public static final double NOTICE_RANGE = 24.0;
	/** Out of sight for this long and it gives up (10 seconds). */
	public static final int LOSE_INTEREST_TICKS = 200;

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
				.add(Attributes.SCALE, 1.45);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FollowRunnerGoal(this));
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
			if (tickCount % 5 == 0 && hasLineOfSight(target)) {
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

	private boolean reached(ServerPlayer target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		return dx * dx + dz * dz < 1.7 * 1.7 && Math.abs(target.getY() - getY()) < 2.0 && hasLineOfSight(target);
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
