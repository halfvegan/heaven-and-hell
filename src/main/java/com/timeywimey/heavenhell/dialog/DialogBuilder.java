package com.timeywimey.heavenhell.dialog;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;

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

	/** A big picture made from an item texture (our "vision" items). */
	public DialogBuilder picture(String itemId, int size) {
		body.add("{type:\"minecraft:item\",item:{id:" + Txt.quote(itemId) + "},show_decorations:false,show_tooltip:false,width:"
				+ size + ",height:" + size + "}");
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
