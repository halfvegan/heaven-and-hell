package com.timeywimey.poolrooms.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.block.ExitDoorBlock;
import com.timeywimey.poolrooms.block.ExitSignBlock;
import com.timeywimey.poolrooms.block.FacingBlock;
import com.timeywimey.poolrooms.block.WristbandBlock;
import com.timeywimey.poolrooms.registry.ModBlocks;

/**
 * The room designs. Coordinates are room-local: x and z run from 0 at the room's north-west corner (the outer ring of
 * blocks, x = 0 / width-1 and z = 0 / depth-1, is the room's own wall), y is the real height. The floor is at y = 48,
 * so you stand at y = 49.
 */
final class Rooms {
	private Rooms() {
	}

	static final int FLOOR = Pool.FLOOR;
	static final int WALK = Pool.WALK;

	static final BlockState AIR = Blocks.AIR.defaultBlockState();
	static final BlockState WATER = Blocks.WATER.defaultBlockState();
	static final BlockState WHITE = ModBlocks.WHITE_POOL_TILES.defaultBlockState();
	static final BlockState AQUA = ModBlocks.AQUA_POOL_TILES.defaultBlockState();
	static final BlockState MOSAIC = ModBlocks.MOSAIC_POOL_TILES.defaultBlockState();
	static final BlockState LANE = ModBlocks.LANE_LINE_TILES.defaultBlockState();
	static final BlockState CEILING_LIGHT = ModBlocks.CEILING_LIGHT.defaultBlockState();
	static final BlockState SKY = ModBlocks.FAKE_SKY_PANEL.defaultBlockState();
	static final BlockState POOL_LIGHT = ModBlocks.POOL_LIGHT.defaultBlockState();
	static final BlockState SHALLOW = ModBlocks.SHALLOW_WATER.defaultBlockState();
	static final BlockState BARRIER = ModBlocks.LIFEGUARD_BARRIER.defaultBlockState();

	static final ResourceKey<LootTable> LOST_AND_FOUND = loot("chests/lost_and_found");
	static final ResourceKey<LootTable> LOUNGE_LOCKER = loot("chests/lounge_locker");
	static final ResourceKey<LootTable> OFFICE_LOCKER = loot("chests/office_locker");

