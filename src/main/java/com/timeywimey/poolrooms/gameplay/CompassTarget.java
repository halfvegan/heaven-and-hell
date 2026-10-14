package com.timeywimey.poolrooms.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

import com.timeywimey.poolrooms.registry.ModBlocks;
import com.timeywimey.poolrooms.world.Layout;
import com.timeywimey.poolrooms.world.Layout.RoomType;

/** What the Chlorine Compass points at: the next wristband you still need, then the nearest EXIT door. */
public record CompassTarget(BlockPos pos, String label, String color) {
	public static boolean has(Inventory inventory, Block block) {
		return inventory.contains(stack -> stack.is(block.asItem()));
	}

	public static CompassTarget find(ServerPlayer player) {
		ServerLevel level = player.level();
		Inventory inventory = player.getInventory();
		RoomType type;
		Block band;
		String label;
		String color;
		if (!has(inventory, ModBlocks.BLUE_WRISTBAND)) {
			type = RoomType.DEEP_END;
			band = ModBlocks.BLUE_WRISTBAND;
			label = "Blue wristband (The Deep End)";
			color = "blue";
		} else if (!has(inventory, ModBlocks.YELLOW_WRISTBAND)) {
			type = RoomType.SLIDE_TOWER;
			band = ModBlocks.YELLOW_WRISTBAND;
			label = "Yellow wristband (Slide Tower)";
			color = "gold";
		} else if (!has(inventory, ModBlocks.RED_WRISTBAND)) {
			type = RoomType.OFFICE;
			band = ModBlocks.RED_WRISTBAND;
			label = "Red wristband (Lifeguard Office)";
			color = "red";
		} else {
			type = RoomType.EXIT;
			band = null;
			label = "EXIT door";
			color = "green";
		}
		Block expected = band;
		BlockPos pos = Layout.nearest(level.getSeed(), player.blockPosition(), type,
				spot -> expected != null && level.isLoaded(spot) && !level.getBlockState(spot).is(expected));
		return pos == null ? null : new CompassTarget(pos, label, color);
	}
}
