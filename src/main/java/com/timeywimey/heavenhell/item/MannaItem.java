package com.timeywimey.heavenhell.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bread of Heaven: fills you up and heals you. */
public class MannaItem extends Item {
	public MannaItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide()) {
			entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 0));
		}
		return super.finishUsingItem(stack, level, entity);
	}
}
