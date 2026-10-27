package com.nikoltu.w3prep;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Cli {
	private Cli() {
	}

	static final String USAGE = String.join(System.lineSeparator(),
			"W3 Data Packer " + AppInfo.fullVersion() + ", command line", "",
			"  --install <folder>   game install folder (default: found automatically)",
			"  --out <folder>       where packages go (default: Downloads)",
			"  --melee              melee only, no campaign files", "  --movies             include movies",
			"  --set de|hd          also make a graphics set package (experimental)",
			"  --desktop-folder     also write a plain folder and warsmash-data.ini",
			"  --dry-run            show what would be packed, write nothing",
			"  --send               after packing, push the Classic package with adb", "",
			"For use with your own legal copy only. Do not share the extracted files.");

	public static int run(final String[] args) {
		boolean onlyCli = true;
		for (final String arg : args) {
			onlyCli &= "--cli".equals(arg);
		}
		if (!onlyCli) {
			return runSafely(args);
		}
		System.out.println("W3 Data Packer " + AppInfo.fullVersion());
		System.out.println("No options given, so this only shows what would be packed and writes nothing.");
		System.out.println("Run it from a command prompt with --help to see all options.");
		System.out.println();
		final int code = runSafely(new String[] { "--dry-run" });
		System.out.println();
		System.out.println("Press Enter to close.");
		try {
			System.in.read();
		} catch (final IOException e) {
			return code;
		}
		return code;
	}

	private static int runSafely(final String[] args) {
		try {
			return runWith(args);
		} catch (final RuntimeException | Error e) {
			final AppLog log = AppLog.open(false);
			log.error("unexpected error", e);
			System.err.println("Unexpected error: " + e);
			if (log.getFile() != null) {
				System.err.println("Details are in " + log.getFile());
			}
			return 9;
		}
	}

	private static int runWith(final String[] args) {
		Path install = null;
		Path out = null;
		PackOptions.Content content = PackOptions.Content.FULL_CLASSIC;
		boolean movies = false;
		boolean desktopFolder = false;
		boolean dryRun = false;
		boolean send = false;
		final Set<GraphicsSet> sets = EnumSet.noneOf(GraphicsSet.class);
		try {
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
				case "--cli":
					break;
				case "--install":
					install = Paths.get(args[++i]);
					break;
				case "--out":
					out = Paths.get(args[++i]);
					break;
				case "--melee":
					content = PackOptions.Content.MELEE_ONLY;
					break;
				case "--movies":
					movies = true;
					break;
				case "--set":
					sets.add(GraphicsSet.valueOf(args[++i].toUpperCase(Locale.ROOT)));
					break;
				case "--desktop-folder":
					desktopFolder = true;
					break;
				case "--dry-run":
					dryRun = true;
					break;
				case "--send":
					send = true;
					break;
				case "--help":
				case "-h":
					System.out.println(USAGE);
					return 0;
				default:
					System.err.println("Unknown option: " + args[i]);
					System.err.println(USAGE);
					return 2;
				}
			}
		} catch (final ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
			System.err.println(USAGE);
			return 2;
		}

		final AppLog log = AppLog.open(true);
		final Settings settings = Settings.load();
		final PackOptions options = new PackOptions(content, movies, sets, desktopFolder);
		try {
			final Path folder = install != null ? GameInstall.resolve(install)
					: GameInstall.locate(settings.getInstallFolder());
			if (folder == null) {
				System.err.println("No game install was found. Use --install <folder>.");
				return 3;
			}
			log.info("install " + folder);
			try (Analysis analysis = Analysis.open(folder)) {
				final List<PackPlan> plans = analysis.plans(options);
				long bytes = 0;
				int files = 0;
				log.info("game version " + analysis.getVersion() + " · language " + analysis.getLocale());
				for (final String line : analysis.describeUnreadableNames()) {
					log.info(line);
				}
				for (final PackPlan plan : plans) {
					if (dryRun) {
						for (final String line : PlanReport.lines(plan)) {
							log.info(line);
						}
					} else {
						log.info(plan.getPackageName() + ": " + Sizes.count(plan.getFileCount()) + " files, "
								+ Sizes.format(plan.getTotalBytes())
								+ (plan.getSkipped().isEmpty() ? ""
										: ", " + plan.getSkipped().size() + " listed files not on this install"));
					}
					bytes += plan.getTotalBytes();
					files += plan.getFileCount();
				}
				for (final GraphicsSet set : sets) {
					if (analysis.planSet(set).getFileCount() == 0) {
						log.info(set.getLabel() + ": not on this install, skipped");
					}
				}
				if (dryRun) {
					log.info(analysis.getStorage().calibrate(plans.get(0).sampleForBlockCheck()).describe());
					log.info("dry run, nothing written");
					return 0;
				}
				final Path outputFolder = out != null ? out : GameInstall.defaultOutputFolder();
				final Progress progress = new Progress(bytes, files, desktopFolder);
				final Thread reporter = startReporter(progress);
				final Packer.Outcome outcome;
				try {
					outcome = Packer.run(folder, analysis.getStorage(), plans, outputFolder, options, progress, log);
				} finally {
					reporter.interrupt();
				}
				if (!outcome.isSuccess()) {
					log.info(outcome.getFailures().size() + " files could not be read. The package was not made.");
					return 4;
				}
				settings.setInstallFolder(folder);
				settings.save();
				log.info("ready: " + Sizes.count(outcome.getFileCount()) + " files, "
						+ Sizes.format(outcome.getByteCount()) + ", every file checked");
				for (final Path packageFile : outcome.getPackages()) {
					log.info("  " + packageFile);
				}
				log.info("Copy the ZIP to the tablet, unpack it there, then choose the unpacked folder in W3.");
				if (send) {
					return send(outcome.getPackages().get(0), log);
				}
				return 0;
			}
		} catch (final PackException e) {
			log.info(e.getMessage());
			return 5;
		} catch (final PackCancelledException e) {
			log.info("cancelled");
			return 6;
		} catch (final IOException e) {
			log.error("could not write the package: " + e.getMessage(), e);
			return 7;
		}
	}

	private static int send(final Path packageFile, final AppLog log) {
		final Adb adb = Adb.find();
		if (adb == null) {
			log.info("adb was not found. Copy the ZIP by USB instead.");
			return 8;
		}
		try {
			final List<Adb.Device> ready = adb.devices().stream().filter(Adb.Device::isReady)
					.collect(java.util.stream.Collectors.toList());
			if (ready.size() != 1) {
				log.info(ready.isEmpty() ? "No authorised tablet is connected." : "More than one device is connected.");
				return 8;
			}
			log.info(Adb.pushCommand(packageFile) + " → " + ready.get(0).getName());
			adb.push(ready.get(0), packageFile, bytes -> {
			});
			log.info("sent. Unpack it on the tablet, then choose the unpacked folder in W3.");
			return 0;
		} catch (final IOException e) {
			log.info("adb: " + e.getMessage());
			return 8;
		}
	}

	private static Thread startReporter(final Progress progress) {
		final Thread thread = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					Thread.sleep(2000);
					System.out.printf(Locale.ROOT, "  %3.0f%%  %s files  %s%n", progress.getFraction() * 100,
							Sizes.count(progress.getCopiedFiles()), progress.getPhase());
				}
			} catch (final InterruptedException e) {
			}
		}, "progress");
		thread.setDaemon(true);
		thread.start();
		return thread;
	}
}
