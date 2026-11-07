package com.nikoltu.w3prep;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The plain folder the desktop engine and its launcher read, unpacked from the checked packages, and an engine
 * configuration naming it. The folder is written beside its final place and only then put there, so a run that stops
 * halfway leaves the earlier folder as it was.
 */
public final class DesktopFolder {
	static final String FOLDER_NAME = "w3-data-classic";
	static final String INI_NAME = "warsmash-data.ini";
	private static final String MARKER = ".reforged-extractor";
	/** Folders made by the tool under its earlier name. */
	private static final String OLD_MARKER = ".w3-datapacker";
	private static final String PART_SUFFIX = ".part";

	private DesktopFolder() {
	}

	public static Path folderIn(final Path outputFolder) {
		return outputFolder.resolve(FOLDER_NAME);
	}

	public static void checkTarget(final Path outputFolder) throws PackException {
		for (final Path folder : new Path[] { folderIn(outputFolder), partIn(outputFolder) }) {
			if (Files.exists(folder) && !isOurs(folder)) {
				throw new PackException(PackException.Kind.BAD_OUTPUT, "A folder named " + folder.getFileName()
						+ " already exists there and was not made by this tool. Rename it, or pick another place.");
			}
		}
	}

	private static Path partIn(final Path outputFolder) {
		return outputFolder.resolve(FOLDER_NAME + PART_SUFFIX);
	}

	private static boolean isOurs(final Path folder) {
		return Files.isRegularFile(folder.resolve(MARKER)) || Files.isRegularFile(folder.resolve(OLD_MARKER));
	}

	public static Path write(final Path outputFolder, final List<Path> packages, final Progress progress)
			throws IOException, PackException, PackCancelledException {
		checkTarget(outputFolder);
		final Path part = partIn(outputFolder).toAbsolutePath().normalize();
		if (Files.exists(part)) {
			deleteTree(part);
		}
		Files.createDirectories(part);
		Files.write(part.resolve(MARKER),
				("Made by " + AppInfo.NAME + ". Personal copy, do not share.\n").getBytes(StandardCharsets.UTF_8));

		final byte[] buffer = new byte[1 << 20];
		for (final Path packageFile : packages) {
			try (ZipFile zip = new ZipFile(packageFile.toFile())) {
				final Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					progress.checkCancelled();
					final ZipEntry entry = entries.nextElement();
					final Path target = part.resolve(entry.getName()).normalize();
					if (!target.startsWith(part)) {
						throw new IOException("unsafe path in package: " + entry.getName());
					}
					if (entry.isDirectory()) {
						Files.createDirectories(target);
						continue;
					}
					Files.createDirectories(target.getParent());
					try (InputStream in = zip.getInputStream(entry); OutputStream out = Files.newOutputStream(target)) {
						int read;
						while ((read = in.read(buffer)) != -1) {
							out.write(buffer, 0, read);
							progress.bytesUnpacked(read);
						}
					}
				}
			}
		}

		final Path folder = folderIn(outputFolder).toAbsolutePath().normalize();
		if (Files.exists(folder)) {
			deleteTree(folder);
		}
		Files.move(part, folder);

		// Graphics sets stay out of the list: the engine reads their _de.w3mod and _hd.w3mod folders below this one,
		// and only for what Classic lacks, as it does from an installed game and on Android.
		final Path ini = outputFolder.resolve(INI_NAME);
		try (BufferedWriter out = Files.newBufferedWriter(ini, StandardCharsets.UTF_8)) {
			out.write(iniText(Collections.singletonList(folder.toString())));
		}
		return ini;
	}

	/**
	 * Only the game data sources: the engine adds its own folders and every other setting from its default
	 * configuration.
	 */
	static String iniText(final List<String> folders) {
		final StringBuilder builder = new StringBuilder();
		builder.append("[DataSources]\r\n");
		builder.append("Count=").append(folders.size()).append("\r\n");
		for (int i = 0; i < folders.size(); i++) {
			final String index = String.format("%02d", i);
			builder.append("Type").append(index).append("=Folder\r\n");
			builder.append("Path").append(index).append("=\"").append(folders.get(i)).append("\"\r\n");
		}
		return builder.toString();
	}

	private static void deleteTree(final Path folder) throws IOException {
		Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
