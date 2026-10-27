package com.nikoltu.w3prep.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

final class ConfirmOverlay extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JRootPane root;

	ConfirmOverlay(final JRootPane root) {
		super(new GridBagLayout());
		this.root = root;
		setOpaque(false);
		addMouseListener(new MouseAdapter() {
		});
	}

	void ask(final String title, final String text, final String keepLabel, final String stopLabel,
			final Runnable onStop) {
		removeAll();
		final RoundPanel card = new RoundPanel(new ColumnLayout(14), Theme.PANEL, Theme.BRONZE, 14, 22, 24) {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				if (getWidth() != 440) {
					setSize(440, Math.max(1, getHeight()));
				}
				return new Dimension(440, super.getPreferredSize().height);
			}
		};
		card.add(Ui.label(title, Theme.bold(17f), Theme.TEXT));
		card.add(Ui.text(text, Theme.SOFT));
		final JButton keep = Ui.primary(keepLabel);
		keep.setFont(Theme.bold(13f));
		keep.putClientProperty("FlatLaf.style", "margin: 7,16,7,16; background: #C9A24A; foreground: #14110A;"
				+ "borderColor: #C9A24A; hoverBackground: #E8C76A");
		final JButton stop = Ui.button(stopLabel);
		keep.addActionListener(e -> close());
		stop.addActionListener(e -> {
			close();
			onStop.run();
		});
		final JPanel buttons = new JPanel(new BorderLayout());
		buttons.setOpaque(false);
		buttons.add(Ui.row(10, keep, stop), BorderLayout.EAST);
		card.add(buttons);
		add(card);
		this.root.setGlassPane(this);
		setVisible(true);
		SwingUtilities.invokeLater(keep::requestFocusInWindow);
	}

	void close() {
		setVisible(false);
	}

	@Override
	protected void paintComponent(final Graphics g) {
		g.setColor(new Color(5, 7, 10, 140));
		g.fillRect(0, 0, getWidth(), getHeight());
		super.paintComponent(g);
	}
}
