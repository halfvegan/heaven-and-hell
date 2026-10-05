package com.timeywimey.heavenhell.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import com.timeywimey.heavenhell.world.Realms;

/** Small, quick demons of Hell. They drop Soul Shards. */
public class ImpEntity extends Monster {
	public ImpEntity(EntityType<? extends ImpEntity> type, Level level) {
		super(type, level);
	}

	/** Imps roam the wilds of Hell, but never spawn on their own inside Lucifer's court. */
	public static boolean checkImpSpawnRules(EntityType<ImpEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
			BlockPos pos, RandomSource random) {
		if (reason == EntitySpawnReason.NATURAL && Realms.isHell(level.getLevel())
				&& (long) pos.getX() * pos.getX() + (long) pos.getZ() * pos.getZ() < 47L * 47L && pos.getY() >= 44) {
			return false;
		}
		return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.SCALE, 0.6);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}
}
