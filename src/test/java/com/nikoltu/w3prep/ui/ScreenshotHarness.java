package com.nikoltu.w3prep.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.nikoltu.w3prep.AppLog;
import com.nikoltu.w3prep.GraphicsSet;
import com.nikoltu.w3prep.Packer;
import com.nikoltu.w3prep.Progress;
import com.nikoltu.w3prep.ReadFailure;

public final class ScreenshotHarness {
	private ScreenshotHarness() {
	}

	public static void main(final String[] args) throws Exception {
		final File out = new File(args[0]);
		out.mkdirs();
		SwingUtilities.invokeAndWait(Theme::install);
		final Path install = Paths.get("C:\\Program Files (x86)\\Warcraft III");
		final Path zip = Paths.get("C:\\Users\\Player\\Downloads\\w3-data-classic.zip");

		shoot(out, "1-ready", 620, () -> {
			final ReadyScreen ready = new ReadyScreen();
			ready.showFound(install, "1.36.2.21230", "enUS");
			ready.setOutput(zip);
			ready.setSummary("Full Classic · 21 627 files · 2.20 GB · about 1 min · 184 GB free on C:");
			return ready;
		}, null);
		shoot(out, "1a-advanced", 980, () -> {
			final ReadyScreen ready = new ReadyScreen();
			ready.showFound(install, "1.36.2.21230", "enUS");
			ready.setOutput(zip);
			ready.setSizes("2.20 GB", "1.79 GB", "+1.71 GB");
			ready.setSetAvailability(GraphicsSet.DE, true, "8.40 GB");
			ready.setSetAvailability(GraphicsSet.HD, false, "");
			ready.setSummary("Full Classic · 21 627 files · 2.20 GB · about 1 min · 184 GB free on C:");
			ready.setAdvancedOpen(true);
			return ready;
		}, null);
		shoot(out, "1b-not-found", 620, () -> {
			final ReadyScreen ready = new ReadyScreen();
			ready.showInstallProblem(Paths.get("E:\\Games\\Downloads"),
					"This folder has no .build.info. Pick the folder that contains Data and .build.info.", true);
			return ready;
		}, null);
		final Progress progress = new Progress(2_362_232_012L, 21627, false);
		progress.setPhase(Progress.Phase.COPY);
		for (int i = 0; i < 11800; i++) {
			progress.fileCopied(109_000);
		}
		progress.setCurrentFile("war3.w3mod\\sound\\units\\human\\footman\\footmanyes2.ogg");
		shoot(out, "2-working", 620, () -> {
			final WorkingScreen working = new WorkingScreen();
			working.reset(false);
			working.update(progress, 20_000, 58_000_000);
			return working;
		}, null);
		shoot(out, "2a-confirm", 620, () -> {
			final WorkingScreen working = new WorkingScreen();
			working.reset(false);
			working.update(progress, 20_000, 58_000_000);
			return working;
		}, frame -> new ConfirmOverlay(frame.getRootPane()).ask("Stop and delete the unfinished package?",
				"The half-written package is deleted. Your game install is not affected. You can start again any "
						+ "time; a full run takes a minute or two.",
				"Keep going", "Stop and delete", () -> {
				}));
		shoot(out, "3-done", 680, () -> {
			final DoneScreen done = new DoneScreen(AppLog.open(false));
			done.show(new Packer.Outcome(List.of(zip), List.of(), 21627, 2_362_232_012L, null, true));
			return done;
		}, null);
		shoot(out, "2b-failed", 620, () -> {
			final FailedScreen failed = new FailedScreen();
			failed.showReadFailures(List.of(new ReadFailure("units/orc/grunt/grunt.mdx", "content does not match the install"),
					new ReadFailure("sound/music/mp3music/orc1.mp3", "data missing from storage"),
					new ReadFailure("replaceabletextures/teamcolor/teamcolor03.blp", "data missing from storage"),
					new ReadFailure("a", "b"), new ReadFailure("c", "d")));
			return failed;
		}, null);
		System.exit(0);
	}

	interface Builder {
		JPanel build();
	}

	interface After {
		void run(JFrame frame);
	}

	private static void shoot(final File out, final String name, final int height, final Builder builder,
			final After after) throws Exception {
		final JFrame[] holder = new JFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			final JFrame frame = new JFrame("W3 Data Packer");
			final JPanel screen = builder.build();
			final JPanel content = new JPanel(new java.awt.BorderLayout());
			content.setBackground(Theme.GROUND);
			content.add(screen);
			frame.setContentPane(content);
			frame.setSize(760, height);
			frame.setVisible(true);
			frame.getRootPane().setFocusable(true);
			frame.getRootPane().requestFocusInWindow();
			if (after != null) {
				after.run(frame);
			}
			holder[0] = frame;
		});
		Thread.sleep(300);
		SwingUtilities.invokeAndWait(() -> holder[0].getContentPane().revalidate());
		Thread.sleep(500);
		SwingUtilities.invokeAndWait(() -> {
			final JFrame frame = holder[0];
			final java.awt.Container root = frame.getRootPane();
			final BufferedImage image = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
			final Graphics2D g = image.createGraphics();
			root.paint(g);
			g.dispose();
			try {
				ImageIO.write(image, "png", new File(out, name + ".png"));
			} catch (final java.io.IOException e) {
				throw new RuntimeException(e);
			}
			frame.dispose();
		});
	}
}
