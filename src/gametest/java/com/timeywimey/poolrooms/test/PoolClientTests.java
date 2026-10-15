package com.timeywimey.poolrooms.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.timeywimey.poolrooms.gameplay.PoolTravel;
import com.timeywimey.poolrooms.world.Layout;
import com.timeywimey.poolrooms.world.Pool;

/**
 * Plays through the mod and takes screenshots (saved by CI): in through the drain, a tour of every room, the
 * Lifeguard, the items, the fade, and out through the EXIT door.
 */
public class PoolClientTests implements FabricClientGameTest {
	private static final String POOL = "poolrooms:poolrooms";
	private static final Logger LOGGER = LoggerFactory.getLogger("PoolClientTests");
	private static final BlockPos[] PROBES = {new BlockPos(8, 50, 3), new BlockPos(8, 48, 3), new BlockPos(8, 52, 6),
			new BlockPos(8, 56, 6), new BlockPos(10, 56, 10), new BlockPos(4, 49, 4), new BlockPos(20, 50, 8), new BlockPos(20, 48, 8),
			new BlockPos(8, 40, 8)};

	/** Logs the block light at foot level in every room near the entrance: min / average per room. */
	private static void lightSurvey(TestServerContext server) {
		server.runOnServer(s -> {
			ServerLevel l = s.getLevel(Pool.DIMENSION);
			long seed = l.getSeed();
			java.util.Set<Layout.Room> seen = new java.util.HashSet<>();
			for (int cx = -3; cx <= 3; cx++) {
				for (int cz = -3; cz <= 3; cz++) {
					Layout.Room room = Layout.roomAt(seed, cx, cz);
					if (!seen.add(room)) {
						continue;
					}
					int min = 99;
					int sum = 0;
					int n = 0;
					for (int x = room.minX() + 1; x < room.minX() + room.width() - 1; x++) {
						for (int z = room.minZ() + 1; z < room.minZ() + room.depth() - 1; z++) {
							BlockPos p = new BlockPos(x, Pool.WALK, z);
							l.getChunk(p);
							BlockState st = l.getBlockState(p);
							if (st.isSolidRender() || !l.getFluidState(p).isEmpty() && st.is(net.minecraft.world.level.block.Blocks.WATER)) {
								continue;
							}
							int light = l.getBrightness(LightLayer.BLOCK, p);
							min = Math.min(min, light);
							sum += light;
							n++;
						}
					}
					LOGGER.info("[light] {} at cell {},{}: min {} avg {} over {} spots", room.type().title, room.cellX(), room.cellZ(),
							min, n == 0 ? 0 : String.format(Locale.ROOT, "%.1f", sum / (double) n), n);
				}
			}
		});
	}

