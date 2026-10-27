package com.nikoltu.w3prep.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.HashSet;
import java.util.Set;

final class ColumnLayout implements LayoutManager2 {
	static final String FILL = "fill";
	private final int gap;
	private final Set<Component> fillers = new HashSet<>();

	ColumnLayout(final int gap) {
		this.gap = gap;
	}

	@Override
	public void addLayoutComponent(final Component comp, final Object constraints) {
		if (FILL.equals(constraints)) {
			this.fillers.add(comp);
		}
	}

	@Override
	public void addLayoutComponent(final String name, final Component comp) {
	}

	@Override
	public void removeLayoutComponent(final Component comp) {
		this.fillers.remove(comp);
	}

	private static int innerWidth(final Container parent) {
		final Insets in = parent.getInsets();
		return Math.max(0, parent.getWidth() - in.left - in.right);
	}

	private static Dimension preferred(final Component child, final int width) {
		if (width > 0) {
			child.setSize(width, child instanceof WrapText ? Short.MAX_VALUE : Math.max(1, child.getHeight()));
		}
		return child.getPreferredSize();
	}

	@Override
	public Dimension preferredLayoutSize(final Container parent) {
		final Insets in = parent.getInsets();
		final int width = innerWidth(parent);
		int maxWidth = 0;
		int height = 0;
		int count = 0;
		for (final Component child : parent.getComponents()) {
			if (!child.isVisible() || this.fillers.contains(child)) {
				continue;
			}
			final Dimension size = preferred(child, width);
			maxWidth = Math.max(maxWidth, size.width);
			height += size.height;
			count++;
		}
		height += Math.max(0, count - 1) * this.gap;
		return new Dimension(maxWidth + in.left + in.right, height + in.top + in.bottom);
	}

	@Override
	public Dimension minimumLayoutSize(final Container parent) {
		return preferredLayoutSize(parent);
	}

	@Override
	public Dimension maximumLayoutSize(final Container target) {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public void layoutContainer(final Container parent) {
		final Insets in = parent.getInsets();
		final int width = innerWidth(parent);
		final int available = parent.getHeight() - in.top - in.bottom;
		final int spare = Math.max(0, available - (preferredLayoutSize(parent).height - in.top - in.bottom));
		int y = in.top;
		boolean first = true;
		for (final Component child : parent.getComponents()) {
			if (!child.isVisible()) {
				continue;
			}
			if (this.fillers.contains(child)) {
				child.setBounds(in.left, y, width, spare);
				y += spare;
				continue;
			}
			if (!first) {
				y += this.gap;
			}
			first = false;
			final int height = preferred(child, width).height;
			child.setBounds(in.left, y, width, height);
			y += height;
		}
	}

	@Override
	public float getLayoutAlignmentX(final Container target) {
		return 0;
	}

	@Override
	public float getLayoutAlignmentY(final Container target) {
		return 0;
	}

	@Override
	public void invalidateLayout(final Container target) {
	}
}
