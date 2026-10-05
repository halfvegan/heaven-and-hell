package com.timeywimey.heavenhell.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.item.BookOfDeedsItem;
import com.timeywimey.heavenhell.item.CloudBottleItem;
import com.timeywimey.heavenhell.item.FeatherOfReturnItem;
import com.timeywimey.heavenhell.item.HarpItem;
import com.timeywimey.heavenhell.item.MannaItem;
import com.timeywimey.heavenhell.item.PalaceKeyItem;

public final class ModItems {
	private ModItems() {
	}

	/** Items shown in the creative tab, in order. */
	public static final List<Item> TAB = new ArrayList<>();

	// ------------------------------------------------------------------ Heaven
	public static final Item ANGEL_WINGS = register("angel_wings", Item::new, wings("angel_wings").rarity(Rarity.EPIC));
	public static final Item HALO = register("halo", Item::new, headwear().rarity(Rarity.EPIC));
	public static final Item HARP = register("harp", HarpItem::new, props().stacksTo(1).rarity(Rarity.RARE));
	public static final Item SERAPH_BLADE = register("seraph_blade", Item::new, props()
			.sword(ToolMaterial.DIAMOND, 4.0F, -2.2F).rarity(Rarity.EPIC));
	public static final Item MANNA = register("manna", MannaItem::new, props()
			.food(new FoodProperties.Builder().nutrition(8).saturationModifier(1.2F).alwaysEdible().build()).rarity(Rarity.UNCOMMON));
	public static final Item CLOUD_IN_A_BOTTLE = register("cloud_in_a_bottle", CloudBottleItem::new, props().stacksTo(16));
	public static final Item FEATHER_OF_RETURN = register("feather_of_return", FeatherOfReturnItem::new, props()
			.stacksTo(16).rarity(Rarity.RARE));
	public static final Item GOLDEN_FEATHER = register("golden_feather", Item::new, props().rarity(Rarity.UNCOMMON));

	// -------------------------------------------------------------------- Hell
	public static final Item SOUL_SHARD = register("soul_shard", Item::new, props().rarity(Rarity.UNCOMMON));
	public static final Item INFERNAL_PICKAXE = register("infernal_pickaxe", Item::new, props()
			.pickaxe(ToolMaterial.NETHERITE, 1.0F, -2.8F).fireResistant().rarity(Rarity.RARE));
	public static final Item HELLFIRE_SWORD = register("hellfire_sword", Item::new, props()
			.sword(ToolMaterial.NETHERITE, 3.0F, -2.4F).fireResistant().rarity(Rarity.RARE));
	public static final Item DEMON_WINGS = register("demon_wings", Item::new, wings("demon_wings").fireResistant().rarity(Rarity.EPIC));
	public static final Item INFERNAL_CROWN = register("infernal_crown", Item::new, headwear().fireResistant().rarity(Rarity.EPIC));
	public static final Item MORNINGSTAR = register("morningstar", Item::new, props()
			.sword(ToolMaterial.NETHERITE, 6.0F, -2.3F).fireResistant().rarity(Rarity.EPIC));
	public static final Item FALLEN_HALO = register("fallen_halo", Item::new, headwear().fireResistant().rarity(Rarity.EPIC));
	public static final Item PALACE_KEY = register("palace_key", PalaceKeyItem::new, props().stacksTo(1).fireResistant().rarity(Rarity.RARE));

	// ------------------------------------------------------------------ Shared
	public static final Item BOOK_OF_DEEDS = register("book_of_deeds", BookOfDeedsItem::new, props().stacksTo(1).rarity(Rarity.UNCOMMON));

	// --------------------------------------------- pictures used in dialogs
	public static final String[] VISIONS = {
			"vision_lava_cow", "vision_zombie_ambush", "vision_trapped_wolf", "vision_hungry_traveler", "vision_lost_satchel",
			"vision_devils_bargain", "vision_heaven", "vision_hell", "vision_lucifer", "vision_gatekeeper", "vision_scales"
	};

	static {
		for (String vision : VISIONS) {
			registerHidden(vision, Item::new, props().stacksTo(1));
		}
	}

	private static Item.Properties props() {
		return new Item.Properties();
	}

	private static Item.Properties wings(String asset) {
		return props().durability(864)
				.component(DataComponents.GLIDER, Unit.INSTANCE)
				.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
						.setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
						.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, HeavenHell.id(asset)))
						.setDamageOnHurt(false)
						.build());
	}

	private static Item.Properties headwear() {
		return props().stacksTo(1)
				.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
						.setEquipSound(SoundEvents.ARMOR_EQUIP_GOLD)
						.build());
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		Item item = registerHidden(name, factory, properties);
		TAB.add(item);
		return item;
	}

	private static Item registerHidden(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, HeavenHell.id(name));
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
	}
}
