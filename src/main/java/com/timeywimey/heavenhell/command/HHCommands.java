package com.timeywimey.heavenhell.command;

import java.util.Arrays;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.timeywimey.heavenhell.dialog.Dialogs;
import com.timeywimey.heavenhell.heaven.HeavenLife;
import com.timeywimey.heavenhell.hell.HellLife;
import com.timeywimey.heavenhell.hell.HellRanks;
import com.timeywimey.heavenhell.hell.LuciferBattle;
import com.timeywimey.heavenhell.moment.MomentType;
import com.timeywimey.heavenhell.moment.Moments;
import com.timeywimey.heavenhell.registry.ModBlocks;
import com.timeywimey.heavenhell.registry.ModItems;
import com.timeywimey.heavenhell.soul.Judgment;
import com.timeywimey.heavenhell.soul.Karma;
import com.timeywimey.heavenhell.soul.Soul;
import com.timeywimey.heavenhell.soul.SoulData;
import com.timeywimey.heavenhell.world.Realms;
import com.timeywimey.heavenhell.world.WorldState;

/** {@code /heavenhell ...} - player actions (used by dialog buttons) and creator/admin tools. */
public final class HHCommands {
	private HHCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static boolean isAdmin(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("heavenhell")
				.executes(HHCommands::help)
				.then(Commands.literal("help").executes(HHCommands::help))
				.then(Commands.literal("choose")
						.then(Commands.argument("token", StringArgumentType.word())
								.then(Commands.argument("choice", StringArgumentType.word())
										.executes(HHCommands::choose))))
				.then(Commands.literal("action")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(HHCommands::action)))
				.then(Commands.literal("deeds").executes(ctx -> {
					Dialogs.deeds(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("karma")
						.executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							SoulData soul = Soul.get(p);
							ctx.getSource().sendSuccess(() -> Component.literal("Karma: " + soul.karma + " (" + Karma.tier(soul.karma)
									+ ") - if you died now you would go to " + Karma.fate(soul.karma) + ".").withStyle(ChatFormatting.GOLD), false);
							return soul.karma;
						})
						.then(Commands.literal("set").requires(HHCommands::isAdmin)
								.then(Commands.argument("targets", EntityArgument.players())
										.then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000))
												.executes(ctx -> setKarma(ctx, false)))))
						.then(Commands.literal("add").requires(HHCommands::isAdmin)
								.then(Commands.argument("targets", EntityArgument.players())
										.then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000))
												.executes(ctx -> setKarma(ctx, true))))))
				.then(Commands.literal("moment").requires(HHCommands::isAdmin)
						.then(Commands.argument("type", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										Arrays.stream(MomentType.values()).map(t -> t.id).toList(), builder))
								.executes(ctx -> moment(ctx, java.util.List.of(ctx.getSource().getPlayerOrException())))
								.then(Commands.argument("targets", EntityArgument.players())
										.executes(ctx -> moment(ctx, EntityArgument.getPlayers(ctx, "targets"))))))
				.then(Commands.literal("send").requires(HHCommands::isAdmin)
						.then(Commands.argument("targets", EntityArgument.players())
								.then(Commands.literal("heaven").executes(ctx -> send(ctx, SoulData.HEAVEN)))
								.then(Commands.literal("hell").executes(ctx -> send(ctx, SoulData.HELL)))
								.then(Commands.literal("living").executes(ctx -> send(ctx, SoulData.LIVING)))))
				.then(Commands.literal("rank").requires(HHCommands::isAdmin)
						.then(Commands.argument("targets", EntityArgument.players())
								.then(Commands.argument("rank", IntegerArgumentType.integer(0, HellRanks.MAX))
										.executes(HHCommands::setRank))))
				.then(Commands.literal("free").requires(HHCommands::isAdmin)
						.then(Commands.argument("targets", EntityArgument.players())
								.executes(ctx -> {
									for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) {
										SoulData soul = Soul.get(p);
										soul.freed = true;
										Soul.save(p, soul);
									}
									ServerLevel hell = Realms.hell(ctx.getSource().getServer());
									if (hell != null && WorldState.get(ctx.getSource().getServer()).hellBuilt) {
										WorldState state = WorldState.get(ctx.getSource().getServer());
										state.gateOpen = true;
										state.changed();
										LuciferBattle.openGate(hell);
									}
									ctx.getSource().sendSuccess(() -> Component.literal("The Redemption Gate is open to them."), true);
									return 1;
								})))
				.then(Commands.literal("moments").requires(HHCommands::isAdmin)
						.then(Commands.literal("on").executes(ctx -> toggleMoments(ctx, true)))
						.then(Commands.literal("off").executes(ctx -> toggleMoments(ctx, false)))
						.then(Commands.literal("every")
								.then(Commands.argument("min_minutes", IntegerArgumentType.integer(1, 600))
										.then(Commands.argument("max_minutes", IntegerArgumentType.integer(1, 600))
												.executes(HHCommands::momentInterval)))))
				.then(Commands.literal("judgment").requires(HHCommands::isAdmin)
						.then(Commands.literal("on").executes(ctx -> toggleJudgment(ctx, true)))
						.then(Commands.literal("off").executes(ctx -> toggleJudgment(ctx, false))))
				.then(Commands.literal("kit").requires(HHCommands::isAdmin)
						.then(Commands.literal("heaven").executes(ctx -> kit(ctx, true)))
						.then(Commands.literal("hell").executes(ctx -> kit(ctx, false))))
				.then(Commands.literal("rebuild").requires(HHCommands::isAdmin)
						.then(Commands.literal("heaven").executes(ctx -> rebuild(ctx, true)))
						.then(Commands.literal("hell").executes(ctx -> rebuild(ctx, false)))));
	}

	private static int help(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack s = ctx.getSource();
		s.sendSuccess(() -> Component.literal("Heaven & Hell").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		s.sendSuccess(() -> Component.literal("/heavenhell karma - your karma and where you'd go if you died").withStyle(ChatFormatting.YELLOW), false);
		s.sendSuccess(() -> Component.literal("/heavenhell deeds - open your Book of Deeds").withStyle(ChatFormatting.YELLOW), false);
		if (isAdmin(s)) {
			s.sendSuccess(() -> Component.literal("Creator tools (cheats):").withStyle(ChatFormatting.AQUA), false);
			s.sendSuccess(() -> Component.literal(" /heavenhell moment <lava_animal|zombie_ambush|trapped_wolf|hungry_traveler|lost_satchel|devils_bargain> [player]"), false);
			s.sendSuccess(() -> Component.literal(" /heavenhell send <player> heaven|hell|living"), false);
			s.sendSuccess(() -> Component.literal(" /heavenhell karma set|add <player> <value>"), false);
			s.sendSuccess(() -> Component.literal(" /heavenhell rank <player> <0-5>   /heavenhell free <player>"), false);
			s.sendSuccess(() -> Component.literal(" /heavenhell kit heaven|hell   /heavenhell moments on|off|every <min> <max>"), false);
			s.sendSuccess(() -> Component.literal(" /heavenhell judgment on|off   /heavenhell rebuild heaven|hell"), false);
		}
		return 1;
	}

	private static int choose(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String token = StringArgumentType.getString(ctx, "token");
		boolean good = "good".equals(StringArgumentType.getString(ctx, "choice"));
		if (!Moments.choose(player, token, good)) {
			ctx.getSource().sendFailure(Component.literal("That moment has already passed."));
			return 0;
		}
		return 1;
	}

	private static int action(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		switch (StringArgumentType.getString(ctx, "name")) {
			case "stay" -> HeavenLife.stay(player);
			case "return_confirm" -> {
				if (Realms.isHeaven(player.level())) {
					Dialogs.returnConfirm(player);
				}
			}
			case "return" -> HeavenLife.returnHome(player);
			case "heaven_info" -> Dialogs.heavenInfo(player);
			case "blessing" -> HeavenLife.blessing(player);
			case "rankup" -> HellLife.rankUp(player);
			case "quarters" -> HellLife.quarters(player);
			case "challenge_confirm" -> {
				if (HellLife.nearThrone(player) && Soul.get(player).rank >= HellRanks.CHALLENGE_RANK) {
					Dialogs.challengeConfirm(player);
				}
			}
			case "challenge" -> LuciferBattle.challenge(player);
			default -> {
				ctx.getSource().sendFailure(Component.literal("Unknown action."));
				return 0;
			}
		}
		return 1;
	}

	private static int setKarma(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
		int value = IntegerArgumentType.getInteger(ctx, "value");
		Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
		for (ServerPlayer p : players) {
			if (add) {
				Karma.add(p, value, value >= 0 ? "A blessing from above" : "A curse from below");
			} else {
				SoulData soul = Soul.get(p);
				soul.karma = value;
				Soul.save(p, soul);
			}
		}
		ctx.getSource().sendSuccess(() -> Component.literal((add ? "Added " : "Set ") + value + " karma for " + players.size() + " player(s)."), true);
		return players.size();
	}

	private static int moment(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
		MomentType type = MomentType.byId(StringArgumentType.getString(ctx, "type"));
		if (type == null) {
			ctx.getSource().sendFailure(Component.literal("Unknown moment. Try: lava_animal, zombie_ambush, trapped_wolf, hungry_traveler, lost_satchel, devils_bargain"));
			return 0;
		}
		int started = 0;
		for (ServerPlayer p : players) {
			if (Moments.start(p, type)) {
				started++;
			}
		}
		if (started == 0) {
			ctx.getSource().sendFailure(Component.literal("No room to stage that moment here - stand on open, flat ground and look at an empty spot."));
		}
		return started;
	}

	private static int send(CommandContext<CommandSourceStack> ctx, String realm) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
		for (ServerPlayer p : players) {
			if (SoulData.LIVING.equals(realm)) {
				HellLife.leaveHell(p);
				Realms.returnToLiving(p);
			} else {
				SoulData soul = Soul.get(p);
				if (SoulData.LIVING.equals(soul.realm)) {
					Realms.rememberReturnPoint(p);
				}
				Judgment.deliver(p, realm, false);
			}
		}
		return players.size();
	}

	private static int setRank(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		int rank = IntegerArgumentType.getInteger(ctx, "rank");
		for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) {
			SoulData soul = Soul.get(p);
			soul.rank = rank;
			Soul.save(p, soul);
			HellLife.leaveHell(p);
			HellLife.applyRankTeam(p);
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Rank set to " + HellRanks.get(rank).name() + "."), true);
		return 1;
	}

	private static int toggleMoments(CommandContext<CommandSourceStack> ctx, boolean on) {
		WorldState state = WorldState.get(ctx.getSource().getServer());
		state.momentsEnabled = on;
		state.changed();
		ctx.getSource().sendSuccess(() -> Component.literal("Moments of choice are now " + (on ? "on" : "off") + "."), true);
		return 1;
	}

	private static int momentInterval(CommandContext<CommandSourceStack> ctx) {
		int min = IntegerArgumentType.getInteger(ctx, "min_minutes");
		int max = Math.max(min, IntegerArgumentType.getInteger(ctx, "max_minutes"));
		WorldState state = WorldState.get(ctx.getSource().getServer());
		state.momentMinMinutes = min;
		state.momentMaxMinutes = max;
		state.changed();
		for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			SoulData soul = Soul.get(p);
			soul.nextMoment = 0L;
			Soul.save(p, soul);
		}
		ctx.getSource().sendSuccess(() -> Component.literal("A moment of choice will happen every " + min + "-" + max + " minutes."), true);
		return 1;
	}

	private static int toggleJudgment(CommandContext<CommandSourceStack> ctx, boolean on) {
		WorldState state = WorldState.get(ctx.getSource().getServer());
		state.judgmentEnabled = on;
		state.changed();
		ctx.getSource().sendSuccess(() -> Component.literal("Judgment on death is now " + (on ? "on" : "off") + "."), true);
		return 1;
	}

	private static int kit(CommandContext<CommandSourceStack> ctx, boolean heaven) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		if (heaven) {
			give(p, new ItemStack(ModItems.ANGEL_WINGS));
			give(p, new ItemStack(ModItems.HALO));
			give(p, new ItemStack(ModItems.HARP));
			give(p, new ItemStack(ModItems.SERAPH_BLADE));
			give(p, new ItemStack(ModItems.MANNA, 16));
			give(p, new ItemStack(ModItems.CLOUD_IN_A_BOTTLE, 16));
			give(p, new ItemStack(ModItems.FEATHER_OF_RETURN, 4));
			give(p, new ItemStack(ModItems.GOLDEN_FEATHER, 16));
			give(p, new ItemStack(ModBlocks.CLOUD, 64));
		} else {
			give(p, new ItemStack(ModItems.SOUL_SHARD, 64));
			give(p, new ItemStack(ModItems.INFERNAL_PICKAXE));
			give(p, new ItemStack(ModItems.HELLFIRE_SWORD));
			give(p, new ItemStack(ModItems.DEMON_WINGS));
			give(p, new ItemStack(ModItems.INFERNAL_CROWN));
			give(p, new ItemStack(ModItems.PALACE_KEY));
			give(p, new ItemStack(Items.GOLDEN_APPLE, 8));
		}
		give(p, new ItemStack(ModItems.BOOK_OF_DEEDS));
		return 1;
	}

	private static void give(ServerPlayer p, ItemStack stack) {
		if (!p.getInventory().add(stack)) {
			p.drop(stack, false);
		}
	}

	private static int rebuild(CommandContext<CommandSourceStack> ctx, boolean heaven) {
		MinecraftServer server = ctx.getSource().getServer();
		WorldState state = WorldState.get(server);
		if (heaven) {
			ServerLevel level = Realms.heaven(server);
			if (level != null) {
				Realms.buildHeaven(level);
				state.heavenBuilt = true;
			}
		} else {
			ServerLevel level = Realms.hell(server);
			if (level != null) {
				Realms.buildHell(level);
				state.hellBuilt = true;
				if (state.gateOpen) {
					LuciferBattle.openGate(level);
				}
			}
		}
		state.changed();
		ctx.getSource().sendSuccess(() -> Component.literal("Rebuilt " + (heaven ? "the Pearly Gates." : "the Infernal Court.")), true);
		return 1;
	}
}
