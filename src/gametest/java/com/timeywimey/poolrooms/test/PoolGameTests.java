package com.timeywimey.poolrooms.test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.block.LockerBlockEntity;
import com.timeywimey.poolrooms.block.WristbandBlock;
import com.timeywimey.poolrooms.entity.LifeguardEntity;
import com.timeywimey.poolrooms.registry.ModBlocks;
import com.timeywimey.poolrooms.registry.ModEntities;
import com.timeywimey.poolrooms.world.Layout;
import com.timeywimey.poolrooms.world.Layout.Room;
import com.timeywimey.poolrooms.world.Layout.RoomType;
import com.timeywimey.poolrooms.world.Pool;
import com.timeywimey.poolrooms.world.RoomsFeature;

/** Server-side checks of the dimension data, the room layout, the room builders and the Lifeguard. */
public class PoolGameTests {
	private static final long SEED = 1234567L;

	@GameTest(maxTicks = 40)
	public void dimensionRegistered(GameTestHelper helper) {
		var types = helper.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
		helper.assertTrue(types.containsKey(Poolrooms.id("poolrooms")), "Poolrooms dimension type missing");
		var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		helper.assertTrue(biomes.containsKey(Poolrooms.id("poolrooms")), "Poolrooms biome missing");
		var features = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		helper.assertTrue(features.containsKey(Poolrooms.id("rooms")), "Rooms placed feature missing");
		helper.assertTrue(BuiltInRegistries.FEATURE_TYPE.containsKey(Poolrooms.id("rooms")), "Rooms feature type missing");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void layoutNearEntrance(GameTestHelper helper) {
		for (long seed : new long[] {SEED, 0L, -42L, 8008135L}) {
			check(helper, seed, 0, 0, RoomType.ENTRANCE);
			check(helper, seed, 2, 1, RoomType.DEEP_END);
			check(helper, seed, -2, 2, RoomType.SLIDE_TOWER);
			check(helper, seed, 1, -3, RoomType.OFFICE);
			check(helper, seed, -3, -1, RoomType.EXIT);
			// every forced room can be walked to from the entrance through open doorways
			Set<Long> reached = walk(seed, 6);
			for (int[] cell : new int[][] {{2, 1}, {-2, 2}, {1, -3}, {-3, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}}) {
				helper.assertTrue(reached.contains(key(cell[0], cell[1])), "Cannot walk from the entrance to cell "
						+ cell[0] + "," + cell[1] + " (seed " + seed + ")");
			}
			// the compass finds the nearby wristband rooms
			BlockPos start = new BlockPos(8, Pool.WALK, 8);
			helper.assertTrue(new BlockPos(39, 25, 18).equals(Layout.nearest(seed, start, RoomType.DEEP_END, null)),
					"Compass does not point to the nearby Deep End");
			for (RoomType type : new RoomType[] {RoomType.SLIDE_TOWER, RoomType.OFFICE, RoomType.EXIT}) {
				BlockPos found = Layout.nearest(seed, start, type, null);
				helper.assertTrue(found != null && found.distSqr(start) < 64 * 64, "No " + type + " within 64 blocks of the entrance");
			}
		}
		helper.succeed();
	}

	private static void check(GameTestHelper helper, long seed, int x, int z, RoomType type) {
		Room room = Layout.roomAt(seed, x, z);
		helper.assertTrue(room.type() == type && !room.big(), "Cell " + x + "," + z + " should be " + type + " but is " + room);
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	/** All cells reachable from the entrance within {@code radius} cells. */
	private static Set<Long> walk(long seed, int radius) {
		Set<Long> seen = new HashSet<>();
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[] {0, 0});
		seen.add(key(0, 0));
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!queue.isEmpty()) {
			int[] c = queue.poll();
			for (int[] d : dirs) {
				int nx = c[0] + d[0];
				int nz = c[1] + d[1];
				if (Math.abs(nx) > radius || Math.abs(nz) > radius || seen.contains(key(nx, nz))) {
					continue;
				}
				if (Layout.edgeOpen(seed, c[0], c[1], nx, nz)) {
					seen.add(key(nx, nz));
					queue.add(new int[] {nx, nz});
				}
			}
		}
		return seen;
	}

