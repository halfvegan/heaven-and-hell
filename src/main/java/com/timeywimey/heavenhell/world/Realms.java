package com.timeywimey.heavenhell.world;

import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.entity.AngelEntity;
import com.timeywimey.heavenhell.registry.ModBlocks;
import com.timeywimey.heavenhell.registry.ModEntities;
import com.timeywimey.heavenhell.soul.Soul;
import com.timeywimey.heavenhell.soul.SoulData;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Txt;

/** The two afterlife dimensions: building them, finding them and moving souls between worlds. */
public final class Realms {
	private Realms() {
	}

	public static final ResourceKey<Level> HEAVEN = ResourceKey.create(Registries.DIMENSION, HeavenHell.id("heaven"));
	public static final ResourceKey<Level> HELL = ResourceKey.create(Registries.DIMENSION, HeavenHell.id("hell"));

	public static ServerLevel heaven(MinecraftServer server) {
		return server.getLevel(HEAVEN);
	}

	public static ServerLevel hell(MinecraftServer server) {
		return server.getLevel(HELL);
	}

	public static boolean isHeaven(Level level) {
		return HEAVEN.equals(level.dimension());
	}

	public static boolean isHell(Level level) {
		return HELL.equals(level.dimension());
	}

	// ------------------------------------------------------------ building

	/** Builds the Pearly Gates / the Infernal Court the first time anyone needs them. */
	public static void ensureBuilt(MinecraftServer server, String realm) {
		WorldState state = WorldState.get(server);
		if (SoulData.HEAVEN.equals(realm) && !state.heavenBuilt) {
			ServerLevel level = heaven(server);
			if (level == null) {
				HeavenHell.LOGGER.error("Heaven dimension is missing - is the mod's data pack enabled?");
				return;
			}
			buildHeaven(level);
			state.heavenBuilt = true;
			state.changed();
		} else if (SoulData.HELL.equals(realm) && !state.hellBuilt) {
			ServerLevel level = hell(server);
			if (level == null) {
				HeavenHell.LOGGER.error("Hell dimension is missing - is the mod's data pack enabled?");
				return;
			}
			buildHell(level);
			state.hellBuilt = true;
			state.changed();
		}
	}

	public static void buildHeaven(ServerLevel level) {
		long start = System.currentTimeMillis();
		loadChunks(level, Layout.HEAVEN_CLEAR_MIN, Layout.HEAVEN_CLEAR_MAX);
		clear(level, Layout.HEAVEN_CLEAR_MIN, Layout.HEAVEN_CLEAR_MAX);
		BlockPos o = Layout.HEAVEN_ORIGIN;
		Cmd.in(level, "place template heavenhell:heaven_gates " + o.getX() + " " + o.getY() + " " + o.getZ());
		spawnHeavenFolk(level);
		label(level, 0.5, 120.5, 8.5, Txt.of("✦ The Pearly Gates ✦", "gold").bold(), 2.5F);
		label(level, -25.5, 109.6, -3.5, Txt.of("Gate of Return", "white").then(" - back to the living world", "gray"), 1.2F);
		label(level, 0.5, 115.5, -15.5, Txt.of("Hall of Wonders", "yellow").bold(), 1.6F);
		label(level, 24.5, 105.5, 0.5, Txt.of("Gardens of Rest", "green"), 1.4F);
		label(level, 0.5, 107.0, 33.5, Txt.of("Welcome, blessed soul", "aqua").italic(), 1.2F);
		HeavenHell.LOGGER.info("Built the Pearly Gates in {} ms", System.currentTimeMillis() - start);
	}

	public static void buildHell(ServerLevel level) {
		long start = System.currentTimeMillis();
		loadChunks(level, Layout.HELL_CLEAR_MIN, Layout.HELL_CLEAR_MAX);
		clear(level, Layout.HELL_CLEAR_MIN, Layout.HELL_CLEAR_MAX);
		BlockPos o = Layout.HELL_ORIGIN;
		Cmd.in(level, "place template heavenhell:infernal_court " + o.getX() + " " + o.getY() + " " + o.getZ());
		seal(level, Layout.HELL_CLEAR_MIN, Layout.HELL_CLEAR_MAX);
		label(level, 0.5, 66.0, 30.5, Txt.of("The Infernal Court", "red").bold(), 2.5F);
		label(level, 0.5, 65.0, -24.5, Txt.of("Throne of Lucifer", "dark_red").bold(), 1.4F);
		label(level, 32.5, 92.0, -12.5, Txt.of("Tower of Sin", "gold"), 1.6F);
		label(level, 0.5, 58.2, -29.4, Txt.of("Redemption Gate", "yellow").then(" - sealed", "gray"), 1.0F);
		HeavenHell.LOGGER.info("Built the Infernal Court in {} ms", System.currentTimeMillis() - start);
	}

