package com.timeywimey.heavenhell.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import com.timeywimey.heavenhell.HeavenHell;

public final class ModBlocks {
	private ModBlocks() {
	}

	/** Every block in creative-tab order. */
	public static final List<Block> ALL = new ArrayList<>();

	// ------------------------------------------------------------------ Heaven
	public static final Block CLOUD = register("cloud", Block::new, props()
			.mapColor(MapColor.SNOW).strength(0.3F).sound(SoundType.WOOL));
	public static final Block GOLDEN_CLOUD = register("golden_cloud", Block::new, props()
			.mapColor(MapColor.GOLD).strength(0.3F).sound(SoundType.WOOL).lightLevel(state -> 9));
	public static final Block HEAVEN_GRASS = register("heaven_grass", Block::new, props()
			.mapColor(MapColor.GRASS).strength(0.6F).sound(SoundType.GRASS));
	public static final Block HEAVEN_SOIL = register("heaven_soil", Block::new, props()
			.mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.GRAVEL));
	public static final Block PEARLSTONE = register("pearlstone", Block::new, stone(MapColor.QUARTZ, 1.5F)
			.sound(SoundType.CALCITE));
	public static final Block PEARLSTONE_BRICKS = register("pearlstone_bricks", Block::new, stone(MapColor.QUARTZ, 1.5F)
			.sound(SoundType.CALCITE));
	public static final Block PEARLSTONE_BRICK_STAIRS = register("pearlstone_brick_stairs",
			p -> new StairBlock(PEARLSTONE_BRICKS.defaultBlockState(), p), stone(MapColor.QUARTZ, 1.5F).sound(SoundType.CALCITE));
	public static final Block PEARLSTONE_BRICK_SLAB = register("pearlstone_brick_slab", SlabBlock::new,
			stone(MapColor.QUARTZ, 1.5F).sound(SoundType.CALCITE));
	public static final Block CHISELED_PEARLSTONE = register("chiseled_pearlstone", Block::new, stone(MapColor.QUARTZ, 1.5F)
			.sound(SoundType.CALCITE));
	public static final Block PEARLSTONE_PILLAR = register("pearlstone_pillar", RotatedPillarBlock::new,
			stone(MapColor.QUARTZ, 1.5F).sound(SoundType.CALCITE));
	public static final Block GILDED_PEARLSTONE = register("gilded_pearlstone", Block::new, stone(MapColor.GOLD, 1.5F)
			.sound(SoundType.CALCITE));
	public static final Block GOLDEN_BRICKS = register("golden_bricks", Block::new, stone(MapColor.GOLD, 2.0F)
			.sound(SoundType.METAL));
	public static final Block CELESTIAL_LOG = register("celestial_log", RotatedPillarBlock::new, props()
			.mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
	public static final Block CELESTIAL_PLANKS = register("celestial_planks", Block::new, props()
			.mapColor(MapColor.QUARTZ).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
	public static final Block CELESTIAL_LEAVES = register("celestial_leaves", Block::new, leaves(MapColor.SNOW));
	public static final Block GOLDEN_LEAVES = register("golden_leaves", Block::new, leaves(MapColor.GOLD)
			.lightLevel(state -> 3));
	public static final Block ANGEL_LILY = register("angel_lily", Block::new, props()
			.mapColor(MapColor.SNOW).noCollision().instabreak().noOcclusion().sound(SoundType.GRASS)
			.lightLevel(state -> 5).pushReaction(PushReaction.POPPED));
	public static final Block HALO_LAMP = register("halo_lamp", Block::new, props()
			.mapColor(MapColor.GOLD).strength(0.8F).sound(SoundType.GLASS).lightLevel(state -> 15));
	public static final Block RETURN_LIGHT = register("return_light", Block::new, props()
			.mapColor(MapColor.SNOW).noCollision().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable()
			.lightLevel(state -> 13).sound(SoundType.AMETHYST).pushReaction(PushReaction.IMMOVEABLE));

	// -------------------------------------------------------------------- Hell
	public static final Block BRIMSTONE = register("brimstone", Block::new, stone(MapColor.NETHER, 0.6F)
			.sound(SoundType.NETHERRACK));
	public static final Block BRIMSTONE_BRICKS = register("brimstone_bricks", Block::new, stone(MapColor.NETHER, 2.0F)
			.sound(SoundType.NETHER_BRICKS));
	public static final Block HELLSTONE_BRICKS = register("hellstone_bricks", Block::new, stone(MapColor.COLOR_BLACK, 2.0F)
			.sound(SoundType.NETHER_BRICKS));
	public static final Block HELLSTONE_BRICK_STAIRS = register("hellstone_brick_stairs",
			p -> new StairBlock(HELLSTONE_BRICKS.defaultBlockState(), p), stone(MapColor.COLOR_BLACK, 2.0F).sound(SoundType.NETHER_BRICKS));
	public static final Block HELLSTONE_BRICK_SLAB = register("hellstone_brick_slab", SlabBlock::new,
			stone(MapColor.COLOR_BLACK, 2.0F).sound(SoundType.NETHER_BRICKS));
	public static final Block CHISELED_HELLSTONE = register("chiseled_hellstone", Block::new, stone(MapColor.COLOR_BLACK, 2.0F)
			.sound(SoundType.NETHER_BRICKS).lightLevel(state -> 4));
	public static final Block HELLSTONE_PILLAR = register("hellstone_pillar", RotatedPillarBlock::new,
			stone(MapColor.COLOR_BLACK, 2.0F).sound(SoundType.NETHER_BRICKS));
	public static final Block GILDED_HELLSTONE = register("gilded_hellstone", Block::new, stone(MapColor.GOLD, 2.0F)
			.sound(SoundType.NETHER_BRICKS));
	public static final Block SOUL_SHARD_ORE = register("soul_shard_ore", Block::new, stone(MapColor.NETHER, 3.0F)
			.sound(SoundType.NETHER_GOLD_ORE).lightLevel(state -> 6));
	public static final Block ASH_BLOCK = register("ash_block", Block::new, props()
			.mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.SAND));
	public static final Block EMBER_LAMP = register("ember_lamp", Block::new, props()
			.mapColor(MapColor.FIRE).strength(0.8F).sound(SoundType.GLASS).lightLevel(state -> 15));
	public static final Block REDEMPTION_LIGHT = register("redemption_light", Block::new, props()
			.mapColor(MapColor.GOLD).noCollision().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable()
			.lightLevel(state -> 15).sound(SoundType.AMETHYST).pushReaction(PushReaction.IMMOVEABLE));

	private static BlockBehaviour.Properties props() {
		return BlockBehaviour.Properties.of();
	}

	private static BlockBehaviour.Properties stone(MapColor color, float hardness) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, 6.0F).requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties leaves(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(0.2F).sound(SoundType.GRASS).noOcclusion()
				.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos, box) -> false)
				.ignitedByLava().pushReaction(PushReaction.POPPED);
	}

	private static <B extends Block> B register(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, HeavenHell.id(name));
		B block = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.BLOCK, key, block);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, HeavenHell.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
		ALL.add(block);
		return block;
	}

	public static void init() {
		// class loading registers everything
	}
}