	/** No player is near these far-away chunks during the test, so keep them loaded by hand. */
	private static void forceLoad(ServerLevel level, Room room, boolean on) {
		for (int cx = room.cellX() - 1; cx <= room.cellX() + room.sizeX(); cx++) {
			for (int cz = room.cellZ() - 1; cz <= room.cellZ() + room.sizeZ(); cz++) {
				level.setChunkForced(cx, cz, on);
			}
		}
	}

	private static void build(ServerLevel level, Room room) {
		forceLoad(level, room, true);
		for (int cx = room.cellX(); cx < room.cellX() + room.sizeX(); cx++) {
			for (int cz = room.cellZ(); cz < room.cellZ() + room.sizeZ(); cz++) {
				level.getChunk(cx, cz);
				RoomsFeature.buildChunk(level, SEED, room, cx, cz);
			}
		}
	}

	@GameTest(maxTicks = 400)
	public void roomsBuild(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int base = 300;
		RoomType[] types = RoomType.values();
		Room[] rooms = new Room[types.length + 3];
		for (int i = 0; i < types.length; i++) {
			rooms[i] = new Room(types[i], base + i * 3, base, 1, 1);
		}
		rooms[types.length] = new Room(RoomType.POOL_HALL, base, base + 6, 2, 2);
		rooms[types.length + 1] = new Room(RoomType.PILLAR_FOREST, base + 6, base + 6, 2, 2);
		rooms[types.length + 2] = new Room(RoomType.ATRIUM, base + 12, base + 6, 2, 2);
		for (Room room : rooms) {
			build(level, room);
		}
		helper.runAfterDelay(20, () -> {
			for (Room room : rooms) {
				BlockState floor = level.getBlockState(room.local(room.width() / 2 - 1, Pool.FLOOR, 1));
				helper.assertTrue(!floor.isAir(), "No floor in the " + room.type().title);
			}
			Room entrance = rooms[RoomType.ENTRANCE.ordinal()];
			helper.assertTrue(level.getBlockEntity(entrance.local(2, Pool.WALK, 1)) instanceof LockerBlockEntity locker
					&& locker.getLootTable() != null, "The Lost & Found has no loot");
			helper.assertTrue(level.getBlockState(entrance.local(0, Pool.WALK, 7)).is(ModBlocks.LIFEGUARD_BARRIER)
					|| level.getBlockState(entrance.local(15, Pool.WALK, 7)).is(ModBlocks.LIFEGUARD_BARRIER),
					"The entrance doorways are not closed to the Lifeguard");
			BlockState blue = level.getBlockState(Layout.landmark(rooms[RoomType.DEEP_END.ordinal()]));
			helper.assertTrue(blue.is(ModBlocks.BLUE_WRISTBAND), "No blue wristband at the bottom of the Deep End");
			helper.assertTrue(blue.getValue(WristbandBlock.WATERLOGGED), "The blue wristband is not underwater");
			helper.assertTrue(level.getBlockState(Layout.landmark(rooms[RoomType.SLIDE_TOWER.ordinal()])).is(ModBlocks.YELLOW_WRISTBAND),
					"No yellow wristband on top of the Slide Tower");
			helper.assertTrue(level.getBlockState(Layout.landmark(rooms[RoomType.OFFICE.ordinal()])).is(ModBlocks.RED_WRISTBAND),
					"No red wristband on the Lifeguard's desk");
			helper.assertTrue(level.getBlockState(Layout.landmark(rooms[RoomType.EXIT.ordinal()])).is(ModBlocks.EXIT_DOOR),
					"No EXIT door in the Exit Room");
			for (Room room : rooms) {
				forceLoad(level, room, false);
			}
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void lifeguardRules(GameTestHelper helper) {
		LifeguardEntity guard = helper.spawn(ModEntities.LIFEGUARD, new BlockPos(2, 2, 2));
		helper.runAfterDelay(20, () -> {
			float health = guard.getHealth();
			ServerLevel level = helper.getLevel();
			DamageSource hit = level.damageSources().generic();
			guard.hurtServer(level, hit, 10.0F);
			helper.assertTrue(guard.isAlive() && guard.getHealth() == health, "The Lifeguard can be hurt");
			helper.assertTrue(!guard.shouldBeSaved(), "The Lifeguard would be saved with the world");
			helper.assertTrue(guard.getBbHeight() > 2.6F, "The Lifeguard is not tall (height " + guard.getBbHeight() + ")");
			helper.succeed();
		});
	}
}
