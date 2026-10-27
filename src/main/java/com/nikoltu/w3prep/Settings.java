package com.nikoltu.w3prep;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class Settings {
	private static final String INSTALL = "installFolder";
	private static final String OUTPUT = "outputFolder";
	private static final String ADVANCED_OPEN = "advancedOpen";

	private final Path file;
	private final Properties properties = new Properties();

	private Settings(final Path file) {
		this.file = file;
	}

	public static Settings load() {
		final Settings settings = new Settings(AppInfo.dataFolder().resolve("settings.properties"));
		if (Files.isRegularFile(settings.file)) {
			try (InputStream in = Files.newInputStream(settings.file)) {
				settings.properties.load(in);
			} catch (final IOException e) {
			}
		}
		return settings;
	}

	public void save() {
		try {
			Files.createDirectories(this.file.getParent());
			try (OutputStream out = Files.newOutputStream(this.file)) {
				this.properties.store(out, "W3 Data Packer");
			}
		} catch (final IOException e) {
		}
	}

	public Path getInstallFolder() {
		return path(INSTALL);
	}

	public void setInstallFolder(final Path folder) {
		this.properties.setProperty(INSTALL, folder.toString());
	}

	public Path getOutputFolder() {
		return path(OUTPUT);
	}

	public void setOutputFolder(final Path folder) {
		this.properties.setProperty(OUTPUT, folder.toString());
	}

	public boolean isAdvancedOpen() {
		return Boolean.parseBoolean(this.properties.getProperty(ADVANCED_OPEN, "false"));
	}

	public void setAdvancedOpen(final boolean open) {
		this.properties.setProperty(ADVANCED_OPEN, Boolean.toString(open));
	}

	private Path path(final String key) {
		final String value = this.properties.getProperty(key);
		if ((value == null) || value.isEmpty()) {
			return null;
		}
		try {
			return Paths.get(value);
		} catch (final RuntimeException e) {
			return null;
		}
	}
}
