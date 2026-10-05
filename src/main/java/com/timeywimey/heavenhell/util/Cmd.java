package com.timeywimey.heavenhell.util;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import com.timeywimey.heavenhell.HeavenHell;

/**
 * Runs vanilla commands as the server (silently). Used for titles, sounds, particles, teleports and
 * structure placement so the mod relies on the stable command syntax.
 */
public final class Cmd {
	private Cmd() {
	}

	public static void run(MinecraftServer server, String command) {
		try {
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new FailureLog(command)), command);
		} catch (Exception e) {
			HeavenHell.LOGGER.warn("Command failed: {}", command, e);
		}
	}

	/** Swallows command feedback, but writes errors to the log so broken commands are easy to spot. */
	private record FailureLog(String command) implements CommandSource {
		@Override
		public void sendSystemMessage(Component message) {
			String text = message.getString();
			if (command.contains("particle ") || command.contains("kill @e") || text.startsWith("Nothing changed")) {
				return; // harmless: nobody saw the particles / nothing to remove
			}
			String shown = command.length() > 300 ? command.substring(0, 300) + "..." : command;
			HeavenHell.LOGGER.warn("Command '{}' failed: {}", shown, text);
		}

		@Override
		public boolean acceptsSuccess() {
			return false;
		}

		@Override
		public boolean acceptsFailure() {
			return true;
		}

		@Override
		public boolean shouldInformAdmins() {
			return false;
		}
	}

	/** Runs a command as the player, at the player's position (selector {@code @s} = the player). */
	public static void as(ServerPlayer player, String command) {
		run(player.level().getServer(), "execute as " + player.getStringUUID() + " at @s run " + command);
	}

	/** Runs a command positioned in the given level (useful for fill/setblock/particle in other dimensions). */
	public static void in(ServerLevel level, String command) {
		run(level.getServer(), "execute in " + level.dimension().identifier() + " run " + command);
	}

	/** How commands address a player. Player-only commands (title, dialog...) reject UUIDs, so use the name. */
	public static String target(ServerPlayer player) {
		return player.getScoreboardName();
	}

	public static void title(ServerPlayer player, String titleJson, String subtitleJson, int fadeIn, int stay, int fadeOut) {
		String t = target(player);
		MinecraftServer server = player.level().getServer();
		run(server, "title " + t + " times " + fadeIn + " " + stay + " " + fadeOut);
		if (subtitleJson != null) {
			run(server, "title " + t + " subtitle " + subtitleJson);
		}
		run(server, "title " + t + " title " + titleJson);
	}

	public static void actionbar(ServerPlayer player, String json) {
		run(player.level().getServer(), "title " + target(player) + " actionbar " + json);
	}

	/** Plays a sound to the player only. */
	public static void sound(ServerPlayer player, String sound, float volume, float pitch) {
		as(player, "playsound " + sound + " master @s ~ ~ ~ " + volume + " " + pitch);
	}

	/** Plays a sound at a position for everyone nearby. */
	public static void soundAt(ServerLevel level, double x, double y, double z, String sound, float volume, float pitch) {
		in(level, "playsound " + sound + " master @a " + fmt(x) + " " + fmt(y) + " " + fmt(z) + " " + volume + " " + pitch);
	}

	public static void particle(ServerLevel level, String particle, double x, double y, double z,
			double dx, double dy, double dz, double speed, int count) {
		in(level, "particle " + particle + " " + fmt(x) + " " + fmt(y) + " " + fmt(z) + " "
				+ fmt(dx) + " " + fmt(dy) + " " + fmt(dz) + " " + fmt(speed) + " " + count + " force");
	}

	public static String fmt(double d) {
		return String.format(java.util.Locale.ROOT, "%.3f", d);
	}
}
