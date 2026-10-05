package com.timeywimey.heavenhell.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;

import com.timeywimey.heavenhell.registry.ModItems;
import com.timeywimey.heavenhell.util.Cmd;
import com.timeywimey.heavenhell.util.Txt;

/**
 * Builds a vanilla dialog screen (as SNBT) and shows it with {@code /dialog show}.
 * Buttons run {@code /heavenhell ...} commands, which every player is allowed to use.
 */
public final class DialogBuilder {
	private final Txt title;
	private final List<String> body = new ArrayList<>();
	private final List<String> buttons = new ArrayList<>();
	private String exit;
	private int columns = 2;
	private boolean escape = true;
	private boolean pause = true;
	private int buttonWidth = 150;

	private DialogBuilder(Txt title) {
		this.title = title;
	}

	public static DialogBuilder of(Txt title) {
		return new DialogBuilder(title);
	}

	/**
	 * A picture for the dialog. Item icons are always drawn at 16x16 in dialogs, so the "vision" textures are also
	 * glyphs of two bitmap fonts (assets/heavenhell/font/vision*.json); a glyph can be as big as we like. The glyph sits
	 * on the first line and the following blank lines reserve room for its height.
	 */
	public DialogBuilder picture(String itemId, int size) {
		String name = itemId.substring(itemId.indexOf(':') + 1);
		int index = Arrays.asList(ModItems.VISIONS).indexOf(name);
		if (index < 0) {
			return this;
		}
		boolean big = size >= 96;
		String font = big ? "heavenhell:vision" : "heavenhell:vision_small";
		int padLines = big ? 7 : 4;
		StringBuilder pad = new StringBuilder();
		for (int i = 0; i < padLines; i++) {
			pad.append("\n\u00a0");
		}
		String glyph = String.valueOf((char) (0xE000 + index));
		body.add("{type:\"minecraft:plain_message\",width:300,contents:{\"text\":" + Txt.quote(glyph) + ",\"font\":\"" + font
				+ "\",\"color\":\"white\",\"shadow_color\":0,\"extra\":[{\"text\":" + Txt.quote(pad.toString())
				+ ",\"font\":\"minecraft:default\"}]}}");
		return this;
	}

	/** An item with a description next to it. */
	public DialogBuilder itemLine(String itemId, Txt description) {
		body.add("{type:\"minecraft:item\",item:{id:" + Txt.quote(itemId) + "},show_decorations:false,description:{contents:"
				+ description.str() + ",width:240}}");
		return this;
	}

	public DialogBuilder text(Txt text) {
		return text(text, 300);
	}

	public DialogBuilder text(Txt text, int width) {
		body.add("{type:\"minecraft:plain_message\",contents:" + text.str() + ",width:" + width + "}");
		return this;
	}

	public DialogBuilder button(Txt label, Txt tooltip, String command) {
		StringBuilder sb = new StringBuilder("{label:").append(label.str());
		if (tooltip != null) {
			sb.append(",tooltip:").append(tooltip.str());
		}
		sb.append(",width:").append(buttonWidth);
		if (command != null) {
			sb.append(",action:{type:\"minecraft:run_command\",command:").append(Txt.quote(command)).append('}');
		}
		buttons.add(sb.append('}').toString());
		return this;
	}

	/** Footer button (also used when the dialog is closed with ESC, if allowed). */
	public DialogBuilder exit(Txt label, String command) {
		StringBuilder sb = new StringBuilder("{label:").append(label.str()).append(",width:200");
		if (command != null) {
			sb.append(",action:{type:\"minecraft:run_command\",command:").append(Txt.quote(command)).append('}');
		}
		exit = sb.append('}').toString();
		return this;
	}

	public DialogBuilder columns(int columns) {
		this.columns = columns;
		return this;
	}

	public DialogBuilder buttonWidth(int width) {
		this.buttonWidth = width;
		return this;
	}

	/** The player must pick a button (ESC does nothing). */
	public DialogBuilder mustChoose() {
		this.escape = false;
		return this;
	}

	public DialogBuilder noPause() {
		this.pause = false;
		return this;
	}

	public String snbt() {
		StringBuilder sb = new StringBuilder("{type:\"minecraft:multi_action\",title:").append(title.str());
		if (!body.isEmpty()) {
			sb.append(",body:[").append(String.join(",", body)).append(']');
		}
		if (buttons.isEmpty()) {
			button(Txt.of("OK"), null, null);
		}
		sb.append(",actions:[").append(String.join(",", buttons)).append(']');
		if (exit != null) {
			sb.append(",exit_action:").append(exit);
		}
		sb.append(",columns:").append(columns);
		sb.append(",can_close_with_escape:").append(escape);
		sb.append(",pause:").append(pause);
		sb.append(",after_action:\"close\"}");
		return sb.toString();
	}

	public void show(ServerPlayer player) {
		Cmd.run(player.level().getServer(), "dialog show " + Cmd.target(player) + " " + snbt());
	}
}
