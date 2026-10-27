package com.nikoltu.w3prep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class GameInstall {
	static final String BUILD_INFO = ".build.info";
	static final String DATA = "Data";

	private static final List<String[]> WINDOWS_LOCATIONS = Arrays.asList(
			new String[] { "Program Files (x86)", "Warcraft III" }, new String[] { "Program Files", "Warcraft III" },
			new String[] { "Games", "Warcraft III" }, new String[] { "Warcraft III" },
			new String[] { "Battle.net", "Warcraft III" },
			new String[] { "Program Files (x86)", "Battle.net", "Warcraft III" });

	private static final List<String> INNER_FOLDERS = Arrays.asList("_retail_", "_ptr_", "data", "x86_64", "x86",
			"warcraft iii.app");

	private GameInstall() {
	}

	public static Path locate(final Path lastUsed) {
		final List<Path> candidates = new ArrayList<>();
		if (lastUsed != null) {
			candidates.add(lastUsed);
		}
		if (Platform.isWindows()) {
			for (final File root : File.listRoots()) {
				addWindowsLocations(candidates, root.toPath());
			}
		} else {
			addUnixLocations(candidates);
		}
		for (final Path candidate : candidates) {
			if (isInstall(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static void addWindowsLocations(final List<Path> candidates, final Path root) {
		for (final String[] location : WINDOWS_LOCATIONS) {
			Path path = root;
			for (final String part : location) {
				path = path.resolve(part);
			}
			candidates.add(path);
		}
	}

	private static void addUnixLocations(final List<Path> candidates) {
		final Path home = Paths.get(System.getProperty("user.home"));
		if (Platform.isMac()) {
			candidates.add(Paths.get("/Applications", "Warcraft III"));
			candidates.add(home.resolve("Applications").resolve("Warcraft III"));
		}
		final List<Path> prefixes = new ArrayList<>();
		prefixes.add(home.resolve(".wine"));
		prefixes.add(home.resolve("Games").resolve("battlenet"));
		prefixes.add(home.resolve("Games").resolve("battle-net"));
		addChildren(prefixes, home.resolve(".local/share/bottles/bottles"));
		addChildren(prefixes, home.resolve(".var/app/com.usebottles.bottles/data/bottles/bottles"));
		addChildren(prefixes, home.resolve("Library/Application Support/CrossOver/Bottles"));
		for (final Path compat : new Path[] { home.resolve(".steam/steam/steamapps/compatdata"),
				home.resolve(".local/share/Steam/steamapps/compatdata") }) {
			final List<Path> games = new ArrayList<>();
			addChildren(games, compat);
			for (final Path game : games) {
				prefixes.add(game.resolve("pfx"));
			}
		}
		for (final Path prefix : prefixes) {
			addWindowsLocations(candidates, prefix.resolve("drive_c"));
		}
		final List<Path> mounts = new ArrayList<>();
		addChildren(mounts, Paths.get("/mnt"));
		addChildren(mounts, Paths.get("/media").resolve(System.getProperty("user.name", "")));
		addChildren(mounts, Paths.get("/run/media").resolve(System.getProperty("user.name", "")));
		addChildren(mounts, Paths.get("/Volumes"));
		for (final Path mount : mounts) {
			addWindowsLocations(candidates, mount);
		}
	}

	private static void addChildren(final List<Path> list, final Path folder) {
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> children = Files.list(folder)) {
			children.filter(Files::isDirectory).sorted().forEach(list::add);
		} catch (final IOException | RuntimeException e) {
			return;
		}
	}

	public static String readVersion(final Path install) {
		try {
			final com.hiveworkshop.blizzard.casc.info.Info info = new com.hiveworkshop.blizzard.casc.info.Info(
					java.nio.ByteBuffer.wrap(Files.readAllBytes(install.resolve(BUILD_INFO))));
			final int versionField = info.getFieldIndex("Version");
			final int activeField = info.getFieldIndex("Active");
			if (versionField == -1) {
				return null;
			}
			for (int record = 0; record < info.getRecordCount(); record++) {
				if ((activeField == -1) || "1".equals(info.getField(record, activeField).trim())) {
					final String version = info.getField(record, versionField);
					return (version == null) || version.isEmpty() ? null : version;
				}
			}
		} catch (final IOException | RuntimeException e) {
			return null;
		}
		return null;
	}

	static boolean isInstall(final Path folder) {
		return Files.isRegularFile(folder.resolve(BUILD_INFO)) && Files.isDirectory(folder.resolve(DATA));
	}

	public static Path resolve(final Path picked) throws PackException {
		if ((picked == null) || !Files.isDirectory(picked)) {
			throw new PackException(PackException.Kind.NOT_AN_INSTALL, "This folder does not exist.");
		}
		if (isInstall(picked)) {
			return picked;
		}
		final Path parent = picked.getParent();
		final String name = picked.getFileName() == null ? "" : picked.getFileName().toString();
		if ((parent != null) && INNER_FOLDERS.contains(name.toLowerCase(Locale.ROOT)) && isInstall(parent)) {
			return parent;
		}
		if (hasOldArchives(picked)) {
			throw new PackException(PackException.Kind.NOT_AN_INSTALL,
					"This folder holds an older game version (MPQ files). This tool needs the current version, "
							+ "which keeps its files in a Data folder.");
		}
		throw new PackException(PackException.Kind.NOT_AN_INSTALL,
				"This folder has no .build.info. Pick the folder that contains Data and .build.info.");
	}

	private static boolean hasOldArchives(final Path folder) {
		try (Stream<Path> files = Files.list(folder)) {
			return files.anyMatch(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mpq"));
		} catch (final IOException e) {
			return false;
		}
	}

	public static Map<String, String> snapshot(final Path install) throws IOException {
		final Map<String, String> snapshot = new TreeMap<>();
		addToSnapshot(snapshot, install, install.resolve(BUILD_INFO));
		final Path config = install.resolve(DATA).resolve("config");
		if (Files.isDirectory(config)) {
			try (Stream<Path> files = Files.walk(config, 4)) {
				for (final Path file : (Iterable<Path>) files::iterator) {
					addToSnapshot(snapshot, install, file);
				}
			}
		}
		final Path data = install.resolve(DATA).resolve("data");
		if (Files.isDirectory(data)) {
			try (Stream<Path> files = Files.list(data)) {
				for (final Path file : (Iterable<Path>) files::iterator) {
					if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".idx")) {
						addToSnapshot(snapshot, install, file);
					}
				}
			}
		}
		return snapshot;
	}

	private static void addToSnapshot(final Map<String, String> snapshot, final Path install, final Path file)
			throws IOException {
		if (!Files.isRegularFile(file)) {
			return;
		}
		final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
		snapshot.put(install.relativize(file).toString(),
				attributes.size() + ":" + attributes.lastModifiedTime().toMillis());
	}

	public static void checkOutput(final Path outputFolder, final Path install, final long neededBytes)
			throws PackException {
		final Path output = outputFolder.toAbsolutePath().normalize();
		if (!Files.isDirectory(output)) {
			throw new PackException(PackException.Kind.BAD_OUTPUT, "This folder does not exist. Pick another one.");
		}
		final Path game = install.toAbsolutePath().normalize();
		if (output.startsWith(game)) {
			throw new PackException(PackException.Kind.BAD_OUTPUT,
					"The package can't be saved inside the game install. Pick another folder.");
		}
		for (Path folder = output; folder != null; folder = folder.getParent()) {
			if (Files.exists(folder.resolve(".git"))) {
				throw new PackException(PackException.Kind.BAD_OUTPUT,
						"The package can't be saved inside a git project. Game files must stay outside it.");
			}
		}
		final long free = freeSpace(output);
		final long needed = (long) (neededBytes * 1.05);
		if ((free >= 0) && (free < needed)) {
			throw new PackException(PackException.Kind.NOT_ENOUGH_SPACE,
					"Needs " + Sizes.format(needed) + ", " + Sizes.format(free) + " free on " + driveName(output)
							+ ". Choose another place for the package, or free some space.");
		}
	}

	public static long freeSpace(final Path folder) {
		try {
			return Files.getFileStore(folder).getUsableSpace();
		} catch (final IOException e) {
			return -1;
		}
	}

	public static String driveName(final Path folder) {
		final Path root = folder.toAbsolutePath().getRoot();
		if (!Platform.isWindows() || (root == null)) {
			return "this disk";
		}
		final String name = root.toString();
		return name.endsWith("\\") && (name.length() > 1) ? name.substring(0, name.length() - 1) : name;
	}

	public static Path defaultOutputFolder() {
		final Path home = Paths.get(System.getProperty("user.home"));
		final Path downloads = home.resolve("Downloads");
		return Files.isDirectory(downloads) ? downloads : home;
	}
}
