package com.timeywimey.poolrooms.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/**
 * The plan of the endless Poolrooms. The world is a grid of 16x16 cells (one per chunk). Every cell belongs to a room:
 * most rooms fill one cell, some big rooms fill a 2x2 block of cells. Rooms are joined by doorways in the middle of
 * their walls. Everything here is worked out from the world seed and the cell position alone, so any chunk can be
 * built on its own and the compass can find rooms that are not loaded yet.
 */
public final class Layout {
	private Layout() {
	}

	public enum RoomType {
		ENTRANCE("Entrance Pool"),
		POOL_HALL("Pool Hall"),
		CORRIDORS("Flooded Corridors"),
		PILLAR_FOREST("Pillar Forest"),
		ATRIUM("Skylight Atrium"),
		LOUNGE("Lounge"),
		DEEP_END("The Deep End"),
		SLIDE_TOWER("Slide Tower"),
		OFFICE("Lifeguard Office"),
		EXIT("Exit Room");

		public final String title;

		RoomType(String title) {
			this.title = title;
		}

		public boolean special() {
			return this == DEEP_END || this == SLIDE_TOWER || this == OFFICE || this == EXIT;
		}
	}

	/** A room: its type and the block of cells it covers. */
	public record Room(RoomType type, int cellX, int cellZ, int sizeX, int sizeZ) {
		public int minX() {
			return cellX * 16;
		}

		public int minZ() {
			return cellZ * 16;
		}

		public int width() {
			return sizeX * 16;
		}

		public int depth() {
			return sizeZ * 16;
		}

		public boolean big() {
			return sizeX > 1 || sizeZ > 1;
		}

		public BlockPos local(int x, int y, int z) {
			return new BlockPos(minX() + x, y, minZ() + z);
		}
	}

	/** A doorway between two rooms: where it is along the wall (offset inside the 16-block cell edge) and how high. */
	public record Door(int from, int to, int top) {
		static final Door NORMAL = new Door(6, 9, 52);
		static final Door WIDE = new Door(5, 10, 53);
	}

	// ------------------------------------------------------------------ fixed layout near the entrance

	private static final Map<Long, RoomType> FORCED_CELLS = new HashMap<>();
	private static final Map<Long, RoomType> FORCED_BIG = new HashMap<>();
	private static final Set<Long> FORCED_EDGES = new HashSet<>();

	static {
		forceCell(0, 0, RoomType.ENTRANCE);
		forceCell(1, 0, RoomType.POOL_HALL);
		forceCell(-1, 0, RoomType.CORRIDORS);
		forceCell(0, 1, RoomType.PILLAR_FOREST);
		forceCell(1, 1, RoomType.LOUNGE);
		forceCell(0, -1, RoomType.ATRIUM);
		// the three wristband rooms and an exit, each well under a minute's walk from the entrance
		forceCell(2, 1, RoomType.DEEP_END);
		forceCell(-2, 2, RoomType.SLIDE_TOWER);
		forceCell(1, -3, RoomType.OFFICE);
		forceCell(-3, -1, RoomType.EXIT);
		FORCED_BIG.put(key(1, -1), RoomType.POOL_HALL);
		FORCED_BIG.put(key(-1, -1), RoomType.ATRIUM);
		FORCED_BIG.put(key(1, 1), RoomType.PILLAR_FOREST);
		// guaranteed open routes from the entrance to each of them
		forcePath(0, 0, 2, 1);
		forcePath(0, 0, -2, 2);
		forcePath(0, 0, 1, -3);
		forcePath(0, 0, -3, -1);
		FORCED_EDGES.add(edgeKey(0, -1, 1, -1));
	}

	private static void forceCell(int x, int z, RoomType type) {
		FORCED_CELLS.put(key(x, z), type);
	}

