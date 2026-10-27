package com.nikoltu.w3prep.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

public final class Theme {
	public static final Color GROUND = new Color(0x0D1016);
	public static final Color PANEL = new Color(0x161B24);
	public static final Color PANEL_2 = new Color(0x11151C);
	public static final Color EDGE = new Color(0x2E3644);
	public static final Color BRONZE = new Color(0x3A3322);
	public static final Color RULE = new Color(0x262D39);
	public static final Color FIELD_EDGE = new Color(0x3D4656);
	public static final Color BUTTON = new Color(0x1C2230);
	public static final Color TEXT = new Color(0xECE3CF);
	public static final Color SOFT = new Color(0xCFC6B2);
	public static final Color MUTED = new Color(0xA9A190);
	public static final Color DIM = new Color(0x7B7466);
	public static final Color GOLD = new Color(0xC9A24A);
	public static final Color GOLD_HI = new Color(0xE8C76A);
	public static final Color GOLD_WASH = new Color(0x2B2416);
	public static final Color ON_GOLD = new Color(0x14110A);
	public static final Color OK = new Color(0x8FD49E);
	public static final Color OK_BG = new Color(0x16281C);
	public static final Color WARN = new Color(0xE9B865);
	public static final Color WARN_BG = new Color(0x2D2414);
	public static final Color ERR = new Color(0xF08A7A);
	public static final Color ERR_BG = new Color(0x2C1714);
	public static final Color ERR_EDGE = new Color(0x6A3A33);

	private static Font body;
	private static Font mono;

	private Theme() {
	}

	public static void install() {
		final Map<String, String> extra = new HashMap<>();
		extra.put("@accentColor", "#C9A24A");
		extra.put("@background", "#0D1016");
		extra.put("@foreground", "#ECE3CF");
		FlatLaf.setGlobalExtraDefaults(extra);
		FlatDarkLaf.setup();

		final Font base = UIManager.getFont("defaultFont");
		body = base.deriveFont(13f);
		UIManager.put("defaultFont", body);
		mono = pickMono(12f);

		UIManager.put("Panel.background", GROUND);
		UIManager.put("RootPane.background", GROUND);
		UIManager.put("TitlePane.background", PANEL);
		UIManager.put("TitlePane.inactiveBackground", PANEL);
		UIManager.put("TitlePane.foreground", SOFT);
		UIManager.put("Label.foreground", TEXT);
		UIManager.put("Component.arc", 12);
		UIManager.put("Button.arc", 12);
		UIManager.put("Component.borderColor", FIELD_EDGE);
		UIManager.put("Component.focusedBorderColor", GOLD);
		UIManager.put("Component.focusColor", new Color(0x66C9A24A, true));
		UIManager.put("Button.background", BUTTON);
		UIManager.put("Button.foreground", TEXT);
		UIManager.put("Button.borderColor", FIELD_EDGE);
		UIManager.put("Button.hoverBackground", new Color(0x252C3C));
		UIManager.put("Button.default.background", GOLD);
		UIManager.put("Button.default.foreground", ON_GOLD);
		UIManager.put("Button.default.borderColor", GOLD);
		UIManager.put("Button.default.hoverBackground", GOLD_HI);
		UIManager.put("Button.default.focusedBorderColor", GOLD_HI);
		UIManager.put("TextField.background", PANEL_2);
		UIManager.put("TextField.inactiveBackground", PANEL_2);
		UIManager.put("TextField.foreground", TEXT);
		UIManager.put("TextField.inactiveForeground", TEXT);
		UIManager.put("ProgressBar.foreground", GOLD);
		UIManager.put("ProgressBar.background", PANEL_2);
		UIManager.put("ProgressBar.arc", 10);
		UIManager.put("ScrollPane.background", GROUND);
		UIManager.put("ScrollBar.track", GROUND);
		UIManager.put("ScrollBar.thumb", EDGE);
		UIManager.put("ScrollBar.width", 10);
		UIManager.put("CheckBox.foreground", TEXT);
		UIManager.put("RadioButton.foreground", TEXT);
		UIManager.put("ToolTip.background", PANEL);
		UIManager.put("ToolTip.foreground", TEXT);
	}

	private static Font pickMono(final float size) {
		final List<String> families = Arrays
				.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
		for (final String name : Arrays.asList("Cascadia Mono", "Consolas", "SF Mono", "Menlo", "DejaVu Sans Mono", "Liberation Mono")) {
			if (families.contains(name)) {
				return new Font(name, Font.PLAIN, Math.round(size));
			}
		}
		return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
	}

	public static Font body(final float size) {
		return body.deriveFont(size);
	}

	public static Font bold(final float size) {
		return body.deriveFont(Font.BOLD, size);
	}

	public static Font mono() {
		return mono;
	}
}
