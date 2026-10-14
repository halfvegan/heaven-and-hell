package com.timeywimey.poolrooms.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.entity.LifeguardEntity;

public final class ModEntities {
	private ModEntities() {
	}

	/** Base size of a player; the Lifeguard's SCALE attribute (1.45) makes it about 2.8 blocks tall. */
	public static final EntityType<LifeguardEntity> LIFEGUARD = register("lifeguard",
			FabricEntityType.Builder.createMob(LifeguardEntity::new, MobCategory.MISC, b -> b
					.defaultAttributes(LifeguardEntity::createAttributes))
					.sized(0.6F, 1.95F).clientTrackingRange(10));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Poolrooms.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
	}
}
