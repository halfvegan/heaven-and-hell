package com.timeywimey.poolrooms.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.block.ExitDoorBlock;
import com.timeywimey.poolrooms.block.ExitLightBlock;
import com.timeywimey.poolrooms.block.FacingBlock;
import com.timeywimey.poolrooms.registry.ModBlocks;
import com.timeywimey.poolrooms.registry.ModSounds;
import com.timeywimey.poolrooms.util.Cmd;
import com.timeywimey.poolrooms.util.Scheduler;
import com.timeywimey.poolrooms.util.Txt;
import com.timeywimey.poolrooms.world.Pool;

/** Getting into the Poolrooms (the Strange Drain) and out again (the EXIT door). */
public final class PoolTravel {
	private PoolTravel() {
	}

	/** How long the drain swirls before it pulls you in (3 seconds). */
	public static final int SWIRL_TICKS = 60;

	private record Swirl(BlockPos drain, ResourceKey<Level> dimension, int start) {
	}

	private static final Map<UUID, Swirl> SWIRLS = new HashMap<>();
	private static final Set<UUID> MOVING = new HashSet<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(PoolTravel::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SWIRLS.clear();
			MOVING.clear();
		});
	}

	// ------------------------------------------------------------------ the fade to white

	/** Three copies of the white square glyph from assets/poolrooms/font/fade.json (U+E000), side by side. */
	private static final String FADE_GLYPHS = "";

	/** Fills the screen with white for a moment (a huge white glyph shown as a title). */
	public static void fade(ServerPlayer player) {
		Cmd.title(player, "{\"text\":\"" + FADE_GLYPHS + "\",\"font\":\"poolrooms:fade\",\"color\":\"white\",\"shadow_color\":0}",
				null, 8, 14, 22);
		Cmd.sound(player, ModSounds.FADE.location().toString(), 0.7F, 1.0F);
	}

	public static void teleport(ServerPlayer player, ServerLevel level, Vec3 pos, float yaw, float pitch) {
		player.fallDistance = 0.0;
		Cmd.run(level.getServer(), String.format(Locale.ROOT, "execute in %s run tp %s %.3f %.3f %.3f %.1f %.1f",
				level.dimension().identifier(), player.getStringUUID(), pos.x, pos.y, pos.z, yaw, pitch));
	}

	// ------------------------------------------------------------------ in: the Strange Drain

	/** How many blocks of water are above (and in) the drain. */
	public static int waterDepth(Level level, BlockPos drain) {
		int depth = 0;
		BlockPos.MutableBlockPos p = drain.mutable();
		for (int i = 0; i < 64 && level.getFluidState(p).is(FluidTags.WATER); i++) {
			depth++;
			p.move(Direction.UP);
		}
		return depth;
	}

	public static void useDrain(ServerPlayer player, BlockPos drain) {
		ServerLevel level = player.level();
		if (Pool.isPoolrooms(level)) {
			Cmd.actionbar(player, Txt.of("This drain doesn't lead anywhere from in here. Find an EXIT door!", "aqua").str());
			return;
		}
		if (SWIRLS.containsKey(player.getUUID()) || MOVING.contains(player.getUUID())) {
			return;
		}
		if (waterDepth(level, drain) < 2) {
			Cmd.actionbar(player, Txt.of("The drain needs to be at the bottom of water at least 2 blocks deep.", "yellow").str());
			level.playSound(null, drain, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.5F, 1.5F);
			return;
		}
		SWIRLS.put(player.getUUID(), new Swirl(drain.immutable(), level.dimension(), level.getServer().getTickCount()));
		level.playSound(null, drain, ModSounds.DRAIN_SWIRL, SoundSource.BLOCKS, 0.9F, 1.0F);
		Cmd.actionbar(player, Txt.of("The drain starts to swirl...", "aqua").str());
	}

	private static void tick(MinecraftServer server) {
		if (SWIRLS.isEmpty()) {
			return;
		}
		int now = server.getTickCount();
		Iterator<Map.Entry<UUID, Swirl>> it = SWIRLS.entrySet().iterator();
		List<Runnable> later = new ArrayList<>();
		while (it.hasNext()) {
			Map.Entry<UUID, Swirl> e = it.next();
			Swirl swirl = e.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			ServerLevel level = player.level();
			Vec3 center = Vec3.atBottomCenterOf(swirl.drain()).add(0.0, 0.5, 0.0);
			if (!level.dimension().equals(swirl.dimension()) || player.position().distanceToSqr(center) > 36.0
					|| !level.getBlockState(swirl.drain()).is(ModBlocks.STRANGE_DRAIN)) {
				it.remove();
				Cmd.actionbar(player, Txt.of("The drain stopped swirling.", "gray").str());
				continue;
			}
			int age = now - swirl.start();
			// bubbles spiral around you and the drain tugs you gently down
			double angle = age * 0.55;
			for (int k = 0; k < 3; k++) {
				double a = angle + k * (Math.PI * 2 / 3);
				double r = 1.1 - age / (double) SWIRL_TICKS * 0.7;
				level.sendParticles(ParticleTypes.BUBBLE, player.getX() + Math.cos(a) * r, player.getY() + 0.4 + k * 0.4,
						player.getZ() + Math.sin(a) * r, 2, 0.05, 0.05, 0.05, 0.0);
			}
			level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, center.x, center.y, center.z, 3, 0.4, 0.1, 0.4, 0.02);
			Vec3 pull = center.subtract(player.position()).scale(0.03);
			player.push(pull.x, Math.min(pull.y, 0.0) - 0.01, pull.z);
			player.needsSync = true;
			if (age == SWIRL_TICKS - 10) {
				fade(player);
			}
			if (age >= SWIRL_TICKS) {
				it.remove();
				ServerPlayer p = player;
				BlockPos drain = swirl.drain();
				later.add(() -> enterThroughDrain(p, drain));
			}
		}
		later.forEach(Runnable::run);
	}

	private static void enterThroughDrain(ServerPlayer player, BlockPos drain) {
		ServerLevel level = player.level();
		int depth = waterDepth(level, drain);
		BlockPos top = drain.above(Math.max(0, depth - 1));
		PoolPlayer.ReturnPoint back = new PoolPlayer.ReturnPoint(level.dimension().identifier().toString(),
				top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
		enter(player, back);
	}

	/** Sends a player to the Entrance Pool. {@code back} is where the EXIT door will take them. */
	public static void enter(ServerPlayer player, PoolPlayer.ReturnPoint back) {
		ServerLevel pool = Pool.level(player.level().getServer());
		if (pool == null) {
			Poolrooms.LOGGER.error("The Poolrooms dimension is missing - is the mod's data pack enabled?");
			Cmd.actionbar(player, Txt.of("The Poolrooms could not be found (is the mod installed correctly?)", "red").str());
			return;
		}
		PoolPlayer data = PoolPlayer.get(player);
		data.returnPoint = back;
		data.visits++;
		boolean first = !data.has(PoolPlayer.FLAG_WELCOMED);
		data.flags |= PoolPlayer.FLAG_WELCOMED;
		PoolPlayer.save(player, data);
		teleport(player, pool, Pool.ARRIVAL, Pool.ARRIVAL_YAW, 0.0F);
		LifeguardDirector.onEnter(player);
		Scheduler.after(30, () -> {
			if (!Pool.isPoolrooms(player.level())) {
				return;
			}
			if (first) {
				Cmd.title(player, Txt.of("The Poolrooms", "aqua").bold().str(),
						Txt.of("Find 3 wristbands. Find the EXIT. Don't run.", "white").str(), 10, 70, 20);
			} else {
				Cmd.actionbar(player, Txt.of("Back in the Poolrooms. Walk, don't run.", "aqua").str());
			}
		});
	}

	// ------------------------------------------------------------------ out: the EXIT door

	private static final Block[] BANDS = {ModBlocks.BLUE_WRISTBAND, ModBlocks.YELLOW_WRISTBAND, ModBlocks.RED_WRISTBAND};
	private static final String[] BAND_NAMES = {"Blue", "Yellow", "Red"};
	private static final String[] BAND_COLORS = {"blue", "gold", "red"};

	public static void useExitDoor(ServerPlayer player, BlockPos pos, BlockState state) {
		ServerLevel level = player.level();
		Inventory inventory = player.getInventory();
		List<Integer> missing = new ArrayList<>();
		for (int i = 0; i < BANDS.length; i++) {
			if (!CompassTarget.has(inventory, BANDS[i])) {
				missing.add(i);
			}
		}
		if (!missing.isEmpty() && !player.isCreative()) {
			Txt msg = Txt.of("The EXIT door needs all 3 wristbands. Still missing: ", "white");
			for (int k = 0; k < missing.size(); k++) {
				int i = missing.get(k);
				msg.then(BAND_NAMES[i] + (k < missing.size() - 1 ? ", " : ""), BAND_COLORS[i]);
			}
			Cmd.actionbar(player, msg.str());
			level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.7F, 1.0F);
			return;
		}
		if (!player.isCreative()) {
			for (Block band : BANDS) {
				for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
					ItemStack stack = inventory.getItem(slot);
					if (stack.is(band.asItem())) {
						stack.shrink(1);
						break;
					}
				}
			}
		}
		openDoor(level, pos, state);
		Cmd.actionbar(player, Txt.of("Blue, yellow, red... ", "white").then("the door is open! Walk through.", "green").str());
	}

	/** Turns the whole 2x3 door into light for 30 seconds. */
	public static void openDoor(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FacingBlock.FACING);
		BlockPos origin = ExitDoorBlock.origin(pos, state);
		for (int part = 0; part < 6; part++) {
			BlockPos p = ExitDoorBlock.partPos(origin, facing, part);
			if (level.getBlockState(p).is(ModBlocks.EXIT_DOOR)) {
				level.setBlock(p, ModBlocks.EXIT_LIGHT.defaultBlockState().setValue(FacingBlock.FACING, facing)
						.setValue(ExitDoorBlock.PART, part), Block.UPDATE_ALL);
				level.scheduleTick(p, ModBlocks.EXIT_LIGHT, ExitLightBlock.OPEN_TICKS);
			}
		}
		level.playSound(null, pos, ModSounds.EXIT_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** Takes a player home: back to the water above the drain they came in through. */
	public static void exitHome(ServerPlayer player, boolean throughDoor) {
		UUID id = player.getUUID();
		if (!MOVING.add(id)) {
			return;
		}
		fade(player);
		Scheduler.after(10, () -> {
			MOVING.remove(id);
			if (!player.isAlive() || !Pool.isPoolrooms(player.level())) {
				return;
			}
			goHome(player);
			LifeguardDirector.onLeave(player);
			if (throughDoor) {
				Advancements.award(player, "escaped");
				Scheduler.after(20, () -> Cmd.title(player, Txt.of("You made it out!", "green").bold().str(),
						Txt.of("Thanks for swimming with us.", "aqua").str(), 10, 60, 20));
			}
		});
	}

	private static void goHome(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		PoolPlayer data = PoolPlayer.get(player);
		ServerLevel target = null;
		Vec3 pos = null;
		if (data.returnPoint != null) {
			Identifier dim = Identifier.tryParse(data.returnPoint.dimension());
			if (dim != null) {
				target = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
			}
			pos = new Vec3(data.returnPoint.x(), data.returnPoint.y(), data.returnPoint.z());
		}
		if (target == null || Pool.isPoolrooms(target)) {
			target = server.overworld();
			BlockPos spawn = target.getRespawnData().pos();
			int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
			pos = new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
		}
		teleport(player, target, pos, player.getYRot(), 0.0F);
	}

	public static boolean isMoving(ServerPlayer player) {
		return MOVING.contains(player.getUUID());
	}

	public static void setMoving(ServerPlayer player, boolean moving) {
		if (moving) {
			MOVING.add(player.getUUID());
		} else {
			MOVING.remove(player.getUUID());
		}
	}
}
