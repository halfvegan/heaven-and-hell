package com.timeywimey.heavenhell.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Hands an item to a player, dropping it at their feet when the inventory is full. */
public final class Give {
	private Give() {
	}

	public static void give(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty() || player.getInventory().add(stack) && stack.isEmpty()) {
			return;
		}
		if (!stack.isEmpty()) {
			Level level = player.level();
			ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
			item.setNoPickUpDelay();
			level.addFreshEntity(item);
		}
	}
}
