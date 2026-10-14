package com.timeywimey.poolrooms.client;

import net.minecraft.client.renderer.entity.EntityRenderers;

import net.fabricmc.api.ClientModInitializer;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.registry.ModEntities;

public class PoolroomsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.LIFEGUARD, ctx -> new HumanoidNpcRenderer<>(ctx, Poolrooms.id("textures/entity/lifeguard.png")));
	}
}
