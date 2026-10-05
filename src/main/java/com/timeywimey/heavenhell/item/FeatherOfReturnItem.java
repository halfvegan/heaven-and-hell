package com.timeywimey.heavenhell.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.timeywimey.heavenhell.heaven.HeavenLife;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Txt;
import com.timeywimey.heavenhell.world.Realms;

/** A white feather that carries a soul from Heaven back to the living world. */
public class FeatherOfReturnItem extends Item {
	public FeatherOfReturnItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer sp) {
			if (Realms.isHeaven(level)) {
				if (!sp.isCreative()) {
					stack.shrink(1);
				}
				HeavenLife.returnHome(sp);
			} else if (Realms.isHell(level)) {
				Cmd.actionbar(sp, Txt.of("The feather smoulders in your hand. Hell does not let go so easily.", "red").str());
				Cmd.sound(sp, "minecraft:block.fire.extinguish", 1.0F, 1.0F);
			} else {
				Cmd.actionbar(sp, Txt.of("You are already among the living.", "gray").str());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
