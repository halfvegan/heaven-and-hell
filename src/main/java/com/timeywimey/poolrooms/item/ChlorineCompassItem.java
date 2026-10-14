package com.timeywimey.poolrooms.item;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;

import com.timeywimey.poolrooms.gameplay.CompassTarget;
import com.timeywimey.poolrooms.util.Cmd;
import com.timeywimey.poolrooms.util.Txt;
import com.timeywimey.poolrooms.world.Pool;

/**
 * Points to the next wristband you still need (blue, then yellow, then red), and once you have all three, to the
 * nearest EXIT door. Hold it to see what it is pointing at.
 */
public class ChlorineCompassItem extends Item {
	public ChlorineCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof ServerPlayer player) || (level.getGameTime() + entity.getId()) % 20 != 0) {
			return;
		}
		if (!Pool.isPoolrooms(level)) {
			if (stack.has(DataComponents.LODESTONE_TRACKER)) {
				stack.remove(DataComponents.LODESTONE_TRACKER);
			}
			return;
		}
		CompassTarget target = CompassTarget.find(player);
		LodestoneTracker tracker = target == null ? null
				: new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), target.pos())), false);
		if (!Objects.equals(stack.get(DataComponents.LODESTONE_TRACKER), tracker)) {
			if (tracker == null) {
				stack.remove(DataComponents.LODESTONE_TRACKER);
			} else {
				stack.set(DataComponents.LODESTONE_TRACKER, tracker);
			}
		}
		if (target != null && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
				&& (level.getGameTime() + entity.getId()) % 40 == 0) {
			int blocks = (int) Math.sqrt(target.pos().distSqr(player.blockPosition()));
			Cmd.actionbar(player, Txt.of("Chlorine Compass: ", "aqua").then(target.label(), target.color())
					.then(" (" + blocks + " blocks)", "gray").str());
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.poolrooms.chlorine_compass.desc1").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.poolrooms.chlorine_compass.desc2").withStyle(ChatFormatting.GRAY));
	}
}
