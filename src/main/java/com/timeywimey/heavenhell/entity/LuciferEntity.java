package com.timeywimey.heavenhell.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.timeywimey.heavenhell.hell.HellLife;
import com.timeywimey.heavenhell.registry.ModItems;

/**
 * Lucifer, the Fallen Morningstar. Sits on his throne in the Infernal Court (riding an invisible seat). While seated
 * he cannot be hurt and only talks; once challenged he rises and fights.
 */
public class LuciferEntity extends Monster {
	private boolean inBattle;

	public LuciferEntity(EntityType<? extends LuciferEntity> type, Level level) {
		super(type, level);
		this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.DEMON_WINGS));
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.INFERNAL_CROWN));
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MORNINGSTAR));
		this.xpReward = 500;
		this.setNoAi(true);
		this.setInvulnerable(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 500.0)
				.add(Attributes.ARMOR, 12.0)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.SCALE, 1.35);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public boolean isInBattle() {
		return inBattle;
	}

	public void setInBattle(boolean battle) {
		this.inBattle = battle;
		this.setNoAi(!battle);
		this.setInvulnerable(!battle);
	}

	@Override
	public void tick() {
		super.tick();
		if (!inBattle && !this.level().isClientSide()) {
			// watch whoever is closest while sitting
			Player p = this.level().getNearestPlayer(this, 16.0);
			if (p != null) {
				double dx = p.getX() - this.getX();
				double dz = p.getZ() - this.getZ();
				float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
				this.setYHeadRot(yaw);
				double dy = p.getEyeY() - this.getEyeY();
				this.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
			}
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && !inBattle) {
			HellLife.talkToLucifer(serverPlayer, this);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}
}
