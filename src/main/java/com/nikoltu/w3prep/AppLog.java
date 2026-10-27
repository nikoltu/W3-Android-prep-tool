package com.nikoltu.w3prep;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class AppLog {
	private static final DateTimeFormatter FILE_NAME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private final Writer writer;
	private final Path file;
	private final boolean echo;

	private AppLog(final Writer writer, final Path file, final boolean echo) {
		this.writer = writer;
		this.file = file;
		this.echo = echo;
	}

	public static AppLog open(final boolean echo) {
		try {
			final Path folder = AppInfo.logFolder();
			Files.createDirectories(folder);
			final Path file = folder.resolve("packer-" + LocalDateTime.now().format(FILE_NAME) + ".log");
			return new AppLog(Files.newBufferedWriter(file, StandardCharsets.UTF_8), file, echo);
		} catch (final IOException e) {
			return new AppLog(null, null, echo);
		}
	}

	public Path getFile() {
		return this.file;
	}

	public synchronized void info(final String message) {
		final String line = LocalTime.now().truncatedTo(ChronoUnit.SECONDS) + " " + shorten(message);
		if (this.echo) {
			System.out.println(line);
		}
		if (this.writer != null) {
			try {
				this.writer.write(line);
				this.writer.write(System.lineSeparator());
				this.writer.flush();
			} catch (final IOException e) {
			}
		}
	}

	public void error(final String message, final Throwable error) {
		final StringWriter trace = new StringWriter();
		error.printStackTrace(new PrintWriter(trace));
		info(message + System.lineSeparator() + trace);
	}

	static String shorten(final String message) {
		final String home = System.getProperty("user.home");
		return (home == null) || home.length() < 3 ? message : message.replace(home, "~");
	}
}
