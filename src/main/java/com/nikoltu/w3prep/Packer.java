package com.nikoltu.w3prep;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Packer {
	public static final class Outcome {
		private final List<Path> packages;
		private final List<ReadFailure> failures;
		private final int fileCount;
		private final long byteCount;
		private final Path desktopIni;
		private final boolean blocksChecked;

		public Outcome(final List<Path> packages, final List<ReadFailure> failures, final int fileCount,
				final long byteCount, final Path desktopIni, final boolean blocksChecked) {
			this.packages = packages;
			this.failures = failures;
			this.fileCount = fileCount;
			this.byteCount = byteCount;
			this.desktopIni = desktopIni;
			this.blocksChecked = blocksChecked;
		}

		public boolean isBlocksChecked() {
			return this.blocksChecked;
		}

		public boolean isSuccess() {
			return this.failures.isEmpty();
		}

		public List<Path> getPackages() {
			return this.packages;
		}

		public List<ReadFailure> getFailures() {
			return this.failures;
		}

		public int getFileCount() {
			return this.fileCount;
		}

		public long getByteCount() {
			return this.byteCount;
		}

		public Path getDesktopIni() {
			return this.desktopIni;
		}
	}

	private Packer() {
	}

	public static Outcome run(final Path install, final GameStorage storage, final List<PackPlan> plans,
			final Path outputFolder, final PackOptions options, final Progress progress, final AppLog log)
			throws PackException, PackCancelledException, IOException {
		progress.setPhase(Progress.Phase.CHECK_INSTALL);
		long totalBytes = 0;
		for (final PackPlan plan : plans) {
			totalBytes += plan.getTotalBytes();
		}
		GameInstall.checkOutput(outputFolder, install, options.isDesktopFolder() ? totalBytes * 2 : totalBytes);
		if (options.isDesktopFolder()) {
			DesktopFolder.checkTarget(outputFolder);
		}
		final Map<String, String> before = GameInstall.snapshot(install);
		final BlockCheck blockCheck = storage.calibrate(plans.get(0).sampleForBlockCheck());
		log.info(blockCheck.describe());
		log.info("game version " + storage.getVersion() + " · profile " + options.describe() + " · output "
				+ outputFolder);

		final List<Path> parts = new ArrayList<>();
		final List<PackWriter.Written> written = new ArrayList<>();
		boolean success = false;
		try {
			progress.setPhase(Progress.Phase.COPY);
			for (final PackPlan plan : plans) {
				final Path part = outputFolder.resolve(plan.getPackageName() + ".part");
				log.info(plan.getLabel() + ": writing " + plan.getFileCount() + " files, "
						+ Sizes.format(plan.getTotalBytes()) + (plan.getSkipped().isEmpty() ? ""
								: " · " + plan.getSkipped().size() + " listed files not on this install"));
				parts.add(part);
				final PackWriter.Written result = PackWriter.write(plan, storage, part, progress);
				if (!result.getFailures().isEmpty()) {
					for (final ReadFailure failure : result.getFailures()) {
						log.info("could not read " + failure);
					}
					return new Outcome(Collections.emptyList(), result.getFailures(), 0, 0, null,
							blockCheck.isEnabled());
				}
				written.add(result);
			}

			progress.setPhase(Progress.Phase.VERIFY);
			for (int i = 0; i < plans.size(); i++) {
				log.info(plans.get(i).getLabel() + ": reading the package back");
				PackWriter.verify(parts.get(i), written.get(i).getFiles(), progress);
			}

			if (!before.equals(GameInstall.snapshot(install))) {
				throw new PackException(PackException.Kind.GAME_CHANGED,
						"The game install changed during the run, so this package can't be trusted and was deleted. "
								+ "Press Try again.");
			}

			final List<Path> packages = new ArrayList<>();
			int fileCount = 0;
			long byteCount = 0;
			for (int i = 0; i < plans.size(); i++) {
				final Path target = outputFolder.resolve(plans.get(i).getPackageName());
				move(parts.get(i), target);
				packages.add(target);
				final List<WrittenFile> files = written.get(i).getFiles();
				Manifest.write(Manifest.pathFor(target), storage.getVersion(), options.describe(),
						plans.get(i).getPackageName(), files);
				for (final WrittenFile file : files) {
					if (!"tool".equals(file.getLayer())) {
						fileCount++;
						byteCount += file.getSize();
					}
				}
				log.info(plans.get(i).getLabel() + ": done, " + target);
			}

			Path ini = null;
			if (options.isDesktopFolder()) {
				progress.setPhase(Progress.Phase.DESKTOP_FOLDER);
				ini = DesktopFolder.write(outputFolder, packages, new ArrayList<>(options.getSets()), progress);
				log.info("desktop folder written, " + ini);
			}
			progress.setPhase(Progress.Phase.DONE);
			success = true;
			return new Outcome(packages, Collections.emptyList(), fileCount, byteCount, ini, blockCheck.isEnabled());
		} finally {
			if (!success) {
				for (final Path part : parts) {
					Files.deleteIfExists(part);
				}
			}
		}
	}

	private static void move(final Path from, final Path to) throws IOException {
		try {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (final AtomicMoveNotSupportedException e) {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
