package com.nikoltu.w3prep.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.nikoltu.w3prep.Adb;
import com.nikoltu.w3prep.AppLog;
import com.nikoltu.w3prep.Packer;
import com.nikoltu.w3prep.Sizes;

final class DoneScreen extends JPanel {
	private static final long serialVersionUID = 1L;
	final JButton showInFolderButton = Ui.button("Show in folder");
	final JButton readmeButton = Ui.link("Open README", null);
	final JButton closeButton = Ui.button("Close");

	private final WrapText summary = Ui.sub("");
	private final javax.swing.JTextField pathField = Ui.pathField("Package file");
	private final JPanel extras = Ui.column(4);
	private final JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));
	private final RoundPanel stepsCard = Ui.card();
	private final RoundPanel adbCard = Ui.card();
	private final AppLog log;
	private Path packageFile;
	private Adb adb;

	DoneScreen(final AppLog log) {
		super(new BorderLayout());
		this.log = log;
		setOpaque(false);
		final JPanel body = Ui.column(18);
		body.setBorder(BorderFactory.createEmptyBorder(22, 32, 20, 32));

		final JLabel badge = new JLabel(new Glyph(Glyph.Shape.CHECK, 40, Theme.OK, Theme.OK_BG));
		final JPanel titles = Ui.column(2);
		titles.add(Ui.title("Your tablet package is ready"));
		titles.add(this.summary);
		final JPanel header = new JPanel(new BorderLayout(14, 0));
		header.setOpaque(false);
		header.add(badge, BorderLayout.WEST);
		header.add(titles, BorderLayout.CENTER);
		body.add(header);

		body.add(Ui.fieldRow(this.pathField, this.showInFolderButton));
		body.add(this.extras);

		this.stepsCard.add(Ui.heading("Put it on the tablet"));
		addStep(1, "Connect the tablet by USB and choose File transfer.");
		addStep(2, "Copy the ZIP into the tablet's Download folder.");
		addStep(3, "Unpack it on the tablet with a file manager (Extract).");
		addStep(4, "In W3, choose the unpacked folder, the one with units and ui inside.");
		this.cards.setOpaque(false);
		this.cards.add(this.stepsCard);
		body.add(this.cards);

		body.add(Ui.notice("W3 reads a folder, not the ZIP. Unpack on the tablet, not on the PC: one file copies "
				+ "much faster than thousands.", Theme.WARN_BG, Theme.WARN));

		final JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		body.add(spacer, ColumnLayout.FILL);
		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		bottom.add(this.readmeButton, BorderLayout.WEST);
		bottom.add(this.closeButton, BorderLayout.EAST);
		body.add(bottom);
		add(body, BorderLayout.CENTER);
	}

	private void addStep(final int number, final String text) {
		final JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);
		final JLabel index = Ui.label(number + ".", Theme.bold(13f), number == 3 ? Theme.WARN : Theme.GOLD);
		index.setVerticalAlignment(JLabel.TOP);
		row.add(index, BorderLayout.WEST);
		row.add(Ui.text(text, number == 3 ? Theme.WARN : Theme.SOFT), BorderLayout.CENTER);
		this.stepsCard.add(row);
	}

	void show(final Packer.Outcome outcome) {
		this.packageFile = outcome.getPackages().get(0);
		this.summary.setText(Sizes.count(outcome.getFileCount()) + " files · " + Sizes.format(outcome.getByteCount())
				+ (outcome.isBlocksChecked() ? " · every file matched the game install and was read back"
						: " · every file was read back and checked"));
		this.pathField.setText(this.packageFile.toString());
		this.extras.removeAll();
		if (outcome.getPackages().size() > 1) {
			final String others = outcome.getPackages().subList(1, outcome.getPackages().size()).stream()
					.map(path -> path.getFileName().toString()).collect(Collectors.joining(", "));
			this.extras.add(Ui.text("Also made, in the same folder: " + others
					+ ". Unpack each one into the same folder as Classic.", Theme.SOFT));
		}
		if (outcome.getDesktopIni() != null) {
			this.extras.add(Ui.text("Desktop build: plain folder and " + outcome.getDesktopIni().getFileName()
					+ " written next to the package.", Theme.SOFT));
		}
		if (!outcome.getLeftOut().isEmpty()) {
			int notDownloaded = 0;
			for (final com.nikoltu.w3prep.ReadFailure file : outcome.getLeftOut()) {
				if (file.getReason().startsWith("not downloaded")) {
					notDownloaded++;
				}
			}
			final int unreadable = outcome.getLeftOut().size() - notDownloaded;
			final StringBuilder text = new StringBuilder();
			text.append(Sizes.count(outcome.getLeftOut().size())).append(" files were left out");
			if ((notDownloaded > 0) && (unreadable > 0)) {
				text.append(" (").append(Sizes.count(notDownloaded)).append(" not downloaded by the game yet, ")
						.append(Sizes.count(unreadable)).append(" could not be read)");
			} else if (notDownloaded > 0) {
				text.append(" because the game has not downloaded them yet");
			}
			text.append(". They are listed in the package's manifest. To include them, let the game launcher finish "
					+ "all downloads, then make the package again.");
			this.extras.add(Ui.notice(text.toString(), Theme.WARN_BG, Theme.WARN));
		}
		this.extras.setVisible(this.extras.getComponentCount() > 0);
		checkAdb();
	}

	private void checkAdb() {
		this.adbCard.removeAll();
		this.cards.remove(this.adbCard);
		this.cards.setLayout(new GridLayout(1, 1));
		revalidate();
		repaint();
		new Thread(() -> {
			final Adb found = Adb.find();
			List<Adb.Device> devices = null;
			String problem = null;
			if (found != null) {
				try {
					devices = found.devices();
				} catch (final Exception e) {
					problem = "adb did not answer.";
				}
			}
			final List<Adb.Device> result = devices;
			final String error = problem;
			SwingUtilities.invokeLater(() -> showAdb(found, result, error));
		}, "adb-check").start();
	}

	private void showAdb(final Adb found, final List<Adb.Device> devices, final String problem) {
		this.adb = found;
		if (found == null) {
			return;
		}
		this.adbCard.removeAll();
		final JPanel head = new JPanel(new BorderLayout());
		head.setOpaque(false);
		head.add(Ui.heading("Or send it over ADB"), BorderLayout.WEST);
		head.add(Ui.label("OPTIONAL", Theme.bold(10.5f), Theme.MUTED), BorderLayout.EAST);
		this.adbCard.add(head);

		final List<Adb.Device> ready = devices == null ? List.of()
				: devices.stream().filter(Adb.Device::isReady).collect(Collectors.toList());
		if (problem != null) {
			this.adbCard.add(Ui.text(problem, Theme.MUTED));
			this.adbCard.add(checkAgain());
		} else if (ready.size() == 1) {
			final Adb.Device device = ready.get(0);
			this.adbCard.add(Ui.flowOf(Ui.chip(device.getName() + " connected", Ui.ChipKind.OK)));
			this.adbCard.add(Ui.label("Runs your own adb.exe:", Theme.body(12f), Theme.MUTED));
			final RoundPanel command = new RoundPanel(new BorderLayout(), new java.awt.Color(0x0A0C10), Theme.RULE,
					10, 7, 10);
			command.add(new WrapText(Adb.pushCommand(this.packageFile), Theme.mono(), Theme.SOFT));
			this.adbCard.add(command);
			final JButton send = Ui.outlineGold("Send to tablet");
			send.addActionListener(e -> send(device, send));
			this.adbCard.add(Ui.row(0, send));
			this.adbCard.add(Ui.label("Then steps 3 and 4 on the left.", Theme.body(12f), Theme.MUTED));
		} else {
			final boolean unauthorized = devices != null
					&& devices.stream().anyMatch(device -> "unauthorized".equals(device.getState()));
			final String text = ready.size() > 1 ? "More than one device is connected. Unplug the others."
					: unauthorized ? "Allow USB debugging on the tablet when it asks."
							: "No tablet found. Connect it with USB debugging turned on.";
			this.adbCard.add(Ui.text(text, Theme.MUTED));
			this.adbCard.add(checkAgain());
		}
		this.cards.setLayout(new GridLayout(1, 2, 14, 0));
		if (this.adbCard.getParent() == null) {
			this.cards.add(this.adbCard);
		}
		revalidate();
		repaint();
	}

	private JPanel checkAgain() {
		final JButton again = Ui.button("Check again");
		again.addActionListener(e -> checkAdb());
		return Ui.row(0, again);
	}

	private void send(final Adb.Device device, final JButton send) {
		send.setEnabled(false);
		final JProgressBar bar = new JProgressBar(0, 1000);
		bar.setPreferredSize(new java.awt.Dimension(10, 8));
		final JLabel status = Ui.label("Sending…", Theme.body(12f), Theme.SOFT);
		this.adbCard.add(bar);
		this.adbCard.add(status);
		revalidate();
		final Path file = this.packageFile;
		new Thread(() -> {
			String error = null;
			try {
				final long total = Files.size(file);
				this.log.info(Adb.pushCommand(file) + " → " + device.getName());
				this.adb.push(device, file, arrived -> SwingUtilities.invokeLater(() -> {
					if (arrived >= 0) {
						bar.setValue((int) ((arrived * 1000) / Math.max(1, total)));
						status.setText("Sending… " + Sizes.format(arrived) + " / " + Sizes.format(total));
					}
				}));
				this.log.info("sent to " + device.getName());
			} catch (final Exception e) {
				error = e.getMessage();
				this.log.info("adb push failed: " + error);
			}
			final String failure = error;
			SwingUtilities.invokeLater(() -> {
				if (failure == null) {
					bar.setValue(1000);
					status.setText("Sent to " + device.getName() + ", Download folder.");
					status.setForeground(Theme.OK);
				} else {
					status.setText("<html>The tablet did not accept the file (" + failure + "). Check the cable and "
							+ "that the tablet is unlocked, then send again, or copy by USB.</html>");
					status.setForeground(Theme.ERR);
					send.setEnabled(true);
				}
				revalidate();
			});
		}, "adb-push").start();
	}

	Path getPackageFile() {
		return this.packageFile;
	}
}
