package com.nikoltu.w3prep.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class Ui {
	private Ui() {
	}

	public static JPanel column(final int gap) {
		final JPanel panel = new JPanel(new ColumnLayout(gap));
		panel.setOpaque(false);
		return panel;
	}

	public static RoundPanel card() {
		return new RoundPanel(new ColumnLayout(10), Theme.PANEL, Theme.BRONZE, 12, 14, 18);
	}

	public static RoundPanel quietCard() {
		return new RoundPanel(new ColumnLayout(10), Theme.PANEL_2, Theme.RULE, 12, 14, 18);
	}

	public static JLabel label(final String text, final Font font, final Color color) {
		final JLabel label = new JLabel(text);
		label.setFont(font);
		label.setForeground(color);
		return label;
	}

	public static JLabel title(final String text) {
		return label(text, Theme.bold(22f), Theme.TEXT);
	}

	public static JLabel heading(final String text) {
		return label(text, Theme.bold(13f), Theme.TEXT);
	}

	public static WrapText sub(final String text) {
		return new WrapText(text, Theme.body(13f), Theme.MUTED);
	}

	public static WrapText text(final String text, final Color color) {
		return new WrapText(text, Theme.body(13f), color);
	}

	public static JTextField pathField(final String accessibleName) {
		final JTextField field = new JTextField();
		field.setEditable(false);
		field.setFont(Theme.mono());
		field.putClientProperty("FlatLaf.style", "margin: 6,10,6,10");
		field.getAccessibleContext().setAccessibleName(accessibleName);
		return field;
	}

	public static JButton button(final String text) {
		final JButton button = new JButton(text);
		button.putClientProperty("FlatLaf.style", "margin: 6,14,6,14");
		return button;
	}

	public static JButton primary(final String text) {
		final JButton button = new JButton(text);
		button.setFont(Theme.bold(15f));
		button.putClientProperty("FlatLaf.style", "margin: 11,24,11,24;"
				+ "background: #C9A24A; foreground: #14110A; borderColor: #C9A24A; focusedBorderColor: #E8C76A;"
				+ "hoverBackground: #E8C76A; pressedBackground: #B8892E;"
				+ "disabledBackground: #2E3644; disabledText: #7B7466; disabledBorderColor: #2E3644");
		return button;
	}

	public static JButton outlineGold(final String text) {
		final JButton button = new JButton(text);
		button.setFont(Theme.bold(13f));
		button.putClientProperty("FlatLaf.style", "margin: 6,16,6,16;"
				+ "background: #2B2416; foreground: #E8C76A; borderColor: #C9A24A; hoverBackground: #3A3020");
		return button;
	}

	public static JButton link(final String text, final Icon icon) {
		final JButton button = new JButton(text, icon);
		button.setFont(Theme.bold(13f));
		button.setForeground(Theme.GOLD_HI);
		button.putClientProperty("JButton.buttonType", "borderless");
		button.putClientProperty("FlatLaf.style", "margin: 4,4,4,4");
		button.setHorizontalAlignment(JButton.LEFT);
		return button;
	}

	public static JButton quiet(final String text) {
		final JButton button = new JButton(text);
		button.setForeground(Theme.SOFT);
		button.putClientProperty("JButton.buttonType", "borderless");
		button.putClientProperty("FlatLaf.style", "margin: 4,10,4,10");
		return button;
	}

	public static JPanel fieldRow(final JComponent field, final JComponent button) {
		final JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);
		row.add(field, BorderLayout.CENTER);
		if (button != null) {
			row.add(button, BorderLayout.EAST);
		}
		return row;
	}

	public static JPanel row(final int gap, final Component... parts) {
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				row.add(Box.createHorizontalStrut(gap));
			}
			row.add(parts[i]);
		}
		row.add(Box.createHorizontalGlue());
		return row;
	}

	public static JPanel flow(final int gap) {
		final JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public void layoutContainer(final java.awt.Container target) {
				super.layoutContainer(target);
				for (final Component child : target.getComponents()) {
					child.setLocation(child.getX() - gap, child.getY());
				}
			}
		});
		flow.setOpaque(false);
		return flow;
	}

	public static JPanel flowOf(final Component... parts) {
		final JPanel flow = flow(8);
		for (final Component part : parts) {
			flow.add(part);
		}
		return flow;
	}

	public enum ChipKind {
		OK, WARN, ERR, IDLE
	}

	public static JLabel chip(final String text, final ChipKind kind) {
		final Color fill;
		final Color color;
		Icon icon = null;
		switch (kind) {
		case OK:
			fill = Theme.OK_BG;
			color = Theme.OK;
			icon = new Glyph(Glyph.Shape.CHECK, 12, color);
			break;
		case WARN:
			fill = Theme.WARN_BG;
			color = Theme.WARN;
			icon = new Glyph(Glyph.Shape.EXCLAMATION, 12, color);
			break;
		case ERR:
			fill = Theme.ERR_BG;
			color = Theme.ERR;
			icon = new Glyph(Glyph.Shape.CROSS, 12, color);
			break;
		default:
			fill = Theme.PANEL_2;
			color = Theme.MUTED;
			break;
		}
		final JLabel chip = new JLabel(text, icon, JLabel.LEFT) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(final Graphics graphics) {
				final Graphics2D g = (Graphics2D) graphics.create();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(fill);
				g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
				g.dispose();
				super.paintComponent(graphics);
			}
		};
		chip.setIconTextGap(6);
		chip.setFont(Theme.body(12.5f));
		chip.setForeground(color);
		chip.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
		return chip;
	}

	public static JLabel tag(final String text, final Color fill, final Color color) {
		final JLabel painted = new JLabel(text) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(final Graphics graphics) {
				final Graphics2D g = (Graphics2D) graphics.create();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(fill);
				g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
				g.dispose();
				super.paintComponent(graphics);
			}
		};
		painted.setFont(Theme.bold(10.5f));
		painted.setForeground(color);
		painted.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
		return painted;
	}

	public static RoundPanel notice(final String text, final Color fill, final Color color) {
		final RoundPanel strip = new RoundPanel(new ColumnLayout(0), fill, null, 12, 9, 14);
		strip.add(text(text, color));
		return strip;
	}

	public static JComponent rule() {
		final JPanel rule = new JPanel();
		rule.setBackground(Theme.RULE);
		rule.setOpaque(true);
		rule.setPreferredSize(new Dimension(1, 1));
		return rule;
	}
}
