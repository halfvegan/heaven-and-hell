package com.timeywimey.heavenhell.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Fixed coordinates of the hand-built places in Heaven and Hell. These must match the structure files made by
 * tools/gen (heaven_gates.nbt and infernal_court.nbt).
 */
public final class Layout {
	private Layout() {
	}

	// ------------------------------------------------------------------ Heaven
	/** Lowest corner where heavenhell:heaven_gates is placed. */
	public static final BlockPos HEAVEN_ORIGIN = new BlockPos(-48, 64, -48);
	/** Area cleared of terrain before the structure is placed (min/max inclusive). */
	public static final BlockPos HEAVEN_CLEAR_MIN = new BlockPos(-48, 96, -48);
	public static final BlockPos HEAVEN_CLEAR_MAX = new BlockPos(48, 143, 48);
	/** Arriving souls float down onto the cloud landing. */
	public static final Vec3 HEAVEN_ARRIVAL = new Vec3(0.5, 116.0, 31.5);
	public static final float HEAVEN_ARRIVAL_YAW = 180.0F;
	public static final Vec3 GATEKEEPER = new Vec3(4.5, 101.0, 13.5);
	public static final Vec3[] ANGELS = {
			new Vec3(-7.5, 101.0, -2.5), new Vec3(7.5, 101.0, -6.5), new Vec3(0.5, 101.0, -20.5),
			new Vec3(22.5, 101.0, 2.5), new Vec3(-21.5, 101.0, -14.5)
	};
	/** Walking into this box (the shimmering arch) returns a soul to the living world. */
	public static final AABB RETURN_GATE = new AABB(-26.3, 101.0, -6.3, -24.7, 108.0, -0.7);
	public static final Vec3 HEAVEN_CENTER = new Vec3(0.5, 101.0, 0.5);

	// -------------------------------------------------------------------- Hell
	public static final BlockPos HELL_ORIGIN = new BlockPos(-48, 30, -48);
	public static final BlockPos HELL_CLEAR_MIN = new BlockPos(-48, 50, -48);
	public static final BlockPos HELL_CLEAR_MAX = new BlockPos(48, 96, 48);
	public static final Vec3 HELL_ARRIVAL = new Vec3(0.5, 50.0, 40.5);
	public static final float HELL_ARRIVAL_YAW = 180.0F;
	/** Where Lucifer's invisible seat entity sits (he rides it, so he is drawn sitting on the throne). */
	public static final Vec3 THRONE_SEAT = new Vec3(0.5, 53.5, -24.5);
	public static final float THRONE_YAW = 0.0F;
	/** Middle of the throne hall, where the battle takes place. */
	public static final Vec3 ARENA_CENTER = new Vec3(0.5, 50.0, -8.5);
	public static final AABB ARENA = new AABB(-11.0, 49.0, -28.0, 12.0, 67.0, 8.0);
	/** The sealed arch behind the throne. Opens after Lucifer is defeated. */
	public static final BlockPos REDEMPTION_GATE_MIN = new BlockPos(-2, 50, -30);
	public static final BlockPos REDEMPTION_GATE_MAX = new BlockPos(2, 56, -30);
	public static final AABB REDEMPTION_GATE = new AABB(-2.3, 50.0, -30.3, 3.3, 57.0, -28.7);
	/** Rooms in the Tower of Sin, one per rank (index 1..5). */
	public static final Vec3[] QUARTERS = {
			null,
			new Vec3(32.5, 50.0, -5.5),
			new Vec3(32.5, 58.0, -5.5),
			new Vec3(32.5, 66.0, -5.5),
			new Vec3(32.5, 74.0, -5.5),
			new Vec3(32.5, 82.0, -5.5)
	};
	/** A chest in each room that is restocked when someone reaches that rank. */
	public static final BlockPos[] QUARTER_CHESTS = {
			null,
			new BlockPos(36, 50, -7),
			new BlockPos(36, 58, -7),
			new BlockPos(36, 66, -7),
			new BlockPos(36, 74, -7),
			new BlockPos(36, 82, -7)
	};
	public static final Vec3 HELL_CENTER = new Vec3(0.5, 50.0, 0.5);
}
