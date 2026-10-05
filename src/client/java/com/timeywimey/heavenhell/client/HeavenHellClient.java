package com.timeywimey.heavenhell.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.registry.ModEntities;

public class HeavenHellClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.ANGEL, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/angel.png")));
		EntityRenderers.register(ModEntities.GATEKEEPER, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/gatekeeper.png")));
		EntityRenderers.register(ModEntities.IMP, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/imp.png")));
		EntityRenderers.register(ModEntities.LUCIFER, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/lucifer.png")));
	}
}
