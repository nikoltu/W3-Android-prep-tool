package com.nikoltu.w3prep;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class Platform {
	public enum Os {
		WINDOWS, MAC, LINUX
	}

	public static final Os OS = detect();

	private Platform() {
	}

	private static Os detect() {
		final String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (name.contains("win")) {
			return Os.WINDOWS;
		}
		if (name.contains("mac") || name.contains("darwin")) {
			return Os.MAC;
		}
		return Os.LINUX;
	}

	public static boolean isWindows() {
		return OS == Os.WINDOWS;
	}

	public static boolean isMac() {
		return OS == Os.MAC;
	}

	public static String fileManagerName() {
		return isMac() ? "Finder" : "Explorer";
	}

	public static void open(final Path path) throws IOException {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			try {
				Desktop.getDesktop().open(path.toFile());
				return;
			} catch (final IOException | RuntimeException e) {
				if (isWindows()) {
					throw e instanceof IOException ? (IOException) e : new IOException(e);
				}
			}
		}
		new ProcessBuilder(isMac() ? "open" : "xdg-open", path.toString()).start();
	}

	public static void reveal(final Path file) throws IOException {
		if (isWindows()) {
			new ProcessBuilder("explorer.exe", "/select,", file.toString()).start();
		} else if (isMac()) {
			new ProcessBuilder("open", "-R", file.toString()).start();
		} else {
			open(Files.isDirectory(file) ? file : file.getParent());
		}
	}
}
