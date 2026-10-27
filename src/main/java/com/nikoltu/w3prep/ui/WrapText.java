package com.nikoltu.w3prep.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;

public final class WrapText extends JTextArea {
	private static final long serialVersionUID = 1L;

	public WrapText(final String text, final Font font, final Color color) {
		super(text);
		setFont(font);
		setForeground(color);
		setLineWrap(true);
		setWrapStyleWord(true);
		setEditable(false);
		setFocusable(false);
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder());
		setHighlighter(null);
	}

	@Override
	public Dimension getPreferredSize() {
		final Dimension size = super.getPreferredSize();
		return new Dimension(Math.min(size.width, 420), size.height);
	}
}