	/** Opens every wall along an L-shaped walk: first along x, then along z. */
	private static void forcePath(int x0, int z0, int x1, int z1) {
		int x = x0;
		int z = z0;
		while (x != x1) {
			int nx = x + Integer.signum(x1 - x);
			FORCED_EDGES.add(edgeKey(x, z, nx, z));
			x = nx;
		}
		while (z != z1) {
			int nz = z + Integer.signum(z1 - z);
			FORCED_EDGES.add(edgeKey(x, z, x, nz));
			z = nz;
		}
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	private static long edgeKey(int ax, int az, int bx, int bz) {
		int x = Math.min(ax, bx);
		int z = Math.min(az, bz);
		int dir = ax == bx ? 1 : 0;
		return (((long) x & 0xFFFFFFL) << 40) | (((long) z & 0xFFFFFFL) << 16) | dir;
	}

	// ------------------------------------------------------------------ randomness from the seed

	private static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	static long hash(long seed, int x, int z, int salt) {
		return mix(seed ^ mix(x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L));
	}

	private static double unit(long h) {
		return (h >>> 11) * 0x1.0p-53;
	}

	/** A seed for decorating one room, the same in every chunk the room covers. */
	public static long roomSeed(long seed, Room room) {
		return hash(seed, room.cellX(), room.cellZ(), 7 + room.type().ordinal());
	}

	// ------------------------------------------------------------------ rooms

	private static boolean macroHasForcedCell(int mx, int mz) {
		for (int dx = 0; dx < 2; dx++) {
			for (int dz = 0; dz < 2; dz++) {
				if (FORCED_CELLS.containsKey(key(mx * 2 + dx, mz * 2 + dz))) {
					return true;
				}
			}
		}
		return false;
	}

	/** The big-room type filling this 2x2 block of cells, or null if it is split into four single rooms. */
	private static RoomType bigRoom(long seed, int mx, int mz) {
		RoomType forced = FORCED_BIG.get(key(mx, mz));
		if (forced != null) {
			return forced;
		}
		if (macroHasForcedCell(mx, mz) || unit(hash(seed, mx, mz, 11)) >= 0.30) {
			return null;
		}
		double t = unit(hash(seed, mx, mz, 12));
		return t < 0.45 ? RoomType.POOL_HALL : t < 0.75 ? RoomType.PILLAR_FOREST : RoomType.ATRIUM;
	}

	private static final RoomType[] SINGLE = {
			RoomType.POOL_HALL, RoomType.CORRIDORS, RoomType.PILLAR_FOREST, RoomType.ATRIUM, RoomType.LOUNGE,
			RoomType.DEEP_END, RoomType.SLIDE_TOWER, RoomType.OFFICE, RoomType.EXIT};
	private static final int[] SINGLE_WEIGHTS = {18, 24, 12, 6, 12, 4, 4, 4, 3};
	private static final int NORMAL_TYPES = 5;

	private static RoomType singleRoom(long seed, int cx, int cz) {
		RoomType forced = FORCED_CELLS.get(key(cx, cz));
		if (forced != null) {
			return forced;
		}
		boolean nearEntrance = Math.abs(cx) <= 2 && Math.abs(cz) <= 2;
		int count = nearEntrance ? NORMAL_TYPES : SINGLE.length;
		int total = 0;
		for (int i = 0; i < count; i++) {
			total += SINGLE_WEIGHTS[i];
		}
		int pick = (int) (unit(hash(seed, cx, cz, 21)) * total);
		for (int i = 0; i < count; i++) {
			pick -= SINGLE_WEIGHTS[i];
			if (pick < 0) {
				return SINGLE[i];
			}
		}
		return RoomType.POOL_HALL;
	}

	/** The room that covers cell (cx, cz). */
	public static Room roomAt(long seed, int cx, int cz) {
		int mx = Math.floorDiv(cx, 2);
		int mz = Math.floorDiv(cz, 2);
		RoomType big = bigRoom(seed, mx, mz);
		if (big != null) {
			return new Room(big, mx * 2, mz * 2, 2, 2);
		}
		return new Room(singleRoom(seed, cx, cz), cx, cz, 1, 1);
	}

	public static Room roomAtBlock(long seed, int x, int z) {
		return roomAt(seed, x >> 4, z >> 4);
	}

	/** True when the position is inside the Entrance Pool of the Poolrooms (where the Lifeguard never goes). */
	public static boolean isEntrance(Level level, BlockPos pos) {
		return level instanceof ServerLevel server && Pool.isPoolrooms(level)
				&& roomAtBlock(server.getSeed(), pos.getX(), pos.getZ()).type() == RoomType.ENTRANCE;
	}

	// ------------------------------------------------------------------ doorways

	private static boolean closedNorth(Room room, int cx, int cz, int ox, int oz) {
		return (room.type() == RoomType.ENTRANCE || room.type() == RoomType.EXIT) && ox == cx && oz == cz - 1;
	}

	/**
	 * Whether there is a doorway between two neighbouring cells. Cells of the same room have no wall at all (true).
	 */
	public static boolean edgeOpen(long seed, int ax, int az, int bx, int bz) {
		Room a = roomAt(seed, ax, az);
		Room b = roomAt(seed, bx, bz);
		if (a.equals(b)) {
			return true;
		}
		if (closedNorth(a, ax, az, bx, bz) || closedNorth(b, bx, bz, ax, az)) {
			return false;
		}
		if (a.type() == RoomType.ENTRANCE || b.type() == RoomType.ENTRANCE) {
			return true;
		}
		if (FORCED_EDGES.contains(edgeKey(ax, az, bx, bz)) || a.type().special() || b.type().special()) {
			return true;
		}
		int x = Math.min(ax, bx);
		int z = Math.min(az, bz);
		return unit(hash(seed, x, z, ax == bx ? 41 : 40)) < 0.78;
	}

	private static boolean airy(RoomType type) {
		return type == RoomType.POOL_HALL || type == RoomType.PILLAR_FOREST || type == RoomType.ATRIUM
				|| type == RoomType.LOUNGE || type == RoomType.CORRIDORS;
	}

	/** The doorway between two neighbouring cells of different rooms (only meaningful if {@link #edgeOpen}). */
	public static Door door(long seed, int ax, int az, int bx, int bz) {
		Room a = roomAt(seed, ax, az);
		Room b = roomAt(seed, bx, bz);
		if (!airy(a.type()) || !airy(b.type())) {
			return Door.NORMAL;
		}
		int x = Math.min(ax, bx);
		int z = Math.min(az, bz);
		return unit(hash(seed, x, z, ax == bx ? 43 : 42)) < 0.25 ? Door.WIDE : Door.NORMAL;
	}

	// ------------------------------------------------------------------ things the compass looks for

	/** Room-local spots of the wristbands and the exit door (shared with the room builders). */
	public static final int[] DEEP_END_BAND = {7, 25, 2};
	public static final int[] SLIDE_BAND = {12, 60, 2};
	public static final int[] OFFICE_BAND = {11, 50, 3};
	public static final int[] EXIT_DOOR = {7, 49, 0};

	public static BlockPos landmark(Room room) {
		int[] p = switch (room.type()) {
			case DEEP_END -> DEEP_END_BAND;
			case SLIDE_TOWER -> SLIDE_BAND;
			case OFFICE -> OFFICE_BAND;
			case EXIT -> EXIT_DOOR;
			default -> new int[] {room.width() / 2, Pool.WALK, room.depth() / 2};
		};
		return room.local(p[0], p[1], p[2]);
	}

	/**
	 * The landmark (wristband or exit door) of the nearest room of this type, skipping ones that {@code skip} rejects
	 * (for example rooms whose wristband was already taken). Searches up to 32 cells away.
	 */
	public static BlockPos nearest(long seed, BlockPos from, RoomType type, Predicate<BlockPos> skip) {
		int fx = from.getX() >> 4;
		int fz = from.getZ() >> 4;
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		int foundAt = -1;
		for (int r = 0; r <= 32; r++) {
			if (foundAt >= 0 && r > foundAt + 2) {
				break;
			}
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					Room room = roomAt(seed, fx + dx, fz + dz);
					if (room.type() != type || room.cellX() != fx + dx || room.cellZ() != fz + dz) {
						continue;
					}
					BlockPos spot = landmark(room);
					if (skip != null && skip.test(spot)) {
						continue;
					}
					double d = spot.distSqr(from);
					if (d < bestDistance) {
						bestDistance = d;
						best = spot;
						if (foundAt < 0) {
							foundAt = r;
						}
					}
				}
			}
		}
		return best;
	}
}
