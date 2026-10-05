package com.timeywimey.heavenhell.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.timeywimey.heavenhell.dialog.Dialogs;

/** Shows your karma and your recent deeds. */
public class BookOfDeedsItem extends Item {
	public BookOfDeedsItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp) {
			Dialogs.deeds(sp);
		}
		return InteractionResult.SUCCESS;
	}
}
