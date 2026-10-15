package com.timeywimey.poolrooms.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import com.timeywimey.poolrooms.entity.LifeguardEntity;
import com.timeywimey.poolrooms.registry.ModBlocks;
import com.timeywimey.poolrooms.registry.ModItems;
import com.timeywimey.poolrooms.registry.ModSounds;
import com.timeywimey.poolrooms.world.Pool;

/** Everyday rules: snorkel and flippers, no fall damage in the Poolrooms, the pool noodle, and walking out the EXIT. */
public final class PoolLife {
	private PoolLife() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(PoolLife::tick);
		// nothing in the Poolrooms can hurt you by falling (slides, towers, diving boards...)
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer player && Pool.isPoolrooms(player.level()) && source.is(DamageTypes.FALL)));
		AttackEntityCallback.EVENT.register(PoolLife::noodle);
	}

	private static void tick(MinecraftServer server) {
		int t = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (t % 10 == 0) {
				gear(player);
			}
			if (t % 2 == 0 && Pool.isPoolrooms(player.level()) && player.isAlive() && insideExit(player)) {
				PoolTravel.exitHome(player, true);
			}
		}
	}

	/** Snorkel Mask: breathe and see underwater. Flippers: swim fast. Only while you are in water. */
	private static void gear(ServerPlayer player) {
		if (!player.isInWater()) {
			return;
		}
		if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SNORKEL_MASK)) {
			player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 60, 0, true, false, true));
		}
		if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.FLIPPERS)) {
			player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
		}
	}

	private static boolean insideExit(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos feet = player.blockPosition();
		BlockPos eyes = BlockPos.containing(player.getEyePosition());
		return level.getBlockState(feet).is(ModBlocks.EXIT_LIGHT) || level.getBlockState(eyes).is(ModBlocks.EXIT_LIGHT);
	}

	/** The Pool Noodle: no damage at all, just a big bouncy shove (it even moves the Lifeguard). */
	private static InteractionResult noodle(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hit) {
		if (player.isSpectator() || !player.getItemInHand(hand).is(ModItems.POOL_NOODLE)) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.PASS; // let the client send the hit to the server
		}
		Vec3 away = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
		away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle().multiply(1.0, 0.0, 1.0) : away;
		away = away.normalize();
		// a big bouncy shove: about 5 or 6 blocks for the Lifeguard, a bit more for lighter things
		boolean isGuard = target instanceof LifeguardEntity;
		double strength = isGuard ? 1.1 : 1.3;
		target.push(away.x * strength, isGuard ? 0.3 : 0.35, away.z * strength);
		target.needsSync = true;
		if (target instanceof LifeguardEntity guard) {
			guard.bonk();
		}
		server.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.NOODLE_BOING, SoundSource.PLAYERS, 0.9F,
				0.9F + server.getRandom().nextFloat() * 0.3F);
		server.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 12,
				0.3, 0.3, 0.3, 0.1);
		return InteractionResult.SUCCESS;
	}
}