	/** Logs what the server and the client each think is at a few spots in the Entrance Pool and the Pool Hall. */
	private static void probe(ClientGameTestContext context, TestServerContext server, String tag) {
		server.runOnServer(s -> {
			ServerLevel l = s.getLevel(Pool.DIMENSION);
			for (BlockPos p : PROBES) {
				LOGGER.info("[probe {}] server {} {} light={}", tag, p.toShortString(), l.getBlockState(p), l.getBrightness(LightLayer.BLOCK, p));
			}
		});
		context.runOnClient(c -> {
			for (BlockPos p : PROBES) {
				LOGGER.info("[probe {}] client {} {} light={} dim={}", tag, p.toShortString(), c.level.getBlockState(p),
						c.level.getBrightness(LightLayer.BLOCK, p), c.level.dimension().identifier());
			}
		});
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			TestServerContext server = sp.getServer();
			sp.getConnection().waitForChunksRender();
			run(server, "gamerule send_command_feedback false");
			run(server, "gamerule advance_time false");
			run(server, "gamerule advance_weather false");
			run(server, "time set noon");
			run(server, "weather clear");
			run(server, "poolrooms lifeguard off");

			// a views.txt file in the run directory replaces the tour with a scripted set of shots (for development)
			Path script = Path.of("views.txt");
			if (Files.exists(script)) {
				runScript(context, sp, server, script);
				return;
			}

			// ---------------------------------------------------------- in through the Strange Drain
			run(server, "fill -3 144 -3 7 153 7 minecraft:air");
			run(server, "fill -1 145 -1 5 145 5 minecraft:white_concrete");
			run(server, "fill -1 146 -1 5 149 -1 minecraft:light_blue_concrete");
			run(server, "fill -1 146 5 5 149 5 minecraft:light_blue_concrete");
			run(server, "fill -1 146 -1 -1 149 5 minecraft:light_blue_concrete");
			run(server, "fill 5 146 -1 5 149 5 minecraft:light_blue_concrete");
			run(server, "fill 0 146 0 4 149 4 minecraft:water");
			run(server, "setblock 2 146 2 poolrooms:strange_drain[waterlogged=true]");
			run(server, "gamemode spectator @a");
			view(context, sp, server, "minecraft:overworld", "01_drain_in_a_pool", 2.5, 151.0, -4.5, 2.5, 146.0, 2.5, 30);
			run(server, "gamemode creative @a");
			run(server, "tp @a 2.5 147.2 2.5 0 80");
			context.waitTicks(10);
			server.runOnServer(s -> PoolTravel.useDrain(player(s), new BlockPos(2, 146, 2)));
			context.waitTicks(30);
			context.takeScreenshot("02_drain_swirl");
			context.waitTicks(26);
			context.takeScreenshot("03_fade_to_white");
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.waitTicks(10);
			context.takeScreenshot("04_arrival_entrance_pool");
			probe(context, server, "arrival");
			lightSurvey(server);

			// ---------------------------------------------------------- the Entrance Pool
			run(server, "gamemode spectator @a");
			view(context, sp, server, POOL, "05_rules_board", 7.95, 50.6, 3.6, 7.95, 51.0, 1.0, 10);
			view(context, sp, server, POOL, "06_entrance_pool", 8.0, 52.5, 1.8, 8.0, 47.0, 12.0, 10);
			view(context, sp, server, POOL, "07_lost_and_found", 4.5, 50.6, 4.5, 2.5, 49.8, 1.5, 10);
			view(context, sp, server, POOL, "08_entrance_corner", 14.2, 52.5, 14.2, 3.0, 48.5, 3.0, 10);

			// ---------------------------------------------------------- the rooms
			view(context, sp, server, POOL, "10_pool_hall", 17.5, 51.6, 1.5, 25.0, 47.0, 11.0, 40);
			view(context, sp, server, POOL, "11_flooded_corridors", -8.0, 50.6, 1.6, -8.0, 50.0, 14.0, 40);
			view(context, sp, server, POOL, "12_flooded_corridors_ring", -14.5, 50.6, 14.4, -14.5, 50.2, 1.0, 10);
			view(context, sp, server, POOL, "13_pillar_forest", 1.6, 51.0, 17.0, 12.0, 49.5, 29.0, 40);
			view(context, sp, server, POOL, "14_lounge", 19.0, 51.5, 25.0, 30.0, 49.5, 19.0, 40);
			view(context, sp, server, POOL, "15_skylight_atrium", 1.6, 51.0, -1.6, 13.0, 58.0, -14.0, 40);
			view(context, sp, server, POOL, "16_big_pool_hall", 33.6, 54.0, -1.6, 54.0, 47.0, -24.0, 60);
			view(context, sp, server, POOL, "17_big_atrium", -1.6, 52.0, -1.6, -24.0, 60.0, -24.0, 60);
			view(context, sp, server, POOL, "18_big_pillar_forest", 33.6, 51.0, 33.6, 58.0, 50.0, 58.0, 60);
			view(context, sp, server, POOL, "20_deep_end", 40.0, 53.0, 31.0, 39.5, 36.0, 22.0, 60);
			view(context, sp, server, POOL, "21_deep_end_bottom", 40.5, 30.5, 26.0, 39.5, 28.0, 21.0, 20);
			view(context, sp, server, POOL, "22_blue_wristband", 41.5, 26.0, 18.5, 39.5, 25.1, 18.5, 20);
			view(context, sp, server, POOL, "23_slide_tower", -29.5, 54.0, 46.5, -19.0, 57.0, 36.0, 120);
			view(context, sp, server, POOL, "24_slide_tower_top", -22.5, 61.6, 33.6, -19.5, 60.2, 34.5, 20);
			view(context, sp, server, POOL, "25_slide_view_down", -18.0, 63.0, 34.0, -26.0, 50.0, 44.0, 20);
			view(context, sp, server, POOL, "26_lifeguard_office", 28.5, 51.5, -35.5, 19.0, 50.0, -45.0, 60);
			view(context, sp, server, POOL, "27_red_wristband", 26.5, 51.0, -42.5, 27.5, 50.2, -44.5, 10);
			view(context, sp, server, POOL, "28_exit_room", -40.0, 51.0, -4.0, -40.0, 51.0, -16.0, 60);

			// ---------------------------------------------------------- the Lifeguard
			run(server, "execute in " + POOL + " run summon poolrooms:lifeguard 24.5 49 3.5 {NoAI:1b,Rotation:[0f,0f]}");
			view(context, sp, server, POOL, "30_lifeguard", 24.5, 50.8, 9.5, 24.5, 51.5, 3.5, 20);
			view(context, sp, server, POOL, "31_lifeguard_close", 24.5, 51.6, 6.6, 24.5, 52.2, 3.5, 10);
			run(server, "execute in " + POOL + " run summon poolrooms:lifeguard -8.5 49 2.5 {NoAI:1b,Rotation:[0f,0f]}");
			view(context, sp, server, POOL, "32_lifeguard_down_the_corridor", -8.0, 50.6, 14.4, -8.5, 51.0, 2.5, 20);

			// ---------------------------------------------------------- items
			run(server, "gamemode survival @a");
			run(server, "execute in " + POOL + " run tp @a 8.5 49 4.5 180 0");
			context.waitTicks(10);
			run(server, "clear @a");
			run(server, "poolrooms kit");
			run(server, "item replace entity @a armor.head with poolrooms:snorkel_mask");
			run(server, "item replace entity @a armor.feet with poolrooms:flippers");
			run(server, "item replace entity @a weapon.mainhand with poolrooms:chlorine_compass");
			context.waitTicks(45);
			context.takeScreenshot("40_compass_in_hand");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("41_items_inventory");
			context.setScreen(() -> null);
			run(server, "item replace entity @a weapon.mainhand with poolrooms:pool_noodle");
			run(server, "execute in " + POOL + " run tp @a 8.5 49 4.5 0 -5");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(15);
			context.takeScreenshot("42_snorkel_flippers_noodle");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			// the fade to white on its own
			server.runOnServer(s -> PoolTravel.fade(player(s)));
			context.waitTicks(9);
			context.takeScreenshot("43_fade");
			context.waitTicks(50);

			// ---------------------------------------------------------- out through the EXIT door
			run(server, "execute in " + POOL + " run tp @a -40.0 49 -13.5 180 0");
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();
			run(server, "give @a poolrooms:blue_wristband");
			run(server, "give @a poolrooms:yellow_wristband");
			run(server, "give @a poolrooms:red_wristband");
			context.takeScreenshot("50_exit_door");
			server.runOnServer(s -> {
				ServerLevel pool = s.getLevel(Pool.DIMENSION);
				BlockPos door = new BlockPos(-41, 49, -16);
				BlockState state = pool.getBlockState(door);
				PoolTravel.useExitDoor(player(s), door, state);
			});
			context.waitTicks(10);
			context.takeScreenshot("51_exit_door_open");
			run(server, "execute in " + POOL + " run tp @a -40.0 49 -15.6 180 0");
			context.waitTicks(60);
			sp.getConnection().waitForChunksRender();
			context.waitTicks(30);
			context.takeScreenshot("52_back_home");

			// ---------------------------------------------------------- the Lifeguard's rules, played out for real
			run(server, "execute as @a run poolrooms enter");
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			new LifeguardBehavior(context, sp, server).run();
		}
	}

	/**
	 * Runs a little screenshot script, one step per line: {@code cmd <command>}, {@code view <name> <dimension> x y z
	 * tx ty tz [ticks]}, {@code wait <ticks>}, {@code shot <name>}, {@code hud on|off}, {@code camera first|back|front},
	 * {@code inventory <name>}, {@code gui <scale>}, {@code fade}, {@code lifeguard} (the behaviour checks) and
	 * {@code drain x y z}. Lines starting with # are comments.
	 */
	private static void runScript(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, Path script) {
		List<String> lines;
		try {
			lines = Files.readAllLines(script);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (String raw : lines) {
			String line = raw.strip();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			String[] a = line.split("\\s+");
			LOGGER.info("[script] {}", line);
			switch (a[0]) {
				case "cmd" -> run(server, line.substring(4).strip());
				case "wait" -> context.waitTicks(Integer.parseInt(a[1]));
				case "shot" -> {
					sp.getConnection().waitForChunksRender();
					context.takeScreenshot(a[1]);
				}
				case "view" -> view(context, sp, server, a[2], a[1], d(a[3]), d(a[4]), d(a[5]), d(a[6]), d(a[7]), d(a[8]),
						a.length > 9 ? Integer.parseInt(a[9]) : 20);
				case "hud" -> {
					boolean hide = a[1].equals("off");
					context.runOnClient(c -> {
						if (c.gui.hud.isHidden() != hide) {
							c.gui.hud.toggle();
						}
					});
				}
				case "camera" -> {
					CameraType type = switch (a[1]) {
						case "back" -> CameraType.THIRD_PERSON_BACK;
						case "front" -> CameraType.THIRD_PERSON_FRONT;
						default -> CameraType.FIRST_PERSON;
					};
					context.runOnClient(c -> c.options.setCameraType(type));
				}
				case "inventory" -> {
					context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
					context.getInput().setCursorPos(2, 2);
					context.waitTicks(5);
					context.takeScreenshot(a[1]);
					context.setScreen(() -> null);
				}
				case "fade" -> server.runOnServer(s -> PoolTravel.fade(player(s)));
				case "lifeguard" -> new LifeguardBehavior(context, sp, server).run();
				case "gui" -> {
					int scale = Integer.parseInt(a[1]);
					context.runOnClient(c -> {
						c.options.guiScale().set(scale);
						c.resizeGui();
					});
				}
				case "drain" -> {
					BlockPos drain = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
					server.runOnServer(s -> PoolTravel.useDrain(player(s), drain));
				}
				default -> LOGGER.warn("[script] unknown step: {}", line);
			}
		}
	}

	private static double d(String s) {
		return Double.parseDouble(s);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	/** Teleports the camera to (x, y, z) looking at (tx, ty, tz) and takes a screenshot. */
	private static void view(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, String dimension,
			String name, double x, double y, double z, double tx, double ty, double tz, int ticks) {
		double dx = tx - x;
		double dy = ty - (y + 1.62);
		double dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		run(server, String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f %.1f %.1f", dimension, x, y, z, yaw, pitch));
		context.waitTicks(ticks);
		sp.getConnection().waitForChunksRender();
		context.waitTicks(5);
		context.takeScreenshot(name);
	}

	private static void run(TestServerContext server, String command) {
		server.runCommand(command);
	}
}
