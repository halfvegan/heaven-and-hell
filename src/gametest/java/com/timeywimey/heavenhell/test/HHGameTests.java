package com.timeywimey.heavenhell.test;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.entity.AngelEntity;
import com.timeywimey.heavenhell.entity.ImpEntity;
import com.timeywimey.heavenhell.entity.LuciferEntity;
import com.timeywimey.heavenhell.hell.LuciferBattle;
import com.timeywimey.heavenhell.registry.ModEntities;
import com.timeywimey.heavenhell.world.Layout;
import com.timeywimey.heavenhell.world.Realms;

/**
 * Server-side checks. The test server only creates the vanilla dimensions, so the builds are tried in the End and the
 * Nether there (in a real world they happen in Heaven and Hell, which the client tests cover).
 */
public class HHGameTests {
	@GameTest(maxTicks = 40)
	public void realmsRegistered(GameTestHelper helper) {
		var types = helper.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
		helper.assertTrue(types.containsKey(HeavenHell.id("heaven")), "Heaven dimension type missing");
		helper.assertTrue(types.containsKey(HeavenHell.id("hell")), "Hell dimension type missing");
		var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		for (String biome : new String[] {"elysian_fields", "golden_groves", "cloud_peaks", "brimstone_wastes", "soul_pits", "infernal_spires"}) {
			helper.assertTrue(biomes.containsKey(HeavenHell.id(biome)), "Biome missing: " + biome);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void templatesLoad(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		checkTemplate(helper, server, "heaven_gates", new Vec3i(97, 80, 97));
		checkTemplate(helper, server, "infernal_court", new Vec3i(97, 70, 97));
		helper.succeed();
	}

	private static void checkTemplate(GameTestHelper helper, MinecraftServer server, String name, Vec3i size) {
		Optional<StructureTemplate> template = server.getStructureTemplateManager().get(HeavenHell.id(name));
		helper.assertTrue(template.isPresent(), "Structure " + name + " did not load");
		helper.assertTrue(template.get().getSize().equals(size), "Structure " + name + " has size " + template.get().getSize());
	}

	private static ServerLevel realmOr(MinecraftServer server, ResourceKey<Level> realm, ResourceKey<Level> fallback) {
		ServerLevel level = server.getLevel(realm);
		return level != null ? level : server.getLevel(fallback);
	}

	/** No player is in that dimension during the test, so keep the build area loaded by hand. */
	private static void forceLoad(ServerLevel level, boolean on) {
		for (int cx = -3; cx <= 3; cx++) {
			for (int cz = -3; cz <= 3; cz++) {
				level.setChunkForced(cx, cz, on);
			}
		}
	}

	@GameTest(maxTicks = 600)
	public void heavenBuilds(GameTestHelper helper) {
		ServerLevel heaven = realmOr(helper.getLevel().getServer(), Realms.HEAVEN, Level.END);
		forceLoad(heaven, true);
		Realms.buildHeaven(heaven);
		helper.runAfterDelay(20, () -> {
			BlockState landing = heaven.getBlockState(BlockPos.containing(Layout.HEAVEN_ARRIVAL.x, 100, Layout.HEAVEN_ARRIVAL.z));
			helper.assertTrue(!landing.isAir(), "Cloud landing missing at the arrival point");
			BlockState gate = heaven.getBlockState(BlockPos.containing(-26, 103, -4));
			helper.assertTrue(!gate.isAir(), "Gate of Return is empty");
			long keepers = heaven.getEntitiesOfClass(AngelEntity.class, new AABB(BlockPos.containing(Layout.GATEKEEPER)).inflate(4.0),
					AngelEntity::isGatekeeper).size();
			helper.assertTrue(keepers == 1, "Expected exactly one Gatekeeper, found " + keepers);
			forceLoad(heaven, false);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 600)
	public void hellBuilds(GameTestHelper helper) {
		ServerLevel hell = realmOr(helper.getLevel().getServer(), Realms.HELL, Level.NETHER);
		forceLoad(hell, true);
		Realms.buildHell(hell);
		helper.runAfterDelay(20, () -> {
			BlockState floor = hell.getBlockState(BlockPos.containing(Layout.HELL_ARRIVAL).below());
			helper.assertTrue(!floor.isAir(), "No floor under the Hell arrival point");
			BlockState gate = hell.getBlockState(Layout.REDEMPTION_GATE_MIN);
			helper.assertTrue(!gate.isAir(), "Redemption Gate seal missing");
			for (int rank = 1; rank < Layout.QUARTERS.length; rank++) {
				BlockPos room = BlockPos.containing(Layout.QUARTERS[rank]);
				helper.assertTrue(!hell.getBlockState(room.below()).isAir(), "No floor in the rank " + rank + " quarters");
				helper.assertTrue(hell.getBlockState(room).isAir() && hell.getBlockState(room.above()).isAir(),
						"Rank " + rank + " quarters spawn point is blocked");
				helper.assertTrue(hell.getBlockEntity(Layout.QUARTER_CHESTS[rank]) != null, "No chest in the rank " + rank + " quarters");
			}
			LuciferEntity lucifer = LuciferBattle.spawnOnThrone(hell);
			helper.assertTrue(lucifer != null, "Lucifer did not spawn");
			helper.runAfterDelay(10, () -> {
				helper.assertTrue(lucifer.isPassenger(), "Lucifer is not seated on his throne");
				forceLoad(hell, false);
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 100)
	public void mobsLive(GameTestHelper helper) {
		AngelEntity angel = helper.spawn(ModEntities.ANGEL, new BlockPos(1, 2, 1));
		ImpEntity imp = helper.spawn(ModEntities.IMP, new BlockPos(3, 2, 3));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(angel.isAlive(), "Angel died");
			helper.assertTrue(imp.isAlive(), "Imp died");
			helper.succeed();
		});
	}
}
