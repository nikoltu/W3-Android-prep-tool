package com.nikoltu.w3prep.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.Icon;

public final class Glyph implements Icon {
	public enum Shape {
		CHECK, CROSS, EXCLAMATION, CHEVRON_RIGHT, CHEVRON_DOWN
	}

	private final Shape shape;
	private final int size;
	private final Color color;
	private final Color badge;

	public Glyph(final Shape shape, final int size, final Color color, final Color badge) {
		this.shape = shape;
		this.size = size;
		this.color = color;
		this.badge = badge;
	}

	public Glyph(final Shape shape, final int size, final Color color) {
		this(shape, size, color, null);
	}

	@Override
	public void paintIcon(final Component c, final Graphics graphics, final int x, final int y) {
		final Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.translate(x, y);
		final double s = this.size;
		double inset = 0;
		if (this.badge != null) {
			g.setColor(this.badge);
			g.fillOval(0, 0, this.size, this.size);
			inset = s * 0.27;
		}
		final double w = s - (2 * inset);
		g.setColor(this.color);
		g.setStroke(new BasicStroke((float) Math.max(1.6, s / (this.badge != null ? 18 : 8)), BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		final Path2D path = new Path2D.Double();
		switch (this.shape) {
		case CHECK:
			path.moveTo(inset + (w * 0.15), inset + (w * 0.55));
			path.lineTo(inset + (w * 0.4), inset + (w * 0.8));
			path.lineTo(inset + (w * 0.88), inset + (w * 0.22));
			break;
		case CROSS:
			path.moveTo(inset + (w * 0.2), inset + (w * 0.2));
			path.lineTo(inset + (w * 0.8), inset + (w * 0.8));
			path.moveTo(inset + (w * 0.8), inset + (w * 0.2));
			path.lineTo(inset + (w * 0.2), inset + (w * 0.8));
			break;
		case EXCLAMATION:
			path.moveTo(inset + (w * 0.5), inset + (w * 0.12));
			path.lineTo(inset + (w * 0.5), inset + (w * 0.62));
			path.moveTo(inset + (w * 0.5), inset + (w * 0.88));
			path.lineTo(inset + (w * 0.5), inset + (w * 0.9));
			break;
		case CHEVRON_RIGHT:
			path.moveTo(inset + (w * 0.35), inset + (w * 0.18));
			path.lineTo(inset + (w * 0.68), inset + (w * 0.5));
			path.lineTo(inset + (w * 0.35), inset + (w * 0.82));
			break;
		case CHEVRON_DOWN:
			path.moveTo(inset + (w * 0.18), inset + (w * 0.35));
			path.lineTo(inset + (w * 0.5), inset + (w * 0.68));
			path.lineTo(inset + (w * 0.82), inset + (w * 0.35));
			break;
		default:
			break;
		}
		g.draw(path);
		g.dispose();
	}

	@Override
	public int getIconWidth() {
		return this.size;
	}

	@Override
	public int getIconHeight() {
		return this.size;
	}
}
