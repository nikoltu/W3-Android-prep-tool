package com.nikoltu.w3prep;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AppInfo {
	public static final String NAME = "Reforged Extractor toolkit";
	private static String version;
	private static String build;

	private AppInfo() {
	}

	public static synchronized String version() {
		if (version == null) {
			version = "dev";
			build = "local";
			try (InputStream in = AppInfo.class.getResourceAsStream("/extractor/version.properties")) {
				if (in != null) {
					final Properties properties = new Properties();
					properties.load(in);
					version = properties.getProperty("version", "dev");
					build = properties.getProperty("build", "local");
				}
			} catch (final IOException e) {
				return version;
			}
		}
		return version;
	}

	public static String fullVersion() {
		final String plain = version();
		return plain + " (" + build + ")";
	}

	public static Path dataFolder() {
		final String home = System.getProperty("user.home");
		if (Platform.isWindows()) {
			final String localAppData = System.getenv("LOCALAPPDATA");
			if ((localAppData != null) && !localAppData.isEmpty()) {
				return Paths.get(localAppData, "Reforged-Extractor");
			}
			return Paths.get(home, "AppData", "Local", "Reforged-Extractor");
		}
		if (Platform.isMac()) {
			return Paths.get(home, "Library", "Application Support", "Reforged-Extractor");
		}
		final String xdg = System.getenv("XDG_DATA_HOME");
		if ((xdg != null) && !xdg.isEmpty()) {
			return Paths.get(xdg, "reforged-extractor");
		}
		return Paths.get(home, ".local", "share", "reforged-extractor");
	}

	public static Path logFolder() {
		return dataFolder().resolve("logs");
	}

	public static Path readme() {
		final String appPath = System.getProperty("jpackage.app-path");
		if (appPath != null) {
			Path folder = Paths.get(appPath).toAbsolutePath().getParent();
			for (int i = 0; (i < 4) && (folder != null); i++, folder = folder.getParent()) {
				final Path readme = folder.resolve("README.md");
				if (Files.isRegularFile(readme)) {
					return readme;
				}
			}
		}
		final Path local = Paths.get("README.md").toAbsolutePath();
		return Files.isRegularFile(local) ? local : null;
	}
}
