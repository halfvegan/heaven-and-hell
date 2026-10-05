package com.timeywimey.heavenhell.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.entity.AngelEntity;
import com.timeywimey.heavenhell.entity.ImpEntity;
import com.timeywimey.heavenhell.entity.LuciferEntity;

public final class ModEntities {
	private ModEntities() {
	}

	public static final EntityType<AngelEntity> ANGEL = register("angel",
			FabricEntityType.Builder.createMob(AngelEntity::new, MobCategory.CREATURE, b -> b
					.defaultAttributes(AngelEntity::createAttributes)
					.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules))
					.sized(0.6F, 1.95F).clientTrackingRange(10));

	public static final EntityType<AngelEntity> GATEKEEPER = register("gatekeeper",
			FabricEntityType.Builder.createMob(AngelEntity::new, MobCategory.MISC, b -> b
					.defaultAttributes(AngelEntity::createAttributes))
					.sized(0.6F, 1.95F).clientTrackingRange(10));

	public static final EntityType<ImpEntity> IMP = register("imp",
			FabricEntityType.Builder.createMob(ImpEntity::new, MobCategory.MONSTER, b -> b
					.defaultAttributes(ImpEntity::createAttributes)
					.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules))
					.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(8));

	public static final EntityType<LuciferEntity> LUCIFER = register("lucifer",
			FabricEntityType.Builder.createMob(LuciferEntity::new, MobCategory.MONSTER, b -> b
					.defaultAttributes(LuciferEntity::createAttributes))
					.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(12));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HeavenHell.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
	}
}
