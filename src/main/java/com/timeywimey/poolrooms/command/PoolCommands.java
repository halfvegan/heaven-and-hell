package com.timeywimey.poolrooms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.timeywimey.poolrooms.gameplay.LifeguardDirector;
import com.timeywimey.poolrooms.gameplay.PoolPlayer;
import com.timeywimey.poolrooms.gameplay.PoolState;
import com.timeywimey.poolrooms.gameplay.PoolTravel;
import com.timeywimey.poolrooms.registry.ModBlocks;
import com.timeywimey.poolrooms.registry.ModItems;
import com.timeywimey.poolrooms.util.Give;
import com.timeywimey.poolrooms.world.Pool;

/**
 * {@code /poolrooms}: {@code lifeguard on|off} works for everyone (it is a comfort setting); {@code enter}, {@code exit}
 * and {@code kit} need cheats.
 */
public final class PoolCommands {
	private PoolCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static boolean isAdmin(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("poolrooms")
				.executes(PoolCommands::help)
				.then(Commands.literal("help").executes(PoolCommands::help))
				.then(Commands.literal("lifeguard")
						.executes(ctx -> {
							boolean on = LifeguardDirector.enabled(ctx.getSource().getServer());
							ctx.getSource().sendSuccess(() -> Component.literal("The Lifeguard is " + (on ? "on duty." : "off duty."))
									.withStyle(ChatFormatting.AQUA), false);
							return on ? 1 : 0;
						})
						.then(Commands.literal("on").executes(ctx -> lifeguard(ctx, true)))
						.then(Commands.literal("off").executes(ctx -> lifeguard(ctx, false))))
				.then(Commands.literal("enter").requires(PoolCommands::isAdmin).executes(PoolCommands::enter))
				.then(Commands.literal("exit").requires(PoolCommands::isAdmin).executes(PoolCommands::exit))
				.then(Commands.literal("kit").requires(PoolCommands::isAdmin).executes(PoolCommands::kit)));
	}

	private static int help(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack s = ctx.getSource();
		s.sendSuccess(() -> Component.literal("The Poolrooms").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
		s.sendSuccess(() -> Component.literal("Find 3 wristbands (blue, yellow, red), then find the EXIT door. Don't run.")
				.withStyle(ChatFormatting.WHITE), false);
		s.sendSuccess(() -> Component.literal("/poolrooms lifeguard on|off - turn the Lifeguard on or off").withStyle(ChatFormatting.YELLOW), false);
		if (isAdmin(s)) {
			s.sendSuccess(() -> Component.literal("/poolrooms enter - go to the Entrance Pool").withStyle(ChatFormatting.YELLOW), false);
			s.sendSuccess(() -> Component.literal("/poolrooms exit - go back home").withStyle(ChatFormatting.YELLOW), false);
			s.sendSuccess(() -> Component.literal("/poolrooms kit - every Poolrooms item").withStyle(ChatFormatting.YELLOW), false);
		}
		return 1;
	}

	private static int lifeguard(CommandContext<CommandSourceStack> ctx, boolean on) {
		PoolState state = PoolState.get(ctx.getSource().getServer());
		state.lifeguard = on;
		state.changed();
		if (!on) {
			LifeguardDirector.dismissAll(ctx.getSource().getServer());
		}
		ctx.getSource().sendSuccess(() -> Component.literal(on
				? "The Lifeguard is on duty. Remember: no running!"
				: "The Lifeguard is off duty. The Poolrooms are all yours.").withStyle(ChatFormatting.AQUA), true);
		return 1;
	}

	private static int enter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (Pool.isPoolrooms(player.level())) {
			PoolTravel.teleport(player, player.level(), Pool.ARRIVAL, Pool.ARRIVAL_YAW, 0.0F);
			return 1;
		}
		Vec3 pos = player.position();
		PoolTravel.enter(player, new PoolPlayer.ReturnPoint(player.level().dimension().identifier().toString(), pos.x, pos.y, pos.z));
		return 1;
	}

	private static int exit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (!Pool.isPoolrooms(player.level())) {
			ctx.getSource().sendFailure(Component.literal("You are not in the Poolrooms."));
			return 0;
		}
		PoolTravel.exitHome(player, false);
		return 1;
	}

	private static int kit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		give(p, ModItems.SNORKEL_MASK, 1);
		give(p, ModItems.FLIPPERS, 1);
		give(p, ModItems.POOL_NOODLE, 1);
		give(p, ModItems.CHLORINE_COMPASS, 1);
		give(p, ModBlocks.BLUE_WRISTBAND, 1);
		give(p, ModBlocks.YELLOW_WRISTBAND, 1);
		give(p, ModBlocks.RED_WRISTBAND, 1);
		give(p, ModBlocks.STRANGE_DRAIN, 2);
		give(p, ModBlocks.WHITE_POOL_TILES, 64);
		give(p, ModBlocks.AQUA_POOL_TILES, 64);
		give(p, ModBlocks.MOSAIC_POOL_TILES, 32);
		give(p, ModBlocks.LANE_LINE_TILES, 32);
		give(p, ModBlocks.WHITE_POOL_TILE_STAIRS, 32);
		give(p, ModBlocks.WHITE_POOL_TILE_SLAB, 32);
		give(p, ModBlocks.CEILING_LIGHT, 16);
		give(p, ModBlocks.FAKE_SKY_PANEL, 16);
		give(p, ModBlocks.POOL_LIGHT, 16);
		give(p, ModBlocks.POOL_LADDER, 8);
		give(p, ModBlocks.POOL_CHAIR, 4);
		give(p, ModBlocks.LOCKER, 4);
		give(p, ModBlocks.LIFE_RING, 4);
		give(p, Items.WATER_BUCKET, 1);
		ctx.getSource().sendSuccess(() -> Component.literal("Here's everything from the Poolrooms.").withStyle(ChatFormatting.AQUA), false);
		return 1;
	}

	private static void give(ServerPlayer player, ItemLike item, int count) {
		Give.give(player, new ItemStack(item, count));
	}
}
