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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DesktopFolder {
	static final String FOLDER_NAME = "w3-data-classic";
	static final String INI_NAME = "warsmash-data.ini";
	private static final String MARKER = ".w3-datapacker";

	private DesktopFolder() {
	}

	public static Path folderIn(final Path outputFolder) {
		return outputFolder.resolve(FOLDER_NAME);
	}

	public static void checkTarget(final Path outputFolder) throws PackException {
		final Path folder = folderIn(outputFolder);
		if (Files.exists(folder) && !Files.isRegularFile(folder.resolve(MARKER))) {
			throw new PackException(PackException.Kind.BAD_OUTPUT, "A folder named " + FOLDER_NAME
					+ " already exists there and was not made by this tool. Rename it, or pick another place.");
		}
	}

	public static Path write(final Path outputFolder, final List<Path> packages, final List<GraphicsSet> sets,
			final Progress progress) throws IOException, PackException, PackCancelledException {
		checkTarget(outputFolder);
		final Path folder = folderIn(outputFolder);
		if (Files.exists(folder)) {
			deleteTree(folder);
		}
		Files.createDirectories(folder);
		Files.write(folder.resolve(MARKER), "Made by W3 Data Packer. Personal copy, do not share.\n"
				.getBytes(StandardCharsets.UTF_8));

		final byte[] buffer = new byte[1 << 20];
		final Path root = folder.toAbsolutePath().normalize();
		for (final Path packageFile : packages) {
			try (ZipFile zip = new ZipFile(packageFile.toFile())) {
				final Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					progress.checkCancelled();
					final ZipEntry entry = entries.nextElement();
					final Path target = root.resolve(entry.getName()).normalize();
					if (!target.startsWith(root)) {
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

		final Path ini = outputFolder.resolve(INI_NAME);
		final List<String> paths = new ArrayList<>();
		paths.add(root.toString());
		for (final GraphicsSet set : sets) {
			paths.add(root.resolve(set.getFolder()).toString());
			paths.add(root.resolve(set.getFolder()).resolve("_locales").resolve("enus.w3mod").toString());
		}
		try (BufferedWriter out = Files.newBufferedWriter(ini, StandardCharsets.UTF_8)) {
			out.write(iniText(paths));
		}
		return ini;
	}

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
