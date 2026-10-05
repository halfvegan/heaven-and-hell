package com.timeywimey.heavenhell.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.timeywimey.heavenhell.dialog.Dialogs;
import com.timeywimey.heavenhell.registry.ModEntities;
import com.timeywimey.heavenhell.registry.ModItems;

/** Gentle angels who wander Heaven. The Gatekeeper is a special angel who stands at the Pearly Gates. */
public class AngelEntity extends PathfinderMob {
	public AngelEntity(EntityType<? extends AngelEntity> type, Level level) {
		super(type, level);
		this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.ANGEL_WINGS));
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HALO));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	public boolean isGatekeeper() {
		return this.getType() == ModEntities.GATEKEEPER;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (!isGatekeeper()) {
			this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		}
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			this.getLookControl().setLookAt(player);
			if (isGatekeeper()) {
				Dialogs.gatekeeper(serverPlayer);
			} else {
				Dialogs.angel(serverPlayer);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
