package com.timeywimey.heavenhell.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.timeywimey.heavenhell.util.Cmd;

/** Harp of Peace: heals nearby players and calms monsters. */
public class HarpItem extends Item {
	public HarpItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			for (Player p : server.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8.0), Player::isAlive)) {
				p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
			}
			for (Mob mob : server.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10.0), m -> m instanceof Monster)) {
				mob.setTarget(null);
				mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2));
				mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
			}
			Cmd.particle(server, "minecraft:note", player.getX(), player.getY() + 2.0, player.getZ(), 1.2, 0.6, 1.2, 1.0, 14);
			float pitch = 0.6F + level.getRandom().nextFloat() * 1.2F;
			Cmd.soundAt(server, player.getX(), player.getY(), player.getZ(), "minecraft:block.note_block.harp", 1.0F, pitch);
			Cmd.soundAt(server, player.getX(), player.getY(), player.getZ(), "minecraft:block.amethyst_block.chime", 1.0F, pitch);
			player.getCooldowns().addCooldown(stack, 60);
		}
		return InteractionResult.SUCCESS;
	}
}
