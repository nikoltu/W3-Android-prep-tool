package com.nikoltu.w3prep.ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.nikoltu.w3prep.Analysis;
import com.nikoltu.w3prep.AppInfo;
import com.nikoltu.w3prep.AppLog;
import com.nikoltu.w3prep.GameInstall;
import com.nikoltu.w3prep.GraphicsSet;
import com.nikoltu.w3prep.PackCancelledException;
import com.nikoltu.w3prep.PackException;
import com.nikoltu.w3prep.PackOptions;
import com.nikoltu.w3prep.PackPlan;
import com.nikoltu.w3prep.Platform;
import com.nikoltu.w3prep.Packer;
import com.nikoltu.w3prep.Progress;
import com.nikoltu.w3prep.Settings;
import com.nikoltu.w3prep.Sizes;

public final class MainWindow {
	private static final String READY = "ready";
	private static final String WORKING = "working";
	private static final String DONE = "done";
	private static final String FAILED = "failed";
	private static final double ESTIMATED_BYTES_PER_SECOND = 55e6;

	private final JFrame frame = new JFrame(AppInfo.NAME + " " + AppInfo.fullVersion());
	private final CardLayout cards = new CardLayout();
	private final JPanel screens = new JPanel(this.cards);
	private final AppLog log = AppLog.open(false);
	private final Settings settings = Settings.load();
	private final ReadyScreen ready = new ReadyScreen();
	private final WorkingScreen working = new WorkingScreen();
	private final DoneScreen done = new DoneScreen(this.log);
	private final FailedScreen failed = new FailedScreen();
	private final ConfirmOverlay overlay;

	private Analysis analysis;
	private Path outputFolder;
	private Progress progress;
	private Thread worker;
	private Timer timer;
	private boolean exitAfterStop;
	private long startedAt;
	private final Deque<long[]> speedSamples = new ArrayDeque<>();

