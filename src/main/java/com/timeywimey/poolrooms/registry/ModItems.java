package com.timeywimey.poolrooms.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import com.timeywimey.poolrooms.Poolrooms;
import com.timeywimey.poolrooms.item.ChlorineCompassItem;
import com.timeywimey.poolrooms.item.DescribedItem;

public final class ModItems {
	private ModItems() {
	}

	/** Items shown in the creative tab, in order (before the blocks). */
	public static final List<Item> TAB = new ArrayList<>();

	/** Wear it on your head: in water you can breathe and see clearly (Conduit Power). */
	public static final Item SNORKEL_MASK = register("snorkel_mask", DescribedItem::new, new Item.Properties().stacksTo(1)
			.rarity(Rarity.UNCOMMON)
			.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
					.setEquipSound(SoundEvents.ARMOR_EQUIP_TURTLE)
					.setDamageOnHurt(false)
					.build()));

	/** Wear them on your feet: you swim much faster (Dolphin's Grace in water). */
	public static final Item FLIPPERS = register("flippers", DescribedItem::new, new Item.Properties().stacksTo(1)
			.rarity(Rarity.UNCOMMON)
			.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.FEET)
					.setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
					.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Poolrooms.id("flippers")))
					.setDamageOnHurt(false)
					.build())
			.attributes(ItemAttributeModifiers.builder()
					.add(Attributes.WATER_MOVEMENT_EFFICIENCY, new AttributeModifier(Poolrooms.id("flippers"), 0.5,
							AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET)
					.build()));

	/** A harmless foam toy with huge knockback. It even pushes the Lifeguard away. */
	public static final Item POOL_NOODLE = register("pool_noodle", DescribedItem::new, new Item.Properties().stacksTo(1));

	public static final Item CHLORINE_COMPASS = register("chlorine_compass", ChlorineCompassItem::new, new Item.Properties()
			.stacksTo(1).rarity(Rarity.UNCOMMON));

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Poolrooms.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		TAB.add(item);
		return item;
	}

	public static void init() {
	}
}
