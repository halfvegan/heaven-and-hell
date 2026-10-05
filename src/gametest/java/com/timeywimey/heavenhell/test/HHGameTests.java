package com.timeywimey.heavenhell.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.timeywimey.heavenhell.entity.AngelEntity;
import com.timeywimey.heavenhell.entity.ImpEntity;
import com.timeywimey.heavenhell.entity.LuciferEntity;
import com.timeywimey.heavenhell.hell.LuciferBattle;
import com.timeywimey.heavenhell.registry.ModEntities;
import com.timeywimey.heavenhell.world.Layout;
import com.timeywimey.heavenhell.world.Realms;

public class HHGameTests {
	@GameTest(maxTicks = 40)
	public void dimensionsExist(GameTestHelper helper) {
		helper.assertTrue(Realms.heaven(helper.getLevel().getServer()) != null, "Heaven dimension missing");
		helper.assertTrue(Realms.hell(helper.getLevel().getServer()) != null, "Hell dimension missing");
		helper.succeed();
	}

	@GameTest(maxTicks = 600)
	public void heavenBuilds(GameTestHelper helper) {
		ServerLevel heaven = Realms.heaven(helper.getLevel().getServer());
		Realms.buildHeaven(heaven);
		helper.runAfterDelay(20, () -> {
			BlockState landing = heaven.getBlockState(BlockPos.containing(Layout.HEAVEN_ARRIVAL.x, 100, Layout.HEAVEN_ARRIVAL.z));
			helper.assertTrue(!landing.isAir(), "Cloud landing missing at the arrival point");
			long keepers = heaven.getEntitiesOfClass(AngelEntity.class, new net.minecraft.world.phys.AABB(BlockPos.containing(Layout.GATEKEEPER)).inflate(4.0),
					AngelEntity::isGatekeeper).size();
			helper.assertTrue(keepers == 1, "Expected exactly one Gatekeeper, found " + keepers);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 600)
	public void hellBuilds(GameTestHelper helper) {
		ServerLevel hell = Realms.hell(helper.getLevel().getServer());
		Realms.buildHell(hell);
		helper.runAfterDelay(20, () -> {
			BlockState floor = hell.getBlockState(BlockPos.containing(Layout.HELL_ARRIVAL).below());
			helper.assertTrue(!floor.isAir(), "No floor under the Hell arrival point");
			BlockState gate = hell.getBlockState(Layout.REDEMPTION_GATE_MIN);
			helper.assertTrue(!gate.isAir(), "Redemption Gate seal missing");
			LuciferEntity lucifer = LuciferBattle.spawnOnThrone(hell);
			helper.assertTrue(lucifer != null, "Lucifer did not spawn");
			helper.runAfterDelay(10, () -> {
				helper.assertTrue(lucifer.isPassenger(), "Lucifer is not seated on his throne");
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
