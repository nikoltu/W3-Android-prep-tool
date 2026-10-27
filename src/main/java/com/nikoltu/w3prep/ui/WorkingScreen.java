package com.nikoltu.w3prep.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.nikoltu.w3prep.Progress;
import com.nikoltu.w3prep.Sizes;

final class WorkingScreen extends JPanel {
	private static final long serialVersionUID = 1L;
	final JButton cancelButton = Ui.button("Cancel");

	private final JLabel percent = Ui.label("0%", Theme.bold(40f), Theme.TEXT);
	private final JLabel timeLeft = Ui.label("Estimating time…", Theme.body(13f), Theme.SOFT);
	private final JProgressBar bar = new JProgressBar(0, 1000);
	private final JLabel files = stat();
	private final JLabel size = stat();
	private final JLabel speed = stat();
	private final JPanel phaseList = Ui.column(8);
	private final List<PhaseRow> phases = new ArrayList<>();
	private final JLabel current = Ui.label(" ", Theme.mono(), Theme.MUTED);

	WorkingScreen() {
		super(new BorderLayout());
		setOpaque(false);
		final JPanel body = Ui.column(22);
		body.setBorder(BorderFactory.createEmptyBorder(22, 32, 24, 32));

		final JPanel header = new JPanel(new BorderLayout(16, 0));
		header.setOpaque(false);
		final JPanel titles = Ui.column(4);
		titles.add(Ui.title("Making the package"));
		titles.add(Ui.sub("You can keep using your PC. The game install is only read."));
		header.add(titles, BorderLayout.CENTER);
		final JPanel cancelBox = new JPanel(new BorderLayout());
		cancelBox.setOpaque(false);
		cancelBox.add(this.cancelButton, BorderLayout.NORTH);
		header.add(cancelBox, BorderLayout.EAST);
		body.add(header);

		final JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(this.percent, BorderLayout.WEST);
		final JPanel leftBox = new JPanel(new BorderLayout());
		leftBox.setOpaque(false);
		leftBox.add(this.timeLeft, BorderLayout.SOUTH);
		top.add(leftBox, BorderLayout.EAST);
		final JPanel progress = Ui.column(10);
		progress.add(top);
		this.bar.setPreferredSize(new Dimension(10, 10));
		this.bar.getAccessibleContext().setAccessibleName("Package progress");
		progress.add(this.bar);
		body.add(progress);

		final JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
		stats.setOpaque(false);
		stats.add(statBox("Files", this.files));
		stats.add(statBox("Size", this.size));
		stats.add(statBox("Speed", this.speed));
		body.add(stats);
		body.add(this.phaseList);
		final JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		body.add(spacer, ColumnLayout.FILL);
		body.add(this.current);
		add(body, BorderLayout.CENTER);
	}

	private static JLabel stat() {
		return Ui.label("–", Theme.bold(17f), Theme.TEXT);
	}

	private static JComponent statBox(final String name, final JLabel value) {
		final RoundPanel box = new RoundPanel(new ColumnLayout(2), Theme.PANEL, Theme.RULE, 12, 12, 14);
		box.add(Ui.label(name, Theme.body(12f), Theme.MUTED));
		box.add(value);
		return box;
	}

	void reset(final boolean desktopFolder) {
		this.phaseList.removeAll();
		this.phases.clear();
		this.phases.add(new PhaseRow("Check the game install", Progress.Phase.CHECK_INSTALL));
		this.phases.add(new PhaseRow("Copy and check each file against the install", Progress.Phase.COPY));
		this.phases.add(new PhaseRow("Read the package back and check every file", Progress.Phase.VERIFY));
		if (desktopFolder) {
			this.phases.add(new PhaseRow("Write the desktop folder", Progress.Phase.DESKTOP_FOLDER));
		}
		for (final PhaseRow row : this.phases) {
			this.phaseList.add(row);
		}
		this.percent.setText("0%");
		this.timeLeft.setText("Estimating time…");
		this.bar.setValue(0);
		this.files.setText("–");
		this.size.setText("–");
		this.speed.setText("–");
		this.current.setText(" ");
		this.cancelButton.setEnabled(true);
		revalidate();
		repaint();
	}

