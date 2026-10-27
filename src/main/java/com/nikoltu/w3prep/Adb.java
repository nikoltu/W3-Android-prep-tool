package com.nikoltu.w3prep;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

public final class Adb {
	public static final String REMOTE_FOLDER = "/sdcard/Download/";

	public static final class Device {
		private final String serial;
		private final String state;
		private final String model;

		Device(final String serial, final String state, final String model) {
			this.serial = serial;
			this.state = state;
			this.model = model;
		}

		public String getSerial() {
			return this.serial;
		}

		public String getState() {
			return this.state;
		}

		public String getName() {
			return this.model == null ? this.serial : this.model;
		}

		public boolean isReady() {
			return "device".equals(this.state);
		}
	}

	private final Path executable;

	private Adb(final Path executable) {
		this.executable = executable;
	}

	public static Adb find() {
		final String name = isWindows() ? "adb.exe" : "adb";
		final List<Path> candidates = new ArrayList<>();
		final String path = System.getenv("PATH");
		if (path != null) {
			for (final String folder : path.split(File.pathSeparator)) {
				if (!folder.isEmpty()) {
					candidates.add(Paths.get(folder.replace("\"", "")).resolve(name));
				}
			}
		}
		for (final String variable : Arrays.asList("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
			final String value = System.getenv(variable);
			if (value != null) {
				candidates.add(Paths.get(value, "platform-tools", name));
			}
		}
		final String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null) {
			candidates.add(Paths.get(localAppData, "Android", "Sdk", "platform-tools", name));
		}
		final String home = System.getProperty("user.home");
		candidates.add(Paths.get(home, "Library", "Android", "sdk", "platform-tools", name));
		candidates.add(Paths.get(home, "Android", "Sdk", "platform-tools", name));
		candidates.add(Paths.get("/opt/homebrew/bin", name));
		candidates.add(Paths.get("/usr/local/bin", name));
		candidates.add(Paths.get("/usr/bin", name));
		for (final Path candidate : candidates) {
			try {
				if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
					return new Adb(candidate);
				}
			} catch (final RuntimeException e) {
			}
		}
		return null;
	}

	public Path getExecutable() {
		return this.executable;
	}

	public List<Device> devices() throws IOException {
		return parseDevices(run(10, "devices", "-l"));
	}

	static List<Device> parseDevices(final String output) {
		final List<Device> devices = new ArrayList<>();
		for (final String rawLine : output.split("\\R")) {
			final String line = rawLine.trim();
			if (line.isEmpty() || line.startsWith("List of devices") || line.startsWith("*")) {
				continue;
			}
			final String[] parts = line.split("\\s+");
			if (parts.length < 2) {
				continue;
			}
			String model = null;
			for (final String part : parts) {
				if (part.startsWith("model:")) {
					model = part.substring("model:".length()).replace('_', '-');
				}
			}
			devices.add(new Device(parts[0], parts[1], model));
		}
		return devices;
	}

	public static String pushCommand(final Path file) {
		return "adb push " + file.getFileName() + " " + REMOTE_FOLDER;
	}

	public void push(final Device device, final Path file, final LongConsumer progress) throws IOException {
		final String remote = REMOTE_FOLDER + file.getFileName();
		final long size = Files.size(file);
		final Process process = new ProcessBuilder(this.executable.toString(), "-s", device.getSerial(), "push",
				file.toString(), remote).redirectErrorStream(true).start();
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final Thread reader = new Thread(() -> copy(process.getInputStream(), output), "adb-output");
		reader.setDaemon(true);
		reader.start();
		try {
			while (!process.waitFor(1, TimeUnit.SECONDS)) {
				progress.accept(remoteSize(device, remote));
			}
			reader.join(2000);
		} catch (final InterruptedException e) {
			process.destroy();
			Thread.currentThread().interrupt();
			throw new IOException("stopped");
		}
		if (process.exitValue() != 0) {
			throw new IOException(lastLine(output.toString(StandardCharsets.UTF_8)));
		}
		final long arrived = remoteSize(device, remote);
		progress.accept(arrived);
		if (arrived != size) {
			throw new IOException("the file on the tablet has " + arrived + " bytes, expected " + size);
		}
	}

	private long remoteSize(final Device device, final String remote) {
		try {
			return Long.parseLong(run(10, "-s", device.getSerial(), "shell", "stat", "-c", "%s", remote).trim());
		} catch (final IOException | NumberFormatException e) {
			return -1;
		}
	}

	private String run(final int timeoutSeconds, final String... args) throws IOException {
		final List<String> command = new ArrayList<>();
		command.add(this.executable.toString());
		command.addAll(Arrays.asList(args));
		final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final Thread reader = new Thread(() -> copy(process.getInputStream(), output), "adb-output");
		reader.setDaemon(true);
		reader.start();
		try {
			if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				throw new IOException("adb did not answer");
			}
			reader.join(2000);
		} catch (final InterruptedException e) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new IOException("stopped");
		}
		return output.toString(StandardCharsets.UTF_8);
	}

	private static void copy(final InputStream in, final ByteArrayOutputStream out) {
		final byte[] buffer = new byte[4096];
		try {
			int read;
			while ((read = in.read(buffer)) != -1) {
				synchronized (out) {
					out.write(buffer, 0, read);
				}
			}
		} catch (final IOException e) {
		}
	}

	private static String lastLine(final String output) {
		final String[] lines = output.trim().split("\\R");
		return lines.length == 0 ? "adb failed" : lines[lines.length - 1];
	}

	static boolean isWindows() {
		return Platform.isWindows();
	}
}
