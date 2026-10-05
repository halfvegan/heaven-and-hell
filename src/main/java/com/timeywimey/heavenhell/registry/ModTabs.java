package com.timeywimey.heavenhell.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.timeywimey.heavenhell.HeavenHell;

public final class ModTabs {
	private ModTabs() {
	}

	public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(Registries.CREATIVE_MODE_TAB, HeavenHell.id("main"));

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.heavenhell.main"))
				.icon(() -> new ItemStack(ModItems.HALO))
				.displayItems((context, output) -> {
					for (Item item : ModItems.TAB) {
						output.accept(item);
					}
					for (Block block : ModBlocks.ALL) {
						if (block != ModBlocks.RETURN_LIGHT && block != ModBlocks.REDEMPTION_LIGHT) {
							output.accept(block);
						}
					}
				})
				.build());
	}
}