	void update(final Progress progress, final long elapsedMillis, final long bytesPerSecond) {
		final double fraction = progress.getFraction();
		final int pct = (int) Math.floor(fraction * 100);
		this.percent.setText(pct + "%");
		this.bar.setValue((int) (fraction * 1000));
		if (progress.getPhase() == Progress.Phase.DONE) {
			this.timeLeft.setText("Finishing");
		} else if ((elapsedMillis < 3000) || (fraction < 0.01)) {
			this.timeLeft.setText("Estimating time…");
		} else {
			final long left = (long) ((elapsedMillis / 1000.0) * ((1 - fraction) / fraction));
			this.timeLeft.setText(Sizes.duration(left).replace("about", "About") + " left");
		}
		this.files.setText(Sizes.count(progress.getCopiedFiles()) + " / " + Sizes.count(progress.getTotalFiles()));
		this.size.setText(Sizes.format(progress.getCopiedBytes()) + " / " + Sizes.format(progress.getTotalBytes()));
		this.speed.setText(bytesPerSecond > 0 ? Sizes.format(bytesPerSecond) + "/s" : "–");
		final String file = progress.getCurrentFile();
		this.current.setText(file == null || file.isEmpty() ? " " : file);
		for (final PhaseRow row : this.phases) {
			row.show(progress.getPhase());
		}
	}

	void setCancelling() {
		this.cancelButton.setEnabled(false);
		this.timeLeft.setText("Stopping…");
	}

	private static final class PhaseRow extends RoundPanel {
		private static final long serialVersionUID = 1L;
		private final Progress.Phase phase;
		private final JLabel name;
		private final JLabel state = Ui.label("Next", Theme.body(12f), Theme.MUTED);
		private final Dot dot = new Dot();

		PhaseRow(final String text, final Progress.Phase phase) {
			super(new BorderLayout(12, 0), Theme.PANEL_2, Theme.RULE, 12, 9, 14);
			this.phase = phase;
			this.name = Ui.label(text, Theme.body(13f), Theme.MUTED);
			add(this.dot, BorderLayout.WEST);
			add(this.name, BorderLayout.CENTER);
			add(this.state, BorderLayout.EAST);
		}

		void show(final Progress.Phase current) {
			final int order = this.phase.ordinal() - current.ordinal();
			if (order < 0 || current == Progress.Phase.DONE) {
				this.state.setText("Done");
				this.dot.set(Theme.OK, true);
				this.name.setForeground(Theme.TEXT);
				setColors(Theme.PANEL_2, Theme.RULE);
			} else if (order == 0) {
				this.state.setText("Now");
				this.dot.set(Theme.GOLD, false);
				this.name.setForeground(Theme.TEXT);
				setColors(Theme.PANEL_2, Theme.GOLD);
			} else {
				this.state.setText("Next");
				this.dot.set(Theme.EDGE, false);
				this.name.setForeground(Theme.MUTED);
				setColors(Theme.PANEL_2, Theme.RULE);
			}
		}
	}

	private static final class Dot extends JComponent {
		private static final long serialVersionUID = 1L;
		private Color color = Theme.EDGE;
		private boolean check;

		Dot() {
			setPreferredSize(new Dimension(18, 18));
		}

		void set(final Color color, final boolean check) {
			this.color = color;
			this.check = check;
			repaint();
		}

		@Override
		protected void paintComponent(final Graphics graphics) {
			final Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final int y = (getHeight() - 18) / 2;
			g.setColor(this.color);
			g.fillOval(0, y, 18, 18);
			if (this.check) {
				new Glyph(Glyph.Shape.CHECK, 12, Theme.ON_GOLD).paintIcon(this, g, 3, y + 3);
			}
			g.dispose();
		}
	}
}
