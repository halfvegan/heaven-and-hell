package com.timeywimey.heavenhell.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny builder for text components in SNBT/JSON form, used inside commands and dialogs.
 * Example: {@code Txt.of("Hello", "gold").bold().str()} -> {@code {"text":"Hello","color":"gold","bold":true}}
 */
public final class Txt {
	private final String text;
	private final String color;
	private boolean bold;
	private boolean italic;
	private final List<Txt> extra = new ArrayList<>();

	private Txt(String text, String color) {
		this.text = text;
		this.color = color;
	}

	public static Txt of(String text) {
		return new Txt(text, null);
	}

	public static Txt of(String text, String color) {
		return new Txt(text, color);
	}

	public Txt bold() {
		this.bold = true;
		return this;
	}

	public Txt italic() {
		this.italic = true;
		return this;
	}

	public Txt then(Txt next) {
		this.extra.add(next);
		return this;
	}

	public Txt then(String text, String color) {
		return then(Txt.of(text, color));
	}

	public String str() {
		StringBuilder sb = new StringBuilder("{\"text\":").append(quote(text));
		if (color != null) {
			sb.append(",\"color\":").append(quote(color));
		}
		if (bold) {
			sb.append(",\"bold\":true");
		}
		// default dialog/chat text is not italic; titles are not italic either
		sb.append(",\"italic\":").append(italic ? "true" : "false");
		if (!extra.isEmpty()) {
			sb.append(",\"extra\":[");
			for (int i = 0; i < extra.size(); i++) {
				if (i > 0) {
					sb.append(',');
				}
				sb.append(extra.get(i).str());
			}
			sb.append(']');
		}
		return sb.append('}').toString();
	}

	@Override
	public String toString() {
		return str();
	}

	/** Quotes a string for JSON / SNBT (double quotes). */
	public static String quote(String s) {
		StringBuilder sb = new StringBuilder("\"");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				default -> sb.append(c);
			}
		}
		return sb.append('"').toString();
	}
}
