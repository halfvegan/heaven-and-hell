package com.timeywimey.heavenhell.test;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Walks through the mod and takes screenshots (saved by CI) so the visuals can be checked without a human.
 */
public class HHClientTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
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
			run(server, "heavenhell moments off");

			// ------------------------------------------------ a moment of choice
			run(server, "execute as @a at @s run fill ~-10 ~-1 ~-10 ~10 ~-1 ~10 minecraft:grass_block");
			run(server, "execute as @a at @s run fill ~-10 ~-2 ~-10 ~10 ~-2 ~10 minecraft:dirt");
			run(server, "execute as @a at @s run fill ~-10 ~ ~-10 ~10 ~6 ~10 minecraft:air");
			run(server, "execute as @a at @s run tp @s ~ ~ ~ 0 15");
			context.waitTicks(20);
			run(server, "execute as @a run heavenhell moment lava_animal");
			context.waitTicks(15);
			shot(context, sp, "01_time_freezes");
			context.waitTicks(40);
			context.takeScreenshot("02_moment_dialog");
			run(server, "dialog clear @a");
			context.waitTicks(5);
			context.takeScreenshot("03_frozen_cow");

			// ------------------------------------------------------------- heaven
			run(server, "heavenhell send @a heaven");
			context.waitTicks(120);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("10_heaven_judgment");
			run(server, "dialog clear @a");
			run(server, "execute in heavenhell:heaven run tp @a 0.5 101 30.5 180 5");
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("11_heaven_gates");
			run(server, "execute in heavenhell:heaven run tp @a 0.5 104 22.5 180 10");
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("12_heaven_road");
			run(server, "execute in heavenhell:heaven run tp @a 0.5 102 2.5 180 0");
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("13_heaven_plaza");
			run(server, "execute in heavenhell:heaven run tp @a -14.5 103 -3.5 -90 0");
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("14_gate_of_return");
			run(server, "execute in heavenhell:heaven run tp @a 0.5 102 -12.5 180 0");
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("15_hall_of_wonders");
			run(server, "execute in heavenhell:heaven run tp @a 60.5 135 60.5 135 25");
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("16_heaven_overview");
			run(server, "execute in heavenhell:heaven run tp @a 200.5 140 200.5 45 20");
			context.waitTicks(60);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("17_heaven_landscape");
			run(server, "execute as @a run heavenhell action heaven_info");
			context.waitTicks(10);
			context.takeScreenshot("18_heaven_info");
			run(server, "dialog clear @a");

			// wings + halo on the player (third person)
			run(server, "item replace entity @a armor.chest with heavenhell:angel_wings");
			run(server, "item replace entity @a armor.head with heavenhell:halo");
			run(server, "item replace entity @a weapon.mainhand with heavenhell:harp");
			run(server, "execute in heavenhell:heaven run tp @a 4.5 101 20.5 0 10");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("19_angel_player");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			run(server, "execute in heavenhell:heaven run tp @a 4.5 101 17.5 180 0");
			context.waitTicks(20);
			context.takeScreenshot("20_gatekeeper");
			run(server, "item replace entity @a armor.chest with minecraft:air");
			run(server, "item replace entity @a armor.head with minecraft:air");

			// ---------------------------------------------------------------- hell
			run(server, "heavenhell send @a hell");
			context.waitTicks(120);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("30_hell_judgment");
			run(server, "dialog clear @a");
			run(server, "effect give @a minecraft:night_vision 600 0 true");
			run(server, "execute in heavenhell:hell run tp @a 0.5 50 40.5 180 0");
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("31_gates_of_hell");
			run(server, "effect clear @a minecraft:night_vision");
			context.waitTicks(10);
			context.takeScreenshot("32_gates_of_hell_dark");
			run(server, "execute in heavenhell:hell run tp @a 0.5 51 -4.5 180 0");
			context.waitTicks(130);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("33_throne_hall");
			run(server, "execute in heavenhell:hell run tp @a 0.5 52 -16.5 180 -5");
			context.waitTicks(20);
			context.takeScreenshot("34_lucifer_on_throne");
			run(server, "heavenhell rank @a 5");
			for (int rank = 1; rank <= 5; rank++) {
				run(server, "execute in heavenhell:hell run tp @a " + quarters(rank));
				context.waitTicks(30);
				sp.getConnection().waitForChunksRender();
				context.takeScreenshot("4" + rank + "_quarters_rank" + rank);
			}
			run(server, "execute in heavenhell:hell run tp @a 60.5 80 60.5 135 20");
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("50_hell_overview");
			run(server, "execute in heavenhell:hell run tp @a 160.5 70 160.5 45 0");
			context.waitTicks(60);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("51_hell_landscape");
			run(server, "execute in heavenhell:hell run summon heavenhell:imp 0.5 50 30.5");
			run(server, "execute in heavenhell:hell run tp @a 0.5 50 34.5 180 10");
			context.waitTicks(10);
			context.takeScreenshot("52_imp");

			// lucifer close up, third person of a demon
			run(server, "item replace entity @a armor.chest with heavenhell:demon_wings");
			run(server, "item replace entity @a armor.head with heavenhell:infernal_crown");
			run(server, "item replace entity @a weapon.mainhand with heavenhell:hellfire_sword");
			run(server, "execute in heavenhell:hell run tp @a 0.5 50 30.5 0 10");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("53_demon_player");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			run(server, "execute in heavenhell:hell run tp @a 0.5 51.5 -20.5 180 -8");
			context.waitTicks(20);
			context.takeScreenshot("54_lucifer_close");
			run(server, "execute as @a run heavenhell deeds");
			context.waitTicks(10);
			context.takeScreenshot("60_book_of_deeds");
			run(server, "dialog clear @a");
			run(server, "execute as @a at @s run heavenhell action quarters");
			run(server, "execute in heavenhell:hell run tp @a 0.5 51 -16.5 180 0");
			context.waitTicks(10);
			run(server, "execute as @a run heavenhell action challenge_confirm");
			context.waitTicks(10);
			context.takeScreenshot("61_challenge_dialog");
			run(server, "dialog clear @a");
		}
	}

	private static String quarters(int rank) {
		int y = 50 + (rank - 1) * 8;
		return "32.5 " + y + " -5.5 180 15";
	}

	private static void run(TestServerContext server, String command) {
		server.runCommand(command);
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext sp, String name) {
		context.takeScreenshot(name);
	}
}