	private static ResourceKey<LootTable> loot(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Poolrooms.id(path));
	}

	enum Side { NORTH, SOUTH, WEST, EAST }

	/** A doorway cut into this room's wall: which wall, where along it (room-local) and how high. */
	record Opening(Side side, int from, int to, int top) {
	}

	// ------------------------------------------------------------------ block helpers

	static BlockState light(int level, boolean wet) {
		return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level).setValue(LightBlock.WATERLOGGED, wet);
	}

	static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
		return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF,
				upsideDown ? Half.TOP : Half.BOTTOM);
	}

	static BlockState slab(Block block, SlabType type) {
		return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
	}

	static BlockState ladder(Direction facing, boolean wet) {
		return ModBlocks.POOL_LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing).setValue(LadderBlock.WATERLOGGED, wet);
	}

	static BlockState pane(boolean north, boolean east, boolean south, boolean west) {
		return Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState().setValue(CrossCollisionBlock.NORTH, north)
				.setValue(CrossCollisionBlock.EAST, east).setValue(CrossCollisionBlock.SOUTH, south)
				.setValue(CrossCollisionBlock.WEST, west);
	}

	static BlockState facing(Block block, Direction facing) {
		return block.defaultBlockState().setValue(FacingBlock.FACING, facing);
	}

	static BlockState band(Block block, boolean wet) {
		return block.defaultBlockState().setValue(WristbandBlock.WATERLOGGED, wet);
	}

	static Component text(String s) {
		return Component.literal(s);
	}

	static Component text(String s, ChatFormatting... style) {
		return Component.literal(s).withStyle(style);
	}

	// ------------------------------------------------------------------ the shell every room starts from

	static void build(Canvas c) {
		switch (c.room.type()) {
			case ENTRANCE -> entrance(c);
			case POOL_HALL -> {
				if (c.room.big()) {
					bigPoolHall(c);
				} else {
					poolHall(c);
				}
			}
			case CORRIDORS -> corridors(c);
			case PILLAR_FOREST -> pillarForest(c);
			case ATRIUM -> atrium(c);
			case LOUNGE -> lounge(c);
			case DEEP_END -> deepEnd(c);
			case SLIDE_TOWER -> slideTower(c);
			case OFFICE -> office(c);
			case EXIT -> exit(c);
		}
	}

	/** Empties the room, lays the floor and ceiling, tiles the walls and cuts the doorways. */
	static List<Opening> shell(Canvas c, int ceil, BlockState floor, boolean lights) {
		int w = c.width;
		int d = c.depth;
		c.fill(1, WALK, 1, w - 2, ceil - 1, d - 2, AIR);
		c.fill(1, FLOOR, 1, w - 2, FLOOR, d - 2, floor);
		c.fill(1, ceil, 1, w - 2, ceil, d - 2, WHITE);
		if (lights) {
			for (int x = 2; x <= w - 2; x += 4) {
				for (int z = 2; z <= d - 2; z += 4) {
					c.set(x, ceil, z, CEILING_LIGHT);
				}
			}
		}
		walls(c, ceil);
		return doorways(c);
	}

	static void walls(Canvas c, int ceil) {
		for (int y = FLOOR; y <= ceil; y++) {
			ring(c, y, y == WALK || y == ceil - 1 ? AQUA : WHITE);
		}
	}

	/** One horizontal ring of the room's own wall. */
	static void ring(Canvas c, int y, BlockState state) {
		int w = c.width;
		int d = c.depth;
		c.fill(0, y, 0, w - 1, y, 0, state);
		c.fill(0, y, d - 1, w - 1, y, d - 1, state);
		c.fill(0, y, 0, 0, y, d - 1, state);
		c.fill(w - 1, y, 0, w - 1, y, d - 1, state);
	}

	/** A rectangle outline on one layer. */
	static void outline(Canvas c, int x0, int z0, int x1, int z1, int y, BlockState state) {
		c.fill(x0, y, z0, x1, y, z0, state);
		c.fill(x0, y, z1, x1, y, z1, state);
		c.fill(x0, y, z0, x0, y, z1, state);
		c.fill(x1, y, z0, x1, y, z1, state);
	}

	static List<Opening> doorways(Canvas c) {
		List<Opening> openings = new ArrayList<>();
		Layout.Room r = c.room;
		for (int i = 0; i < r.sizeX(); i++) {
			int cx = r.cellX() + i;
			opening(c, openings, Side.NORTH, cx, r.cellZ(), cx, r.cellZ() - 1, i * 16);
			int south = r.cellZ() + r.sizeZ() - 1;
			opening(c, openings, Side.SOUTH, cx, south, cx, south + 1, i * 16);
		}
		for (int j = 0; j < r.sizeZ(); j++) {
			int cz = r.cellZ() + j;
			opening(c, openings, Side.WEST, r.cellX(), cz, r.cellX() - 1, cz, j * 16);
			int east = r.cellX() + r.sizeX() - 1;
			opening(c, openings, Side.EAST, east, cz, east + 1, cz, j * 16);
		}
		return openings;
	}

	private static void opening(Canvas c, List<Opening> openings, Side side, int ax, int az, int bx, int bz, int base) {
		if (!Layout.edgeOpen(c.seed, ax, az, bx, bz)) {
			return;
		}
		Layout.Door door = Layout.door(c.seed, ax, az, bx, bz);
		int from = base + door.from();
		int to = base + door.to();
		for (int a = from - 1; a <= to + 1; a++) {
			for (int y = WALK; y <= door.top() + 1; y++) {
				wall(c, side, a, y, AQUA);
			}
		}
		for (int a = from; a <= to; a++) {
			for (int y = WALK; y <= door.top(); y++) {
				wall(c, side, a, y, AIR);
			}
		}
		if (door == Layout.Door.WIDE) {
			// round off the top corners of the wide arches
			boolean alongX = side == Side.NORTH || side == Side.SOUTH;
			Block stair = ModBlocks.AQUA_POOL_TILE_STAIRS;
			wall(c, side, from, door.top(), stairs(stair, alongX ? Direction.WEST : Direction.NORTH, true));
			wall(c, side, to, door.top(), stairs(stair, alongX ? Direction.EAST : Direction.SOUTH, true));
		}
		openings.add(new Opening(side, from, to, door.top()));
	}

	/** Sets a block in the room's own wall ring on the given side; {@code along} runs along that wall. */
	static void wall(Canvas c, Side side, int along, int y, BlockState state) {
		switch (side) {
			case NORTH -> c.set(along, y, 0, state);
			case SOUTH -> c.set(along, y, c.depth - 1, state);
			case WEST -> c.set(0, y, along, state);
			case EAST -> c.set(c.width - 1, y, along, state);
		}
	}

	// ------------------------------------------------------------------ shared pieces

	/** A rectangular pool, water from x0..x1 / z0..z1 and {@code depth} blocks deep, with tiled sides and lights. */
	static void pool(Canvas c, int x0, int z0, int x1, int z1, int depth, BlockState sides, BlockState bottom) {
		int bottomY = FLOOR - depth;
		c.fill(x0 - 1, bottomY, z0 - 1, x1 + 1, FLOOR - 1, z1 + 1, sides);
		outline(c, x0 - 1, z0 - 1, x1 + 1, z1 + 1, FLOOR, AQUA);
		c.fill(x0, bottomY, z0, x1, bottomY, z1, bottom);
		c.fill(x0, bottomY + 1, z0, x1, FLOOR, z1, WATER);
		int ly = Math.max(bottomY + 1, FLOOR - 2);
		for (int x = x0 + 1; x < x1; x += 4) {
			c.set(x, ly, z0 - 1, POOL_LIGHT);
			c.set(x, ly, z1 + 1, POOL_LIGHT);
		}
		for (int z = z0 + 1; z < z1; z += 4) {
			c.set(x0 - 1, ly, z, POOL_LIGHT);
			c.set(x1 + 1, ly, z, POOL_LIGHT);
		}
	}

	/** A pool ladder from the bottom of the water up to the surface. */
	static void poolLadder(Canvas c, int x, int z, int depth, Direction facing) {
		c.fill(x, FLOOR - depth + 1, z, x, FLOOR, z, ladder(facing, true));
	}

	/** A lifeguard's high chair: metal legs, a platform and a seat looking the given way. */
	static void highChair(Canvas c, int x, int z, Direction looking) {
		for (int dx = -1; dx <= 1; dx += 2) {
			for (int dz = -1; dz <= 1; dz += 2) {
				c.fill(x + dx, WALK, z + dz, x + dx, WALK + 1, z + dz, Blocks.IRON_BARS.defaultBlockState());
			}
		}
		c.fill(x - 1, WALK + 2, z - 1, x + 1, WALK + 2, z + 1, slab(ModBlocks.WHITE_POOL_TILE_SLAB, SlabType.TOP));
		c.set(x, WALK + 3, z, stairs(ModBlocks.AQUA_POOL_TILE_STAIRS, looking.getOpposite(), false));
	}

	static final Component[][] RULES = {
			{text("POOL RULE", ChatFormatting.BOLD), text("NO RUNNING", ChatFormatting.DARK_RED, ChatFormatting.BOLD),
					text("Walk at all times"), null},
			{text("POOL RULE", ChatFormatting.BOLD), text("NO DIVING", ChatFormatting.DARK_BLUE, ChatFormatting.BOLD), null, null},
			{text("POOL RULE", ChatFormatting.BOLD), text("SWIM AT YOUR", ChatFormatting.DARK_AQUA), text("OWN RISK",
					ChatFormatting.DARK_AQUA), null},
			{text("POOL RULE", ChatFormatting.BOLD), text("LIFEGUARD", ChatFormatting.DARK_RED), text("ON DUTY",
					ChatFormatting.DARK_RED), null},
	};

	/** Half the ordinary rooms have one of the four pool rule signs on the north wall. */
	static void maybeRuleSign(Canvas c, int x) {
		boolean place = c.random.nextBoolean();
		int which = c.random.nextInt(RULES.length);
		if (place) {
			c.sign(x, WALK + 2, 1, Direction.SOUTH, DyeColor.BLACK, false, RULES[which]);
		}
	}

	// ------------------------------------------------------------------ the rooms

	/** The safe room you arrive in: a calm pool, a welcome sign, the pool rules and the Lost &amp; Found. */
	static void entrance(Canvas c) {
		List<Opening> doors = shell(c, 56, WHITE, true);
		outline(c, 1, 1, 14, 14, FLOOR, AQUA);
		pool(c, 3, 7, 12, 12, 4, AQUA, MOSAIC);
		poolLadder(c, 5, 7, 4, Direction.SOUTH);
		poolLadder(c, 10, 7, 4, Direction.SOUTH);
		for (int z = 11; z <= 13; z++) {
			c.set(1, WALK, z, facing(ModBlocks.POOL_CHAIR, Direction.EAST));
			c.set(14, WALK, z, facing(ModBlocks.POOL_CHAIR, Direction.WEST));
		}
		c.sign(7, WALK + 3, 1, Direction.SOUTH, DyeColor.BLACK, false, text("~ WELCOME ~", ChatFormatting.DARK_AQUA,
				ChatFormatting.BOLD), text("to the"), text("POOLROOMS", ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
		c.sign(8, WALK + 3, 1, Direction.SOUTH, DyeColor.BLACK, false, text("Please enjoy"), text("your swim!"));
		c.sign(7, WALK + 1, 1, Direction.SOUTH, DyeColor.BLACK, false, text("POOL RULES", ChatFormatting.BOLD),
				text("1. No Running", ChatFormatting.DARK_RED), text("2. No Diving"));
		c.sign(8, WALK + 1, 1, Direction.SOUTH, DyeColor.BLACK, false, text("3. Swim at Your"), text("Own Risk"),
				text("4. Lifeguard"), text("on Duty"));
		c.locker(2, WALK, 1, Direction.SOUTH, LOST_AND_FOUND, Component.literal("Lost & Found"));
		c.sign(2, WALK + 1, 1, Direction.SOUTH, DyeColor.BLACK, false, text("LOST & FOUND", ChatFormatting.BOLD),
				text("Take what"), text("you need!"));
		c.sign(13, WALK + 1, 1, Direction.SOUTH, DyeColor.BLACK, false, text("HOW TO LEAVE", ChatFormatting.BOLD),
				text("Find 3 wristbands"), text("Blue Yellow Red"), text("Then find EXIT", ChatFormatting.DARK_GREEN));
		// the Lifeguard can never come in: an invisible wall (for it only) fills the doorways
		for (Opening o : doors) {
			for (int a = o.from(); a <= o.to(); a++) {
				for (int y = WALK; y <= WALK + 2; y++) {
					wall(c, o.side(), a, y, BARRIER);
				}
			}
		}
	}

	static void poolHall(Canvas c) {
		shell(c, 55, WHITE, true);
		outline(c, 1, 1, 14, 14, FLOOR, AQUA);
		pool(c, 4, 3, 11, 12, 4, AQUA, WHITE);
		c.fill(6, FLOOR - 4, 4, 6, FLOOR - 4, 11, LANE);
		c.fill(9, FLOOR - 4, 4, 9, FLOOR - 4, 11, LANE);
		poolLadder(c, 5, 3, 4, Direction.SOUTH);
		poolLadder(c, 10, 3, 4, Direction.SOUTH);
		for (int[] p : new int[][] {{2, 2}, {13, 2}, {2, 13}, {13, 13}}) {
			c.fill(p[0], WALK, p[1], p[0], 54, p[1], AQUA);
		}
		maybeRuleSign(c, 3);
	}

	static void bigPoolHall(Canvas c) {
		shell(c, 58, WHITE, true);
		outline(c, 1, 1, 30, 30, FLOOR, AQUA);
		pool(c, 6, 7, 25, 24, 4, AQUA, WHITE);
		for (int x = 9; x <= 21; x += 4) {
			c.fill(x, FLOOR - 4, 9, x, FLOOR - 4, 22, LANE);
		}
		poolLadder(c, 8, 7, 4, Direction.SOUTH);
		poolLadder(c, 23, 7, 4, Direction.SOUTH);
		for (int x : new int[] {4, 11, 20, 27}) {
			c.fill(x, WALK, 4, x, 57, 4, AQUA);
			c.fill(x, WALK, 27, x, 57, 27, AQUA);
		}
		for (int z : new int[] {11, 20}) {
			c.fill(4, WALK, z, 4, 57, z, AQUA);
			c.fill(27, WALK, z, 27, 57, z, AQUA);
		}
		highChair(c, 28, 16, Direction.WEST);
		c.sign(3, WALK + 2, 1, Direction.SOUTH, DyeColor.BLACK, false, RULES[0]);
		c.sign(28, WALK + 2, 1, Direction.SOUTH, DyeColor.BLACK, false, RULES[1]);
	}

	private static boolean hallway(int x, int z) {
		boolean cross = (x >= 6 && x <= 9) || (z >= 6 && z <= 9);
		boolean around = x <= 2 || x >= 13 || z <= 2 || z >= 13;
		return cross || around;
	}

	/** Narrow hallways with ankle-deep water and arches, between four big tiled blocks. */
	static void corridors(Canvas c) {
		int ceil = 54;
		walls(c, ceil);
		for (int x = 1; x <= 14; x++) {
			for (int z = 1; z <= 14; z++) {
				if (hallway(x, z)) {
					boolean lane = (x >= 7 && x <= 8 && (z < 6 || z > 9)) || (z >= 7 && z <= 8 && (x < 6 || x > 9));
					c.set(x, FLOOR, z, lane ? LANE : WHITE);
					c.set(x, WALK, z, SHALLOW);
					c.fill(x, WALK + 1, z, x, ceil - 1, z, AIR);
					c.set(x, ceil, z, x % 3 == 1 && z % 3 == 1 ? CEILING_LIGHT : WHITE);
				} else {
					c.fill(x, FLOOR, z, x, ceil, z, WHITE);
					c.set(x, WALK, z, AQUA);
				}
			}
		}
		for (int a : new int[] {2, 4, 11, 13}) {
			c.set(6, ceil - 1, a, stairs(ModBlocks.WHITE_POOL_TILE_STAIRS, Direction.WEST, true));
			c.set(9, ceil - 1, a, stairs(ModBlocks.WHITE_POOL_TILE_STAIRS, Direction.EAST, true));
			c.set(a, ceil - 1, 6, stairs(ModBlocks.WHITE_POOL_TILE_STAIRS, Direction.NORTH, true));
			c.set(a, ceil - 1, 9, stairs(ModBlocks.WHITE_POOL_TILE_STAIRS, Direction.SOUTH, true));
		}
		doorways(c);
	}

	/** Dozens of thin columns rising out of shallow water. */
	static void pillarForest(Canvas c) {
		int ceil = c.room.big() ? 58 : 56;
		shell(c, ceil, AQUA, true);
		c.fill(1, WALK, 1, c.width - 2, WALK, c.depth - 2, SHALLOW);
		for (int gx = 3; gx <= c.width - 4; gx += 3) {
			for (int gz = 3; gz <= c.depth - 4; gz += 3) {
				int x = gx + c.random.nextInt(3) - 1;
				int z = gz + c.random.nextInt(3) - 1;
				boolean aqua = c.random.nextBoolean();
				if (x < 3 || z < 3 || x > c.width - 4 || z > c.depth - 4) {
					continue;
				}
				c.fill(x, WALK, z, x, ceil - 1, z, aqua ? AQUA : WHITE);
			}
		}
		maybeRuleSign(c, 3);
	}

	/** A tall room with a glowing fake-sky ceiling and a reflecting pool. */
	static void atrium(Canvas c) {
		boolean big = c.room.big();
		int ceil = big ? 70 : 66;
		shell(c, ceil, WHITE, false);
		c.fill(1, ceil, 1, c.width - 2, ceil, c.depth - 2, SKY);
		for (int y = 54; y < ceil - 1; y += 5) {
			ring(c, y, AQUA);
		}
		for (int x = 2; x <= c.width - 2; x += 5) {
			for (int z = 2; z <= c.depth - 2; z += 5) {
				c.set(x, 53, z, light(15, false));
				c.set(x, 60, z, light(15, false));
			}
		}
		if (big) {
			pool(c, 10, 10, 21, 21, 2, AQUA, MOSAIC);
			for (int[] p : new int[][] {{9, 9}, {22, 9}, {9, 22}, {22, 22}}) {
				c.fill(p[0], WALK, p[1], p[0], ceil - 1, p[1], WHITE);
			}
		} else {
			pool(c, 5, 5, 10, 10, 2, AQUA, MOSAIC);
		}
		maybeRuleSign(c, 3);
	}

	/** Pool chairs, towels and lockers full of loot. */
	static void lounge(Canvas c) {
		shell(c, 55, WHITE, true);
		for (int x = 1; x <= 14; x++) {
			for (int z = 1; z <= 14; z++) {
				if ((x / 2 + z / 2) % 2 == 0) {
					c.set(x, FLOOR, z, AQUA);
				}
			}
		}
		for (int z : new int[] {1, 2, 3, 4, 11, 12, 13, 14}) {
			c.locker(14, WALK, z, Direction.WEST, LOUNGE_LOCKER, null);
			c.locker(14, WALK + 1, z, Direction.WEST, z % 2 == 0 ? LOUNGE_LOCKER : null, null);
		}
		Block[] towels = {Blocks.LIGHT_BLUE_CARPET, Blocks.WHITE_CARPET, Blocks.YELLOW_CARPET};
		int i = 0;
		for (int z : new int[] {2, 3, 4, 11, 12, 13}) {
			c.set(2, WALK, z, facing(ModBlocks.POOL_CHAIR, Direction.EAST));
			c.set(3, WALK, z, towels[i++ % towels.length].defaultBlockState());
		}
		for (int x = 6; x <= 9; x++) {
			for (int z = 6; z <= 9; z++) {
				c.set(x, WALK, z, ((x + z) % 2 == 0 ? Blocks.WHITE_CARPET : Blocks.LIGHT_BLUE_CARPET).defaultBlockState());
			}
		}
		maybeRuleSign(c, 3);
	}

	/** A very deep pool with an underwater tunnel. The blue wristband is at the bottom, in a little chamber. */
	static void deepEnd(Canvas c) {
		shell(c, 55, WHITE, true);
		outline(c, 1, 1, 14, 14, FLOOR, AQUA);
		int bottom = 28;
		c.fill(2, bottom, 2, 13, FLOOR - 1, 13, AQUA);
		c.fill(3, bottom, 3, 12, bottom, 12, LANE);
		c.fill(3, bottom + 1, 3, 12, FLOOR, 12, WATER);
		outline(c, 2, 2, 13, 13, FLOOR, AQUA);
		for (int y : new int[] {45, 39, 33}) {
			for (int a : new int[] {5, 10}) {
				c.set(2, y, a, POOL_LIGHT);
				c.set(13, y, a, POOL_LIGHT);
				c.set(a, y, 2, POOL_LIGHT);
				c.set(a, y, 13, POOL_LIGHT);
			}
		}
		c.fill(5, bottom + 1, 12, 5, FLOOR, 12, ladder(Direction.NORTH, true));
		// the tunnel: down through a hole in the floor, then north to a small chamber
		c.fill(4, 23, 0, 11, 28, 4, AQUA);
		c.fill(6, 23, 3, 9, 27, 9, AQUA);
		c.fill(5, 24, 1, 10, 27, 3, WATER);
		c.fill(7, 24, 3, 8, 26, 8, WATER);
		c.fill(7, 24, 7, 8, 28, 8, WATER);
		c.fill(5, 23, 1, 10, 23, 3, MOSAIC);
		int[] band = Layout.DEEP_END_BAND;
		c.set(band[0], band[1] - 1, band[2], MOSAIC);
		c.set(band[0], band[1], band[2], band(ModBlocks.BLUE_WRISTBAND, true));
		c.set(9, 26, 2, light(8, true));
		// a diving board (and the sign that says not to use it)
		c.fill(8, WALK, 10, 8, WALK, 13, slab(ModBlocks.WHITE_POOL_TILE_SLAB, SlabType.BOTTOM));
		c.sign(4, WALK + 2, 1, Direction.SOUTH, DyeColor.BLACK, false, text("THE DEEP END", ChatFormatting.BOLD),
				text("20 blocks deep"));
		c.sign(10, WALK + 1, 14, Direction.NORTH, DyeColor.BLACK, false, text("NO DIVING", ChatFormatting.DARK_RED,
				ChatFormatting.BOLD), text("(the board is"), text("just for show)"));
	}

	/** A tall room: stairs up to the top of a tiled tower, and a water slide back down into a splash pool. */
	static void slideTower(Canvas c) {
		int ceil = 74;
		List<Opening> doors = shell(c, ceil, WHITE, true);
		outline(c, 1, 1, 14, 14, FLOOR, AQUA);
		for (int y : new int[] {55, 62, 69}) {
			ring(c, y, AQUA);
		}
		for (int x : new int[] {2, 7, 12}) {
			for (int z : new int[] {6, 11}) {
				c.set(x, 53, z, light(15, false));
			}
		}
		// the tower along the north wall; its top is at y = 59, so you stand at y = 60
		c.fill(1, WALK, 1, 14, 59, 3, WHITE);
		c.fill(1, WALK, 3, 14, 58, 3, AQUA);
		c.fill(1, 59, 1, 14, 59, 3, MOSAIC);
		for (Opening o : doors) {
			if (o.side() == Side.NORTH) {
				c.fill(o.from(), WALK, 1, o.to(), o.top(), 3, AIR);
			}
		}
		c.set(4, 62, 2, light(15, false));
		c.set(10, 62, 2, light(15, false));
		for (int x = 3; x <= 13; x++) {
			c.set(x, 60, 3, pane(false, x < 13, false, x > 3));
		}
		// stairs up the west wall
		for (int k = 0; k <= 10; k++) {
			int z = 14 - k;
			int y = WALK + k;
			if (k > 0) {
				c.fill(1, y - 1, z, 2, y - 1, z, WHITE);
			}
			c.fill(1, y, z, 2, y, z, stairs(ModBlocks.WHITE_POOL_TILE_STAIRS, Direction.NORTH, false));
		}
		pool(c, 4, 8, 11, 12, 4, AQUA, MOSAIC);
		poolLadder(c, 11, 12, 4, Direction.NORTH);
		slide(c);
		int[] band = Layout.SLIDE_BAND;
		c.set(band[0], band[1], band[2], band(ModBlocks.YELLOW_WRISTBAND, false));
		c.sign(11, WALK + 2, 4, Direction.SOUTH, DyeColor.BLACK, false, text("SLIDE TOWER", ChatFormatting.BOLD),
				text("Slide at your"), text("own risk!"));
	}

	/** Floor height of the slide at step i: it drops one block every three steps. */
	private static int slideFloor(int i) {
		return 58 - i / 3;
	}

	/**
	 * The slide: from the top of the tower it runs south along the east wall, west along the south wall, then north and
	 * over the edge into the splash pool. Water flows down it from a source at the top.
	 */
	static void slide(Canvas c) {
		List<int[]> path = new ArrayList<>();
		for (int z = 4; z <= 14; z++) {
			path.add(new int[] {14, z});
		}
		for (int x = 13; x >= 4; x--) {
			path.add(new int[] {x, 14});
		}
		for (int z = 13; z >= 9; z--) {
			path.add(new int[] {4, z});
		}
		for (int i = 0; i < path.size(); i++) {
			int x = path.get(i)[0];
			int z = path.get(i)[1];
			int f = slideFloor(i);
			c.set(x, f, z, AQUA);
			c.set(x, f + 1, z, AIR);
			c.set(x, f + 2, z, AIR);
			// a one-block rail on each open side keeps the water (and you) in the slide
			if (i <= 9) {
				c.set(13, f + 1, z, WHITE); // east wall run
			} else if (i >= 11 && i <= 19) {
				c.set(x, f + 1, 13, WHITE); // south wall run
			} else if (i == 20) {
				c.set(3, f + 1, 14, WHITE); // the turn towards the pool
			} else if (i >= 21) {
				c.set(3, f + 1, z, WHITE); // last run, open on both sides
				c.set(5, f + 1, z, WHITE);
			}
		}
		// hold up the last stretch where it crosses the deck
		c.fill(4, WALK, 13, 4, slideFloor(21) - 1, 13, WHITE);
		int[] start = path.get(0);
		c.set(start[0], slideFloor(0) + 1, start[1], WATER);
		c.flow(start[0], slideFloor(0) + 1, start[1]);
	}

	/** A high chair and a desk. The red wristband is on the desk. */
	static void office(Canvas c) {
		shell(c, 55, WHITE, true);
		for (int x = 1; x <= 14; x++) {
			for (int z = 1; z <= 14; z++) {
				if ((x / 2 + z / 2) % 2 == 0) {
					c.set(x, FLOOR, z, AQUA);
				}
			}
		}
		highChair(c, 3, 3, Direction.SOUTH);
		c.fill(9, WALK, 3, 12, WALK, 3, slab(ModBlocks.WHITE_POOL_TILE_SLAB, SlabType.TOP));
		c.set(10, WALK, 4, stairs(ModBlocks.AQUA_POOL_TILE_STAIRS, Direction.SOUTH, false));
		int[] band = Layout.OFFICE_BAND;
		c.set(band[0], band[1], band[2], band(ModBlocks.RED_WRISTBAND, false));
		for (int z = 11; z <= 13; z++) {
			c.locker(14, WALK, z, Direction.WEST, OFFICE_LOCKER, null);
		}
		c.sign(3, WALK + 4, 1, Direction.SOUTH, DyeColor.BLACK, false, text("LIFEGUARD", ChatFormatting.DARK_RED,
				ChatFormatting.BOLD), text("OFFICE", ChatFormatting.BOLD), text("Staff only"));
		c.sign(12, WALK + 2, 1, Direction.SOUTH, DyeColor.BLACK, false, text("TODAY'S RULE:"), text("NO RUNNING",
				ChatFormatting.DARK_RED, ChatFormatting.BOLD), text("Walk, please!"));
		c.set(3, WALK + 2, 14, facing(ModBlocks.LIFE_RING, Direction.NORTH));
		c.set(12, WALK + 2, 14, facing(ModBlocks.LIFE_RING, Direction.NORTH));
	}

	/** The way out: the EXIT door under a glowing green sign. */
	static void exit(Canvas c) {
		shell(c, 56, WHITE, true);
		c.fill(7, FLOOR, 1, 8, FLOOR, 14, AQUA);
		int[] door = Layout.EXIT_DOOR;
		c.fill(door[0] - 1, WALK, 0, door[0] + 2, WALK + 3, 0, LANE);
		for (int part = 0; part < 6; part++) {
			c.set(door[0] + part % 2, door[1] + part / 2, door[2], ModBlocks.EXIT_DOOR.defaultBlockState()
					.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(ExitDoorBlock.PART, part));
		}
		c.set(door[0], WALK + 4, 1, facing(ModBlocks.EXIT_SIGN, Direction.SOUTH).setValue(ExitSignBlock.RIGHT, false));
		c.set(door[0] + 1, WALK + 4, 1, facing(ModBlocks.EXIT_SIGN, Direction.SOUTH).setValue(ExitSignBlock.RIGHT, true));
		c.sign(5, WALK + 1, 1, Direction.SOUTH, DyeColor.BLACK, false, text("~ EXIT ~", ChatFormatting.DARK_GREEN,
				ChatFormatting.BOLD), text("Bring all 3"), text("wristbands, then"), text("right-click door"));
		c.sign(10, WALK + 1, 1, Direction.SOUTH, DyeColor.BLACK, false, text("Blue", ChatFormatting.BLUE, ChatFormatting.BOLD),
				text("Yellow", ChatFormatting.GOLD, ChatFormatting.BOLD), text("Red", ChatFormatting.RED, ChatFormatting.BOLD));
	}
}
