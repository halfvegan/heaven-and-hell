package com.timeywimey.poolrooms.test;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

public class PoolGameTests {
	@GameTest(maxTicks = 20)
	public void loads(GameTestHelper helper) {
		helper.succeed();
	}
}
