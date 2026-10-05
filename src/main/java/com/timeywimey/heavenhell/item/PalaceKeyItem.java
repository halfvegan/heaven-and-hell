package com.timeywimey.heavenhell.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.timeywimey.heavenhell.hell.HellLife;

/** Takes a ranked demon straight to their quarters in the Tower of Sin. */
public class PalaceKeyItem extends Item {
	public PalaceKeyItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp) {
			HellLife.quarters(sp);
			sp.getCooldowns().addCooldown(player.getItemInHand(hand), 100);
		}
		return InteractionResult.SUCCESS;
	}
}
