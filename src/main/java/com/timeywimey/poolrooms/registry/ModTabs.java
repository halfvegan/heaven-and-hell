package com.timeywimey.poolrooms.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.timeywimey.poolrooms.Poolrooms;

public final class ModTabs {
	private ModTabs() {
	}

	public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Poolrooms.id("main"));

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.poolrooms.main"))
				.icon(() -> new ItemStack(ModBlocks.STRANGE_DRAIN))
				.displayItems((context, output) -> {
					for (Item item : ModItems.TAB) {
						output.accept(item);
					}
					for (Block block : ModBlocks.TAB) {
						output.accept(block);
					}
				})
				.build());
	}
}
