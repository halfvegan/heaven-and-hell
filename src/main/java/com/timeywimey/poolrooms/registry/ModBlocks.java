package com.timeywimey.poolrooms.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.block.ExitDoorBlock;
import com.timeywimey.poolrooms.block.ExitLightBlock;
import com.timeywimey.poolrooms.block.ExitSignBlock;
import com.timeywimey.poolrooms.block.FacingBlock;
import com.timeywimey.poolrooms.block.LifeguardBarrierBlock;
import com.timeywimey.poolrooms.block.LockerBlock;
import com.timeywimey.poolrooms.block.LockerBlockEntity;
import com.timeywimey.poolrooms.block.ShallowWaterBlock;
import com.timeywimey.poolrooms.block.StrangeDrainBlock;
import com.timeywimey.poolrooms.block.WristbandBlock;

public final class ModBlocks {
	private ModBlocks() {
	}

	/** Blocks shown in the creative tab, in order. */
	public static final List<Block> TAB = new ArrayList<>();

	// ------------------------------------------------------------------ building blocks
	public static final Block WHITE_POOL_TILES = register("white_pool_tiles", Block::new, tiles(MapColor.SNOW));
	public static final Block WHITE_POOL_TILE_STAIRS = register("white_pool_tile_stairs",
			p -> new StairBlock(WHITE_POOL_TILES.defaultBlockState(), p), tiles(MapColor.SNOW));
	public static final Block WHITE_POOL_TILE_SLAB = register("white_pool_tile_slab", SlabBlock::new, tiles(MapColor.SNOW));
	public static final Block AQUA_POOL_TILES = register("aqua_pool_tiles", Block::new, tiles(MapColor.DIAMOND));
	public static final Block AQUA_POOL_TILE_STAIRS = register("aqua_pool_tile_stairs",
			p -> new StairBlock(AQUA_POOL_TILES.defaultBlockState(), p), tiles(MapColor.DIAMOND));
	public static final Block AQUA_POOL_TILE_SLAB = register("aqua_pool_tile_slab", SlabBlock::new, tiles(MapColor.DIAMOND));
	public static final Block MOSAIC_POOL_TILES = register("mosaic_pool_tiles", Block::new, tiles(MapColor.COLOR_LIGHT_BLUE));
	public static final Block LANE_LINE_TILES = register("lane_line_tiles", Block::new, tiles(MapColor.COLOR_BLUE));
	public static final Block CEILING_LIGHT = register("ceiling_light", Block::new, glow(MapColor.SNOW));
	public static final Block FAKE_SKY_PANEL = register("fake_sky_panel", Block::new, glow(MapColor.COLOR_LIGHT_BLUE));
	public static final Block POOL_LIGHT = register("pool_light", Block::new, glow(MapColor.DIAMOND));
	public static final Block POOL_LADDER = register("pool_ladder", LadderBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.LADDER).noOcclusion().pushReaction(PushReaction.POPPED));

	// ------------------------------------------------------------------ furniture
	public static final Block LOCKER = register("locker", LockerBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN).strength(2.0F, 6.0F).sound(SoundType.IRON));
	public static final Block POOL_CHAIR = register("pool_chair", p -> new FacingBlock(p, new double[] {1, 0, 0, 15, 9, 16}),
			BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6F).sound(SoundType.BAMBOO_WOOD).noOcclusion());
	public static final Block LIFE_RING = register("life_ring", p -> new FacingBlock(p, new double[] {1, 1, 15, 15, 15, 16}),
			BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4F).sound(SoundType.WOOL).noOcclusion()
					.noCollision().pushReaction(PushReaction.POPPED));

	// ------------------------------------------------------------------ the way in and out
	public static final Block STRANGE_DRAIN = register("strange_drain", StrangeDrainBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.IRON).noOcclusion(), Rarity.UNCOMMON);
	public static final Block BLUE_WRISTBAND = register("blue_wristband", WristbandBlock::new, band(MapColor.COLOR_BLUE), Rarity.UNCOMMON);
	public static final Block YELLOW_WRISTBAND = register("yellow_wristband", WristbandBlock::new, band(MapColor.COLOR_YELLOW), Rarity.UNCOMMON);
	public static final Block RED_WRISTBAND = register("red_wristband", WristbandBlock::new, band(MapColor.COLOR_RED), Rarity.UNCOMMON);
	public static final Block EXIT_SIGN = register("exit_sign", ExitSignBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.EMERALD).strength(1.0F).sound(SoundType.GLASS).noOcclusion().noCollision()
			.lightLevel(state -> 12).emissiveRendering(state -> true));

	// ------------------------------------------------------------------ only found in the Poolrooms (no item)
	public static final Block EXIT_DOOR = registerHidden("exit_door", ExitDoorBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.IRON)
			.pushReaction(PushReaction.IMMOVEABLE));
	public static final Block EXIT_LIGHT = registerHidden("exit_light", ExitLightBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.EMERALD).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().noCollision()
			.lightLevel(state -> 15).emissiveRendering(state -> true).sound(SoundType.AMETHYST)
			.pushReaction(PushReaction.IMMOVEABLE));
	public static final Block SHALLOW_WATER = registerHidden("shallow_water", ShallowWaterBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WATER).noCollision().noOcclusion().replaceable().instabreak().noLootTable()
			.sound(ModSounds.SHALLOW_WATER).pushReaction(PushReaction.POPPED));
	public static final Block LIFEGUARD_BARRIER = registerHidden("lifeguard_barrier", LifeguardBarrierBlock::new,
			BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().dynamicShape()
					.pushReaction(PushReaction.IMMOVEABLE));

	public static final BlockEntityType<LockerBlockEntity> LOCKER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Poolrooms.id("locker"), FabricBlockEntityTypeBuilder.create(LockerBlockEntity::new, LOCKER).build());

	public static List<Block> wristbands() {
		return List.of(BLUE_WRISTBAND, YELLOW_WRISTBAND, RED_WRISTBAND);
	}

	private static BlockBehaviour.Properties tiles(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(1.2F, 6.0F).sound(ModSounds.TILES);
	}

	private static BlockBehaviour.Properties glow(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(0.6F).sound(SoundType.GLASS)
				.lightLevel(state -> 15).emissiveRendering(state -> true);
	}

	private static BlockBehaviour.Properties band(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).instabreak().noCollision().noOcclusion().sound(SoundType.WOOL)
				.lightLevel(state -> 6).emissiveRendering(state -> true).pushReaction(PushReaction.POPPED);
	}

	private static <B extends Block> B register(String name, Function<BlockBehaviour.Properties, B> factory,
			BlockBehaviour.Properties properties) {
		return register(name, factory, properties, Rarity.COMMON);
	}

	private static <B extends Block> B register(String name, Function<BlockBehaviour.Properties, B> factory,
			BlockBehaviour.Properties properties, Rarity rarity) {
		B block = registerHidden(name, factory, properties);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Poolrooms.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)
				.useBlockDescriptionPrefix().rarity(rarity)));
		TAB.add(block);
		return block;
	}

	private static <B extends Block> B registerHidden(String name, Function<BlockBehaviour.Properties, B> factory,
			BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Poolrooms.id(name));
		B block = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}

	public static void init() {
	}
}
