package com.timeywimey.heavenhell.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.registry.ModEntities;

public class HeavenHellClient implements ClientModInitializer {
	@Override
	@SuppressWarnings("deprecation")
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.ANGEL, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/angel.png")));
		EntityRendererRegistry.register(ModEntities.GATEKEEPER, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/gatekeeper.png")));
		EntityRendererRegistry.register(ModEntities.IMP, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/imp.png")));
		EntityRendererRegistry.register(ModEntities.LUCIFER, ctx -> new HumanoidNpcRenderer<>(ctx, HeavenHell.id("textures/entity/lucifer.png")));
	}
}
