package com.timeywimey.heavenhell.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.timeywimey.heavenhell.registry.ModBlocks;
import com.timeywimey.heavenhell.util.Cmd;

/** Uncork it to spread a 3x3 platform of cloud under your feet. */
public class CloudBottleItem extends Item {
	public CloudBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			BlockPos below = player.blockPosition().below();
			int placed = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos p = below.offset(dx, 0, dz);
					if (server.getBlockState(p).canBeReplaced()) {
						server.setBlock(p, ModBlocks.CLOUD.defaultBlockState(), Block.UPDATE_ALL);
						placed++;
					}
				}
			}
			if (placed > 0) {
				Cmd.particle(server, "minecraft:cloud", player.getX(), player.getY() - 0.5, player.getZ(), 1.0, 0.2, 1.0, 0.02, 30);
				Cmd.soundAt(server, player.getX(), player.getY(), player.getZ(), "minecraft:block.wool.place", 1.0F, 0.8F);
				if (!player.isCreative()) {
					stack.shrink(1);
				}
				player.resetFallDistance();
			}
		}
		return InteractionResult.SUCCESS;
	}
}
