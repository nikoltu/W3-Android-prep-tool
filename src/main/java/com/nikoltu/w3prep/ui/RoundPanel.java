package com.nikoltu.w3prep.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class RoundPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private Color fill;
	private Color edge;
	private final int arc;

	public RoundPanel(final LayoutManager layout, final Color fill, final Color edge, final int arc,
			final int padY, final int padX) {
		super(layout);
		this.fill = fill;
		this.edge = edge;
		this.arc = arc;
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(padY, padX, padY, padX));
	}

	public void setColors(final Color fill, final Color edge) {
		this.fill = fill;
		this.edge = edge;
		repaint();
	}

	@Override
	protected void paintComponent(final Graphics graphics) {
		final Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (this.fill != null) {
			g.setColor(this.fill);
			g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, this.arc, this.arc);
		}
		if (this.edge != null) {
			g.setColor(this.edge);
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, this.arc, this.arc);
		}
		g.dispose();
		super.paintComponent(graphics);
	}
}
