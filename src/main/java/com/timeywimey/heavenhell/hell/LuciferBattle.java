package com.timeywimey.heavenhell.hell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.timeywimey.heavenhell.HeavenHell;
import com.timeywimey.heavenhell.entity.ImpEntity;
import com.timeywimey.heavenhell.entity.LuciferEntity;
import com.timeywimey.heavenhell.registry.ModBlocks;
import com.timeywimey.heavenhell.registry.ModEntities;
import com.timeywimey.heavenhell.registry.ModItems;
import com.timeywimey.heavenhell.soul.Soul;
import com.timeywimey.heavenhell.soul.SoulData;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Scheduler;
import com.timeywimey.heavenhell.util.Txt;
import com.timeywimey.heavenhell.world.Layout;
import com.timeywimey.heavenhell.world.Realms;
import com.timeywimey.heavenhell.world.WorldState;

/** Lucifer on his throne, and the battle for a soul's freedom. */
public final class LuciferBattle {
	private LuciferBattle() {
	}

	private static final String BAR = "heavenhell:lucifer";
	private static final String SEAT_TAG = "heavenhell_throne_seat";
	private static final RandomSource RANDOM = RandomSource.create();

	/** The fight in progress, if any. */
	private static LuciferEntity fighting;
	private static UUID challenger;
	private static final Set<UUID> participants = new HashSet<>();
	private static int phase;
	private static int age;
	private static int fireballCooldown;
	private static int specialCooldown;
	private static final List<Vec3> pillars = new ArrayList<>();
	private static int pillarTimer;
	private static int tauntCooldown;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(LuciferBattle::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Cmd.run(server, "bossbar add " + BAR + " " + Txt.of("Lucifer, the Fallen Morningstar", "dark_red").bold().str());
			Cmd.run(server, "bossbar set " + BAR + " color red");
			Cmd.run(server, "bossbar set " + BAR + " style notched_10");
			Cmd.run(server, "bossbar set " + BAR + " visible false");
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (fighting != null) {
				fighting.setHealth(fighting.getMaxHealth());
				fighting.setInBattle(false);
			}
			fighting = null;
			challenger = null;
			participants.clear();
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof LuciferEntity lucifer && !lucifer.isInBattle()) {
				if (source.getEntity() instanceof ServerPlayer player && tauntCooldown <= 0) {
					tauntCooldown = 60;
					speak(player.level(), "You dare raise a hand against me on my own throne? Earn the right first.");
				}
				return false;
			}
			if (entity instanceof LuciferEntity && source.getEntity() instanceof ServerPlayer player) {
				participants.add(player.getUUID());
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof LuciferEntity lucifer) {
				onDefeat(lucifer);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			Entity attacker = source.getEntity();
			if (!(attacker instanceof LivingEntity living) || blocked) {
				return;
			}
			ItemStack weapon = living.getItemBySlot(EquipmentSlot.MAINHAND);
			if (weapon.is(ModItems.HELLFIRE_SWORD)) {
				entity.igniteForSeconds(4.0F);
			} else if (weapon.is(ModItems.MORNINGSTAR)) {
				entity.igniteForSeconds(6.0F);
				entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
			} else if (weapon.is(ModItems.SERAPH_BLADE)) {
				if (entity instanceof ImpEntity || entity instanceof LuciferEntity || entity.typeHolder().is(EntityTypeTags.UNDEAD)) {
					entity.igniteForSeconds(5.0F);
				}
				living.heal(1.0F);
			}
			if (attacker instanceof ImpEntity) {
				entity.igniteForSeconds(2.0F);
			}
		});
	}

	public static boolean canStart(ServerLevel level) {
		return fighting == null && findLucifer(level) != null;
	}

	public static boolean isFighting() {
		return fighting != null;
	}

	// ---------------------------------------------------------- presence

	public static LuciferEntity findLucifer(ServerLevel hell) {
		AABB area = new AABB(Layout.THRONE_SEAT.add(-40, -20, -40), Layout.THRONE_SEAT.add(40, 30, 40));
		List<LuciferEntity> found = hell.getEntitiesOfClass(LuciferEntity.class, area, LivingEntity::isAlive);
		return found.isEmpty() ? null : found.get(0);
	}

	/** Puts Lucifer back on his throne if he is missing (and his respawn time has come). */
	private static void ensureLucifer(MinecraftServer server) {
		ServerLevel hell = Realms.hell(server);
		WorldState state = WorldState.get(server);
		if (hell == null || !state.hellBuilt || hell.players().isEmpty()) {
			return;
		}
		BlockPos seatPos = BlockPos.containing(Layout.THRONE_SEAT);
		if (!hell.isLoaded(seatPos)) {
			return;
		}
		boolean someoneNear = hell.players().stream().anyMatch(p -> p.position().distanceTo(Layout.THRONE_SEAT) < 80.0);
		if (!someoneNear) {
			return;
		}
		LuciferEntity lucifer = findLucifer(hell);
		if (lucifer == null) {
			if (hell.getGameTime() < state.luciferRespawnAt) {
				return;
			}
			lucifer = spawnOnThrone(hell);
			if (lucifer != null && state.luciferDefeats > 0) {
				speak(hell, "Did you think I could be destroyed? I am eternal. The throne is mine again.");
			}
		} else if (fighting == null && !lucifer.isPassenger()) {
			seat(hell, lucifer);
		}
	}

	/** Creates a new Lucifer seated on his throne. */
	public static LuciferEntity spawnOnThrone(ServerLevel hell) {
		LuciferEntity lucifer = ModEntities.LUCIFER.create(hell, EntitySpawnReason.TRIGGERED);
		if (lucifer == null) {
			return null;
		}
		lucifer.snapTo(Layout.THRONE_SEAT);
		lucifer.setPersistenceRequired();
		hell.addFreshEntity(lucifer);
		WorldState state = WorldState.get(hell.getServer());
		state.luciferAlive = true;
		state.changed();
		seat(hell, lucifer);
		return lucifer;
	}

	/** Lucifer sits by riding an invisible marker on the throne. */
	private static void seat(ServerLevel hell, LuciferEntity lucifer) {
		lucifer.setInBattle(false);
		lucifer.setTarget(null);
		lucifer.setHealth(lucifer.getMaxHealth());
		lucifer.clearFire();
		String uuid = lucifer.getStringUUID();
		Vec3 s = Layout.THRONE_SEAT;
		Cmd.in(hell, "kill @e[type=minecraft:armor_stand,tag=" + SEAT_TAG + "]");
		Cmd.in(hell, "summon minecraft:armor_stand " + Cmd.fmt(s.x) + " " + Cmd.fmt(s.y) + " " + Cmd.fmt(s.z)
				+ " {Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b,Tags:[\"" + SEAT_TAG + "\"],Rotation:[" + Layout.THRONE_YAW + "f,0f]}");
		Cmd.in(hell, "tp " + uuid + " " + Cmd.fmt(s.x) + " " + Cmd.fmt(s.y) + " " + Cmd.fmt(s.z) + " " + Layout.THRONE_YAW + " 0");
		Cmd.in(hell, "ride " + uuid + " mount @e[type=minecraft:armor_stand,tag=" + SEAT_TAG + ",limit=1]");
		Cmd.run(hell.getServer(), "bossbar set " + BAR + " visible false");
	}

	// ------------------------------------------------------------- battle

	public static void challenge(ServerPlayer player) {
		SoulData soul = Soul.get(player);
		if (!HellLife.nearThrone(player)) {
			Cmd.actionbar(player, Txt.of("Lucifer is on his throne. Go to him.", "red").str());
			return;
		}
		if (soul.rank < HellRanks.CHALLENGE_RANK) {
			Cmd.actionbar(player, Txt.of("Only a Demon or higher may challenge the throne.", "red").str());
			return;
		}
		if (fighting != null) {
			Cmd.actionbar(player, Txt.of("Lucifer is already fighting someone.", "red").str());
			return;
		}
		ServerLevel hell = player.level();
		LuciferEntity lucifer = findLucifer(hell);
		if (lucifer == null) {
			Cmd.actionbar(player, Txt.of("The throne is empty... for now.", "gray").str());
			return;
		}
		fighting = lucifer;
		challenger = player.getUUID();
		participants.clear();
		participants.add(player.getUUID());
		phase = 1;
		age = 0;
		fireballCooldown = 80;
		specialCooldown = 160;
		pillars.clear();
		speak(hell, "So. The little " + HellRanks.get(soul.rank).name().toLowerCase() + " wants my crown.");
		Scheduler.after(40, () -> speak(hell, "Very well. Let us see what your soul is worth!"));
		Scheduler.after(60, () -> {
			if (fighting != lucifer || !lucifer.isAlive()) {
				return;
			}
			Cmd.in(hell, "ride " + lucifer.getStringUUID() + " dismount");
			Cmd.in(hell, "kill @e[type=minecraft:armor_stand,tag=" + SEAT_TAG + "]");
			lucifer.setInBattle(true);
			lucifer.snapTo(Layout.THRONE_SEAT.add(0.0, -1.5, 4.0));
			ServerPlayer target = hell.getServer().getPlayerList().getPlayer(challenger);
			if (target != null) {
				lucifer.setTarget(target);
			}
			Cmd.soundAt(hell, lucifer.getX(), lucifer.getY(), lucifer.getZ(), "minecraft:entity.ender_dragon.growl", 2.0F, 0.6F);
			Cmd.particle(hell, "minecraft:flame", lucifer.getX(), lucifer.getY() + 1.5, lucifer.getZ(), 1.5, 1.5, 1.5, 0.1, 150);
			Cmd.run(hell.getServer(), "bossbar set " + BAR + " max " + (int) lucifer.getMaxHealth());
			Cmd.run(hell.getServer(), "bossbar set " + BAR + " value " + (int) lucifer.getHealth());
			Cmd.run(hell.getServer(), "bossbar set " + BAR + " visible true");
			for (ServerPlayer p : hell.players()) {
				if (p.position().distanceTo(Layout.ARENA_CENTER) < 48.0) {
					Cmd.title(p, Txt.of("LUCIFER", "dark_red").bold().str(), Txt.of("The Fallen Morningstar", "red").str(), 10, 50, 20);
				}
			}
		});
	}

	private static void tick(MinecraftServer server) {
		if (tauntCooldown > 0) {
			tauntCooldown--;
		}
		if (server.getTickCount() % 100 == 11) {
			ensureLucifer(server);
		}
		if (fighting == null) {
			return;
		}
		LuciferEntity lucifer = fighting;
		if (lucifer.isRemoved()) {
			endBattle(false);
			return;
		}
		if (lucifer.isDeadOrDying()) {
			return; // onDefeat handles it
		}
		if (!lucifer.isInBattle()) {
			return; // still rising from the throne
		}
		ServerLevel hell = (ServerLevel) lucifer.level();
		ServerPlayer target = server.getPlayerList().getPlayer(challenger);
		if (target == null || !target.isAlive() || target.level() != hell || target.position().distanceTo(Layout.ARENA_CENTER) > 40.0) {
			// challenger fled or fell: find someone else who joined the fight, otherwise Lucifer wins
			target = null;
			for (UUID id : participants) {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null && p.isAlive() && p.level() == hell && p.position().distanceTo(Layout.ARENA_CENTER) < 40.0) {
					target = p;
					challenger = id;
					break;
				}
			}
			if (target == null) {
				speak(hell, "Pathetic. Crawl back to the gates, little soul.");
				endBattle(false);
				return;
			}
		}
		age++;
		lucifer.setTarget(target);
		if (age % 10 == 0) {
			Cmd.run(server, "bossbar set " + BAR + " value " + Math.max(0, (int) lucifer.getHealth()));
			Cmd.in(hell, "execute positioned " + Cmd.fmt(Layout.ARENA_CENTER.x) + " " + Cmd.fmt(Layout.ARENA_CENTER.y) + " "
					+ Cmd.fmt(Layout.ARENA_CENTER.z) + " run bossbar set " + BAR + " players @a[distance=..64]");
		}
		// keep him in his hall
		if (!Layout.ARENA.inflate(2.0).contains(lucifer.position())) {
			lucifer.snapTo(Layout.ARENA_CENTER);
		}
		float hp = lucifer.getHealth() / lucifer.getMaxHealth();
		if (phase == 1 && hp < 0.66F) {
			phase = 2;
			speak(hell, "You think this is pain? Rise, my servants!");
			summonImps(hell, lucifer, 3);
			Cmd.soundAt(hell, lucifer.getX(), lucifer.getY(), lucifer.getZ(), "minecraft:entity.wither.ambient", 2.0F, 0.5F);
		} else if (phase == 2 && hp < 0.33F) {
			phase = 3;
			speak(hell, "ENOUGH! I was the brightest star in Heaven. I will not be beaten by YOU!");
			lucifer.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 600, 0));
			lucifer.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 600, 0));
			hellfireRing(hell, lucifer);
			Cmd.soundAt(hell, lucifer.getX(), lucifer.getY(), lucifer.getZ(), "minecraft:entity.ender_dragon.growl", 2.0F, 0.4F);
		}
		if (--fireballCooldown <= 0) {
			int volley = phase == 3 ? 5 : phase == 2 ? 3 : 2;
			for (int i = 0; i < volley; i++) {
				int delay = i * 6;
				ServerPlayer t = target;
				Scheduler.after(delay + 1, () -> shootFireball(hell, lucifer, t));
			}
			fireballCooldown = phase == 3 ? 50 : 70;
		}
		if (--specialCooldown <= 0) {
			int roll = RANDOM.nextInt(phase >= 2 ? 3 : 1);
			if (roll == 0) {
				flamePillars(target);
			} else if (roll == 1) {
				teleportBehind(hell, lucifer, target);
			} else {
				summonImps(hell, lucifer, 2);
			}
			specialCooldown = phase == 3 ? 100 : 140;
		}
		tickPillars(hell, lucifer);
	}

	private static void shootFireball(ServerLevel hell, LuciferEntity lucifer, ServerPlayer target) {
		if (fighting != lucifer || !lucifer.isAlive() || target == null) {
			return;
		}
		Vec3 eye = lucifer.getEyePosition();
		Vec3 dir = target.getEyePosition().subtract(eye).normalize();
		dir = dir.add((RANDOM.nextDouble() - 0.5) * 0.12, (RANDOM.nextDouble() - 0.5) * 0.08, (RANDOM.nextDouble() - 0.5) * 0.12);
		SmallFireball fireball = new SmallFireball(hell, lucifer, dir.normalize());
		fireball.setPos(eye.x + dir.x, eye.y + dir.y - 0.3, eye.z + dir.z);
		hell.addFreshEntity(fireball);
		Cmd.soundAt(hell, eye.x, eye.y, eye.z, "minecraft:entity.blaze.shoot", 1.0F, 0.7F);
	}

	private static void flamePillars(ServerPlayer target) {
		pillars.clear();
		Vec3 base = target.position();
		pillars.add(base);
		for (int i = 0; i < 3; i++) {
			pillars.add(base.add((RANDOM.nextDouble() - 0.5) * 8.0, 0.0, (RANDOM.nextDouble() - 0.5) * 8.0));
		}
		pillarTimer = 30;
	}

	private static void tickPillars(ServerLevel hell, LuciferEntity lucifer) {
		if (pillars.isEmpty()) {
			return;
		}
		pillarTimer--;
		if (pillarTimer > 0) {
			if (pillarTimer % 4 == 0) {
				for (Vec3 p : pillars) {
					Cmd.particle(hell, "minecraft:flame", p.x, p.y + 0.1, p.z, 0.5, 0.0, 0.5, 0.01, 12);
				}
			}
			return;
		}
		for (Vec3 p : pillars) {
			Cmd.particle(hell, "minecraft:flame", p.x, p.y + 2.5, p.z, 0.3, 2.5, 0.3, 0.05, 120);
			Cmd.particle(hell, "minecraft:lava", p.x, p.y + 0.5, p.z, 0.3, 0.3, 0.3, 0.5, 12);
			Cmd.soundAt(hell, p.x, p.y, p.z, "minecraft:entity.blaze.shoot", 1.5F, 0.5F);
			AABB box = new AABB(p.x - 1.3, p.y - 0.5, p.z - 1.3, p.x + 1.3, p.y + 4.0, p.z + 1.3);
			for (Player victim : hell.getEntitiesOfClass(Player.class, box, LivingEntity::isAlive)) {
				victim.hurtServer(hell, hell.damageSources().mobAttack(lucifer), 7.0F);
				victim.igniteForSeconds(4.0F);
			}
		}
		pillars.clear();
	}

	private static void teleportBehind(ServerLevel hell, LuciferEntity lucifer, ServerPlayer target) {
		Vec3 look = target.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
		Vec3 dest = target.position().subtract(look.scale(2.5));
		if (!Layout.ARENA.contains(dest)) {
			return;
		}
		Cmd.particle(hell, "minecraft:large_smoke", lucifer.getX(), lucifer.getY() + 1.5, lucifer.getZ(), 0.5, 1.0, 0.5, 0.02, 40);
		lucifer.snapTo(dest);
		Cmd.particle(hell, "minecraft:flame", dest.x, dest.y + 1.5, dest.z, 0.5, 1.0, 0.5, 0.05, 40);
		Cmd.soundAt(hell, dest.x, dest.y, dest.z, "minecraft:entity.enderman.teleport", 1.5F, 0.5F);
		if (RANDOM.nextBoolean()) {
			speak(hell, "Behind you.");
		}
	}

	private static void summonImps(ServerLevel hell, LuciferEntity lucifer, int count) {
		for (int i = 0; i < count; i++) {
			ImpEntity imp = ModEntities.IMP.create(hell, EntitySpawnReason.MOB_SUMMONED);
			if (imp == null) {
				continue;
			}
			double a = RANDOM.nextDouble() * Math.PI * 2.0;
			Vec3 pos = lucifer.position().add(Math.cos(a) * 3.0, 0.0, Math.sin(a) * 3.0);
			imp.snapTo(pos);
			CourtGuard.markSummoned(imp);
			hell.addFreshEntity(imp);
			Cmd.particle(hell, "minecraft:flame", pos.x, pos.y + 0.5, pos.z, 0.3, 0.5, 0.3, 0.05, 20);
		}
	}

	private static void hellfireRing(ServerLevel hell, LuciferEntity lucifer) {
		BlockPos center = lucifer.blockPosition();
		for (int i = 0; i < 32; i++) {
			double a = i / 32.0 * Math.PI * 2.0;
			BlockPos p = center.offset((int) Math.round(Math.cos(a) * 6.0), 0, (int) Math.round(Math.sin(a) * 6.0));
			if (hell.getBlockState(p).isAir() && !hell.getBlockState(p.below()).isAir()) {
				hell.setBlock(p, Blocks.FIRE.defaultBlockState(), 3);
			}
		}
	}

	private static void endBattle(boolean won) {
		LuciferEntity lucifer = fighting;
		fighting = null;
		challenger = null;
		participants.clear();
		pillars.clear();
		if (lucifer != null && lucifer.isAlive()) {
			ServerLevel hell = (ServerLevel) lucifer.level();
			Cmd.run(hell.getServer(), "bossbar set " + BAR + " visible false");
			seat(hell, lucifer);
		}
	}

	private static void onDefeat(LuciferEntity lucifer) {
		ServerLevel hell = (ServerLevel) lucifer.level();
		MinecraftServer server = hell.getServer();
		Cmd.run(server, "bossbar set " + BAR + " visible false");
		Set<UUID> winners = new HashSet<>(participants);
		if (challenger != null) {
			winners.add(challenger);
		}
		for (ServerPlayer p : hell.players()) {
			if (p.position().distanceTo(Layout.ARENA_CENTER) < 40.0) {
				winners.add(p.getUUID());
			}
		}
		fighting = null;
		challenger = null;
		participants.clear();
		pillars.clear();
		WorldState state = WorldState.get(server);
		state.luciferAlive = false;
		state.luciferRespawnAt = hell.getGameTime() + 20L * 60L * 3L;
		state.luciferDefeats++;
		state.gateOpen = true;
		state.changed();
		speak(hell, "...Impossible.");
		Scheduler.after(40, () -> speak(hell, "Go, then. Climb back toward the light... if it will still have you."));
		Cmd.soundAt(hell, lucifer.getX(), lucifer.getY(), lucifer.getZ(), "minecraft:entity.wither.death", 2.0F, 0.6F);
		openGate(hell);
		for (UUID id : winners) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) {
				continue;
			}
			SoulData soul = Soul.get(p);
			soul.freed = true;
			soul.remember("+0  Defeated Lucifer, the Fallen Morningstar");
			Soul.save(p, soul);
			Cmd.title(p, Txt.of("LUCIFER HAS FALLEN", "gold").bold().str(),
					Txt.of("The Redemption Gate behind the throne is open", "yellow").str(), 10, 100, 30);
			Cmd.sound(p, "minecraft:ui.toast.challenge_complete", 1.0F, 1.0F);
		}
		HeavenHell.LOGGER.info("Lucifer was defeated ({} total)", state.luciferDefeats);
	}

	public static void openGate(ServerLevel hell) {
		BlockPos a = Layout.REDEMPTION_GATE_MIN;
		BlockPos b = Layout.REDEMPTION_GATE_MAX;
		for (BlockPos p : BlockPos.betweenClosed(a, b)) {
			hell.setBlock(p, ModBlocks.REDEMPTION_LIGHT.defaultBlockState(), 3);
		}
		Vec3 c = Vec3.atCenterOf(a).add(Vec3.atCenterOf(b)).scale(0.5);
		Cmd.particle(hell, "minecraft:end_rod", c.x, c.y, c.z, 2.0, 3.0, 0.5, 0.05, 200);
		Cmd.soundAt(hell, c.x, c.y, c.z, "minecraft:block.end_portal.spawn", 2.0F, 1.2F);
	}

	static void speak(ServerLevel level, String line) {
		Component msg = Component.literal("<Lucifer> ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
				.append(Component.literal(line).withStyle(ChatFormatting.RED));
		for (ServerPlayer p : level.players()) {
			if (p.position().distanceTo(Layout.THRONE_SEAT) < 96.0) {
				p.sendSystemMessage(msg);
			}
		}
	}
}