	/** Makes sure the Gatekeeper and a few angels are around the Pearly Gates. */
	public static void spawnHeavenFolk(ServerLevel level) {
		AABB area = new AABB(Layout.HEAVEN_CENTER.add(-48, -20, -48), Layout.HEAVEN_CENTER.add(48, 30, 48));
		List<AngelEntity> folk = level.getEntitiesOfClass(AngelEntity.class, area, e -> true);
		boolean keeper = folk.stream().anyMatch(AngelEntity::isGatekeeper);
		long angels = folk.stream().filter(a -> !a.isGatekeeper()).count();
		if (!keeper) {
			AngelEntity gatekeeper = ModEntities.GATEKEEPER.create(level, EntitySpawnReason.STRUCTURE);
			if (gatekeeper != null) {
				gatekeeper.snapTo(Layout.GATEKEEPER);
				gatekeeper.setYRot(0.0F);
				gatekeeper.setYHeadRot(0.0F);
				gatekeeper.setPersistenceRequired();
				gatekeeper.setPermanentlyInvulnerable(true);
				level.addFreshEntity(gatekeeper);
			}
		}
		for (int i = (int) angels; i < Layout.ANGELS.length; i++) {
			AngelEntity angel = ModEntities.ANGEL.create(level, EntitySpawnReason.STRUCTURE);
			if (angel != null) {
				angel.snapTo(Layout.ANGELS[i]);
				angel.setPersistenceRequired();
				level.addFreshEntity(angel);
			}
		}
	}

	/** Floating text (a text display entity). */
	public static void label(ServerLevel level, double x, double y, double z, Txt text, float scale) {
		String s = Cmd.fmt(scale);
		Cmd.in(level, "summon minecraft:text_display " + Cmd.fmt(x) + " " + Cmd.fmt(y) + " " + Cmd.fmt(z)
				+ " {billboard:\"center\",see_through:false,shadow:true,background:0,Tags:[\"heavenhell_label\"],"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:["
				+ s + "f," + s + "f," + s + "f]},text:" + text.str() + "}");
	}

	public static void loadChunks(ServerLevel level, BlockPos min, BlockPos max) {
		for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
			for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
				level.getChunk(cx, cz);
			}
		}
	}

	/** Removes terrain (and fluids) so the structure has room. */
	public static void clear(ServerLevel level, BlockPos min, BlockPos max) {
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int z = min.getZ(); z <= max.getZ(); z++) {
				for (int y = min.getY(); y <= max.getY(); y++) {
					p.set(x, y, z);
					if (!level.getBlockState(p).isAir()) {
						level.setBlock(p, air, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	/** Plugs any lava or water touching the cleared box so it cannot pour in. */
	public static void seal(ServerLevel level, BlockPos min, BlockPos max) {
		BlockState rock = ModBlocks.BRIMSTONE.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = min.getX() - 1; x <= max.getX() + 1; x++) {
			for (int z = min.getZ() - 1; z <= max.getZ() + 1; z++) {
				for (int y = min.getY(); y <= max.getY() + 1; y++) {
					boolean edge = x < min.getX() || x > max.getX() || z < min.getZ() || z > max.getZ() || y > max.getY();
					if (!edge) {
						continue;
					}
					p.set(x, y, z);
					if (!level.getFluidState(p).isEmpty()) {
						level.setBlock(p, rock, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------ travel

	public static void teleport(ServerPlayer player, ServerLevel level, Vec3 pos, float yaw, float pitch) {
		Cmd.run(level.getServer(), String.format(Locale.ROOT, "execute in %s run tp %s %.3f %.3f %.3f %.1f %.1f",
				level.dimension().identifier(), player.getStringUUID(), pos.x, pos.y, pos.z, yaw, pitch));
	}

	/** Remembers where this player should come back to in the living world. */
	public static void rememberReturnPoint(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		Vec3 pos = player.position();
		soul.returnPoint = new SoulData.ReturnPoint(player.level().dimension().identifier().toString(), pos.x, pos.y, pos.z);
		Soul.save(player, soul);
	}

	/** Sends a soul back to the living world (bed / world spawn they would normally respawn at). */
	public static void returnToLiving(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		SoulData soul = Soul.get(player);
		ServerLevel target = null;
		Vec3 pos = null;
		if (soul.returnPoint != null) {
			Identifier dim = Identifier.tryParse(soul.returnPoint.dimension());
			if (dim != null) {
				target = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
			}
			pos = new Vec3(soul.returnPoint.x(), soul.returnPoint.y(), soul.returnPoint.z());
		}
		if (target == null || isHeaven(target) || isHell(target)) {
			target = server.overworld();
			BlockPos spawn = target.getRespawnData().pos();
			int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
			pos = new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
		}
		soul.realm = SoulData.LIVING;
		soul.pending = "";
		Soul.save(player, soul);
		teleport(player, target, pos, player.getYRot(), 0.0F);
	}
}