	private MainWindow() {
		this.overlay = new ConfirmOverlay(this.frame.getRootPane());
		this.screens.setOpaque(true);
		this.screens.setBackground(Theme.GROUND);
		this.screens.add(this.ready, READY);
		this.screens.add(this.working, WORKING);
		this.screens.add(this.done, DONE);
		this.screens.add(this.failed, FAILED);
		this.frame.setContentPane(this.screens);
		this.frame.setIconImages(loadIcons());
		this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				requestClose();
			}
		});
		this.frame.setMinimumSize(new Dimension(700, 560));
		this.frame.setSize(760, 620);
		this.frame.setLocationRelativeTo(null);
		wire();
	}

	public static void start() {
		SwingUtilities.invokeLater(() -> {
			Theme.install();
			final MainWindow window = new MainWindow();
			window.frame.setVisible(true);
			window.show(READY);
			window.log.info(AppInfo.NAME + " " + AppInfo.fullVersion() + " started on " + System.getProperty("os.name")
					+ ", Java " + System.getProperty("java.version"));
			window.findInstall();
		});
	}

	private void wire() {
		final Path savedOutput = this.settings.getOutputFolder();
		this.outputFolder = (savedOutput != null) && Files.isDirectory(savedOutput) ? savedOutput
				: GameInstall.defaultOutputFolder();
		this.ready.setOutput(this.outputFolder.resolve("w3-data-classic.zip"));
		this.ready.setAdvancedOpen(this.settings.isAdvancedOpen());

		this.ready.helpButton.addActionListener(e -> openReadme());
		this.ready.logButton.addActionListener(e -> openFolder(AppInfo.logFolder()));
		this.ready.chooseInstallButton.addActionListener(e -> chooseInstall());
		this.ready.changeOutputButton.addActionListener(e -> chooseOutput());
		this.ready.advancedButton.addActionListener(e -> toggleAdvanced());
		this.ready.createButton.addActionListener(e -> startRun(false));
		this.ready.onOptionsChanged(this::updateSummary);

		this.working.cancelButton.addActionListener(e -> confirmStop(false));

		this.done.showInFolderButton.addActionListener(e -> showInFolder(this.done.getPackageFile()));
		this.done.readmeButton.addActionListener(e -> openReadme());
		this.done.closeButton.addActionListener(e -> exit());

		this.failed.copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(this.failed.getDetails() + System.lineSeparator() + "Log: "
						+ (this.log.getFile() == null ? "-" : this.log.getFile())), null));
		this.failed.backButton.addActionListener(e -> show(READY));
		this.failed.retryButton.addActionListener(e -> startRun(false));
		this.failed.skipButton.addActionListener(e -> startRun(true));
	}

	private void findInstall() {
		this.ready.showChecking(null);
		runInBackground(() -> {
			final Path found = GameInstall.locate(this.settings.getInstallFolder());
			SwingUtilities.invokeLater(() -> {
				if (found == null) {
					this.log.info("no install found in the usual places");
					this.ready.showInstallProblem(null, null, true);
				} else {
					openInstall(found);
				}
			});
		});
	}

	private void chooseInstall() {
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Choose the game install folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (this.analysis != null) {
			chooser.setCurrentDirectory(this.analysis.getInstall().toFile());
		}
		if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
			final Path picked = chooser.getSelectedFile().toPath();
			try {
				openInstall(GameInstall.resolve(picked));
			} catch (final PackException e) {
				this.log.info("picked " + picked + ": " + e.getMessage());
				closeAnalysis();
				this.ready.showInstallProblem(picked, e.getMessage(), false);
			}
		}
	}

	private void openInstall(final Path install) {
		this.ready.showChecking(install);
		closeAnalysis();
		runInBackground(() -> {
			try {
				final Analysis opened = Analysis.open(install);
				opened.planClassic(PackOptions.defaults());
				SwingUtilities.invokeLater(() -> {
					this.analysis = opened;
					this.settings.setInstallFolder(install);
					this.settings.save();
					this.log.info("install " + install + ", version " + opened.getVersion() + ", language " + opened.getLocale());
					for (final String line : opened.describeUnreadableNames()) {
						this.log.info(line);
					}
					this.ready.showFound(install, opened.getVersion(), opened.getLocale());
					updateSizes();
					updateSummary();
				});
			} catch (final PackException e) {
				this.log.error("install " + install + ": " + e.getMessage(), e.getCause() == null ? e : e.getCause());
				SwingUtilities.invokeLater(() -> this.ready.showInstallProblem(install, e.getMessage(), false));
			}
		});
	}

	private void closeAnalysis() {
		if (this.analysis != null) {
			try {
				this.analysis.close();
			} catch (final IOException e) {
			}
			this.analysis = null;
		}
	}

	private void toggleAdvanced() {
		final boolean open = !this.ready.isAdvancedOpen();
		this.ready.setAdvancedOpen(open);
		this.settings.setAdvancedOpen(open);
		this.settings.save();
		if (open && (this.frame.getHeight() < 900)) {
			final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			this.frame.setSize(this.frame.getWidth(), Math.min(980, screen.height - 40));
			if (this.frame.getY() + this.frame.getHeight() > screen.y + screen.height) {
				this.frame.setLocation(this.frame.getX(), Math.max(screen.y, (screen.y + screen.height)
						- this.frame.getHeight()));
			}
		}
	}

	private void updateSizes() {
		try {
			final PackPlan full = this.analysis.planClassic(PackOptions.defaults());
			final PackPlan melee = this.analysis.planClassic(new PackOptions(PackOptions.Content.MELEE_ONLY, false,
					java.util.EnumSet.noneOf(GraphicsSet.class), false));
			final PackPlan withMovies = this.analysis.planClassic(new PackOptions(PackOptions.Content.FULL_CLASSIC,
					true, java.util.EnumSet.noneOf(GraphicsSet.class), false));
			this.ready.setSizes(Sizes.format(full.getTotalBytes()), Sizes.format(melee.getTotalBytes()),
					"+" + Sizes.format(withMovies.getTotalBytes() - full.getTotalBytes()));
		} catch (final PackException e) {
			this.ready.setSizes("", "", "");
		}
		for (final GraphicsSet set : GraphicsSet.values()) {
			final PackPlan plan = this.analysis.planSet(set);
			this.ready.setSetAvailability(set, plan.getFileCount() > 0, Sizes.format(plan.getTotalBytes()));
		}
	}

	private void updateSummary() {
		this.ready.setPackageError(null);
		if (this.analysis == null) {
			return;
		}
		final PackOptions options = this.ready.readOptions();
		try {
			final List<PackPlan> plans = this.analysis.plans(options);
			final PackPlan classic = plans.get(0);
			long bytes = 0;
			for (final PackPlan plan : plans) {
				bytes += plan.getTotalBytes();
			}
			final StringBuilder text = new StringBuilder();
			text.append(options.getContent() == PackOptions.Content.FULL_CLASSIC ? "Full Classic" : "Melee only");
			if (options.isMovies()) {
				text.append(" + movies");
			}
			text.append(" · ").append(Sizes.count(classic.getFileCount())).append(" files · ")
					.append(Sizes.format(classic.getTotalBytes()));
			for (int i = 1; i < plans.size(); i++) {
				text.append(" · + ").append(plans.get(i).getLabel()).append(" package");
			}
			final long seconds = (long) ((bytes * 1.15) / ESTIMATED_BYTES_PER_SECOND);
			text.append(" · ").append(Sizes.duration(seconds));
			final long free = GameInstall.freeSpace(this.outputFolder);
			if (free >= 0) {
				text.append(" · ").append(Sizes.format(free)).append(" free on ")
						.append(GameInstall.driveName(this.outputFolder));
			}
			this.ready.setSummary(text.toString());
			this.ready.createButton.setEnabled(true);
		} catch (final PackException e) {
			this.ready.setSummary("");
			this.ready.setPackageError(e.getMessage());
			this.ready.createButton.setEnabled(false);
		}
	}

	private void chooseOutput() {
		final JFileChooser chooser = new JFileChooser(this.outputFolder.toFile());
		chooser.setDialogTitle("Choose where to save the package");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
			this.outputFolder = chooser.getSelectedFile().toPath();
			this.settings.setOutputFolder(this.outputFolder);
			this.settings.save();
			this.ready.setOutput(this.outputFolder.resolve("w3-data-classic.zip"));
			updateSummary();
		}
	}

	private void startRun(final boolean allowMissing) {
		if (this.analysis == null) {
			show(READY);
			return;
		}
		final PackOptions options = this.ready.readOptions();
		final List<PackPlan> plans;
		try {
			plans = this.analysis.plans(options);
			long bytes = 0;
			for (final PackPlan plan : plans) {
				bytes += plan.getTotalBytes();
			}
			GameInstall.checkOutput(this.outputFolder, this.analysis.getInstall(),
					options.isDesktopFolder() ? bytes * 2 : bytes);
		} catch (final PackException e) {
			show(READY);
			this.ready.setPackageError(e.getMessage());
			return;
		}
		long bytes = 0;
		int files = 0;
		for (final PackPlan plan : plans) {
			bytes += plan.getTotalBytes();
			files += plan.getFileCount();
		}
		final Progress run = new Progress(bytes, files, options.isDesktopFolder());
		this.progress = run;
		this.working.reset(options.isDesktopFolder());
		show(WORKING);
		this.startedAt = System.currentTimeMillis();
		this.speedSamples.clear();
		this.timer = new Timer(100, e -> tick());
		this.timer.start();

		final Analysis current = this.analysis;
		final Path output = this.outputFolder;
		this.worker = new Thread(() -> {
			try {
				final Packer.Outcome outcome = Packer.run(current.getInstall(), current.getStorage(), plans, output,
						options, run, this.log, allowMissing);
				SwingUtilities.invokeLater(() -> finished(outcome));
			} catch (final PackCancelledException e) {
				this.log.info("cancelled, unfinished package deleted");
				SwingUtilities.invokeLater(this::stopped);
			} catch (final PackException e) {
				this.log.info(e.getMessage());
				SwingUtilities.invokeLater(() -> failedWith(e));
			} catch (final IOException | RuntimeException e) {
				this.log.error("could not write the package", e);
				SwingUtilities.invokeLater(() -> writeFailed(e));
			}
		}, "packer");
		this.worker.start();
	}

	private void tick() {
		if (this.progress == null) {
			return;
		}
		final long now = System.currentTimeMillis();
		this.speedSamples.addLast(new long[] { now, this.progress.getCopiedBytes() });
		while (!this.speedSamples.isEmpty() && (now - this.speedSamples.peekFirst()[0] > 30000)) {
			this.speedSamples.removeFirst();
		}
		long speed = 0;
		if (this.speedSamples.size() > 1) {
			final long[] first = this.speedSamples.peekFirst();
			final long[] last = this.speedSamples.peekLast();
			if (last[0] > first[0]) {
				speed = ((last[1] - first[1]) * 1000) / (last[0] - first[0]);
			}
		}
		this.working.update(this.progress, now - this.startedAt, speed);
		this.frame.setTitle(AppInfo.NAME + " · " + (int) Math.floor(this.progress.getFraction() * 100) + "%");
	}

	private void endRun() {
		if (this.timer != null) {
			this.timer.stop();
		}
		this.worker = null;
		this.progress = null;
		this.frame.setTitle(AppInfo.NAME + " " + AppInfo.fullVersion());
		this.overlay.close();
		if (this.exitAfterStop) {
			exit();
		}
	}

	private void finished(final Packer.Outcome outcome) {
		endRun();
		if (outcome.isSuccess()) {
			this.done.show(outcome);
			show(DONE);
		} else {
			this.failed.showReadFailures(outcome.getFailures());
			show(FAILED);
		}
	}

	private void stopped() {
		endRun();
		show(READY);
	}

	private void failedWith(final PackException e) {
		endRun();
		switch (e.getKind()) {
		case GAME_CHANGED:
			this.failed.showProblem("The game was updated while packing", e.getMessage(),
					"Let the game launcher finish its update, then press Try again. A full run takes a minute or two.",
					null);
			break;
		case NOT_ENOUGH_SPACE:
		case BAD_OUTPUT:
			show(READY);
			this.ready.setPackageError(e.getMessage());
			return;
		default:
			this.failed.showProblem("The package was not made", e.getMessage(),
					"Check that the drive has free space and is not removed while packing, then try again.", null);
			break;
		}
		show(FAILED);
	}

	private void writeFailed(final Exception e) {
		endRun();
		final long free = GameInstall.freeSpace(this.outputFolder);
		this.failed.showProblem("The package could not be written",
				"Writing to " + this.outputFolder + " failed (" + e.getMessage() + ").",
				"Check that the drive has free space (" + (free >= 0 ? Sizes.format(free) + " free now" : "unknown")
						+ ") and is not removed while packing. Or choose another place for the package.",
				e.toString());
		show(FAILED);
	}

	private void confirmStop(final boolean thenExit) {
		this.overlay.ask("Stop and delete the unfinished package?",
				"The half-written package is deleted. Your game install is not affected. You can start again any "
						+ "time; a full run takes a minute or two.",
				"Keep going", "Stop and delete", () -> {
					this.exitAfterStop = thenExit;
					if (this.progress != null) {
						this.progress.cancel();
						this.working.setCancelling();
					} else if (thenExit) {
						exit();
					}
				});
	}

	private void requestClose() {
		if (this.worker != null) {
			confirmStop(true);
		} else {
			exit();
		}
	}

	private void exit() {
		closeAnalysis();
		this.log.info("closed");
		this.frame.dispose();
		System.exit(0);
	}

	private void show(final String name) {
		this.cards.show(this.screens, name);
		final javax.swing.JButton main;
		switch (name) {
		case WORKING:
			main = this.working.cancelButton;
			break;
		case DONE:
			main = this.done.closeButton;
			break;
		case FAILED:
			main = this.failed.retryButton;
			break;
		default:
			main = this.ready.createButton;
			break;
		}
		this.frame.getRootPane().setDefaultButton(name.equals(WORKING) ? null : main);
		SwingUtilities.invokeLater(() -> {
			this.screens.revalidate();
			this.screens.repaint();
			main.requestFocusInWindow();
		});
	}

	private static void runInBackground(final Runnable task) {
		final Thread thread = new Thread(task, "background");
		thread.setDaemon(true);
		thread.start();
	}

	private void openReadme() {
		final Path readme = AppInfo.readme();
		if (readme == null) {
			return;
		}
		try {
			Platform.open(readme);
		} catch (final IOException | RuntimeException e) {
			showInFolder(readme);
		}
	}

	private void openFolder(final Path folder) {
		try {
			Files.createDirectories(folder);
			Platform.open(folder);
		} catch (final IOException | RuntimeException e) {
			this.log.info("could not open " + folder + ": " + e.getMessage());
		}
	}

	private void showInFolder(final Path file) {
		if (file == null) {
			return;
		}
		try {
			Platform.reveal(file);
		} catch (final IOException | RuntimeException e) {
			openFolder(file.getParent());
		}
	}

	private static List<Image> loadIcons() {
		final List<Image> icons = new ArrayList<>();
		for (final int size : new int[] { 16, 24, 32, 48, 64, 256 }) {
			try (InputStream in = MainWindow.class.getResourceAsStream("/datapacker/icon-" + size + ".png")) {
				if (in != null) {
					icons.add(ImageIO.read(in));
				}
			} catch (final IOException e) {
			}
		}
		return icons;
	}
}
