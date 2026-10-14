package com.timeywimey.poolrooms.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;

import com.timeywimey.poolrooms.block.LockerBlock;
import com.timeywimey.poolrooms.block.LockerBlockEntity;
import com.timeywimey.poolrooms.registry.ModBlocks;

/**
 * Draws one room in room-local coordinates (x and z from the room's north-west corner, real y), but only writes the
 * blocks that fall inside the chunk being generated. Big rooms are drawn by each of their chunks this way, with the
 * same random seed, so the pieces line up.
 */
public final class Canvas {
	final WorldGenLevel level;
	final long seed;
	final Layout.Room room;
	final RandomSource random;
	final int width;
	final int depth;
	private final int ox;
	private final int oz;
	private final int minX;
	private final int minZ;
	private final int maxX;
	private final int maxZ;
	private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

	Canvas(WorldGenLevel level, long seed, Layout.Room room, int chunkX, int chunkZ) {
		this.level = level;
		this.seed = seed;
		this.room = room;
		this.random = RandomSource.create(Layout.roomSeed(seed, room));
		this.width = room.width();
		this.depth = room.depth();
		this.ox = room.minX();
		this.oz = room.minZ();
		this.minX = chunkX * 16;
		this.minZ = chunkZ * 16;
		this.maxX = minX + 15;
		this.maxZ = minZ + 15;
	}

	boolean inside(int x, int z) {
		int wx = ox + x;
		int wz = oz + z;
		return wx >= minX && wx <= maxX && wz >= minZ && wz <= maxZ;
	}

	BlockPos pos(int x, int y, int z) {
		return new BlockPos(ox + x, y, oz + z);
	}

	void set(int x, int y, int z, BlockState state) {
		if (inside(x, z)) {
			level.setBlock(cursor.set(ox + x, y, oz + z), state, Block.UPDATE_CLIENTS);
		}
	}

	BlockState get(int x, int y, int z) {
		return inside(x, z) ? level.getBlockState(cursor.set(ox + x, y, oz + z)) : Blocks.AIR.defaultBlockState();
	}

	/** Fills the box between two corners (inclusive). */
	void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		int lx = Math.max(Math.min(x0, x1), minX - ox);
		int hx = Math.min(Math.max(x0, x1), maxX - ox);
		int lz = Math.max(Math.min(z0, z1), minZ - oz);
		int hz = Math.min(Math.max(z0, z1), maxZ - oz);
		int ly = Math.min(y0, y1);
		int hy = Math.max(y0, y1);
		for (int x = lx; x <= hx; x++) {
			for (int z = lz; z <= hz; z++) {
				for (int y = ly; y <= hy; y++) {
					level.setBlock(cursor.set(ox + x, y, oz + z), state, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Lets water placed here start flowing once the chunk is loaded (for the slides). */
	void flow(int x, int y, int z) {
		if (inside(x, z)) {
			level.scheduleTick(pos(x, y, z), Fluids.WATER, 2);
		}
	}

	/** A wall sign (pale wood, waxed so it cannot be edited by accident) with up to four lines. */
	void sign(int x, int y, int z, Direction facing, DyeColor color, boolean glowing, Component... lines) {
		if (!inside(x, z)) {
			return;
		}
		BlockPos p = pos(x, y, z);
		level.setBlock(p, Blocks.PALE_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_CLIENTS);
		List<Component> messages = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			messages.add(i < lines.length && lines[i] != null ? lines[i] : Component.empty());
		}
		SignText front = new SignText(messages, messages, color, glowing);
		if (level instanceof Level live) {
			// already a running world (only in tests): set the text directly
			if (live.getBlockEntity(p) instanceof SignBlockEntity sign) {
				sign.setText(front, SignTextSlot.FRONT);
				sign.setWaxed(true);
			}
			return;
		}
		List<Component> empty = List.of(Component.empty(), Component.empty(), Component.empty(), Component.empty());
		SignText back = new SignText(empty, empty, DyeColor.BLACK, false);
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "minecraft:sign");
		tag.putInt("x", p.getX());
		tag.putInt("y", p.getY());
		tag.putInt("z", p.getZ());
		tag.put("front_text", SignText.CODEC.encodeStart(NbtOps.INSTANCE, front).getOrThrow());
		tag.put("back_text", SignText.CODEC.encodeStart(NbtOps.INSTANCE, back).getOrThrow());
		tag.putBoolean("is_waxed", true);
		level.getChunk(p).setBlockEntityNbt(tag);
	}

	/** A locker facing the given way, filled from a loot table the first time it is opened. */
	void locker(int x, int y, int z, Direction facing, ResourceKey<LootTable> loot, Component name) {
		if (!inside(x, z)) {
			return;
		}
		BlockPos p = pos(x, y, z);
		level.setBlock(p, ModBlocks.LOCKER.defaultBlockState().setValue(LockerBlock.FACING, facing), Block.UPDATE_CLIENTS);
		if (level instanceof Level live) {
			if (loot != null && live.getBlockEntity(p) instanceof LockerBlockEntity locker) {
				locker.setLootTable(loot, Layout.hash(seed, p.getX(), p.getZ(), p.getY()));
			}
			return;
		}
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "poolrooms:locker");
		tag.putInt("x", p.getX());
		tag.putInt("y", p.getY());
		tag.putInt("z", p.getZ());
		if (loot != null) {
			tag.putString("LootTable", loot.identifier().toString());
			tag.putLong("LootTableSeed", Layout.hash(seed, p.getX(), p.getZ(), p.getY()));
		}
		if (name != null) {
			tag.put("CustomName", ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, name).getOrThrow());
		}
		level.getChunk(p).setBlockEntityNbt(tag);
	}
}
