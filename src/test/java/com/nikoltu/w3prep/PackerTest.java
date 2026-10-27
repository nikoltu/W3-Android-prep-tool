package com.nikoltu.w3prep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackerTest {
	@TempDir
	Path temp;
	Path install;
	Path out;

	@BeforeEach
	void makeInstall() throws IOException {
		this.install = Files.createDirectories(this.temp.resolve("game"));
		Files.writeString(this.install.resolve(".build.info"), "Version!STRING:0\n1.0\n");
		Files.createDirectories(this.install.resolve("Data").resolve("data"));
		Files.writeString(this.install.resolve("Data").resolve("data").resolve("0000000001.idx"), "index");
		this.out = Files.createDirectories(this.temp.resolve("out"));
	}

	private static FakeStorage storage() {
		final byte[] texture = new byte[300_000];
		new Random(7).nextBytes(texture);
		return FakeStorage.classic().add("war3.w3mod\\units\\human\\footman.blp", texture)
				.add("war3.w3mod\\units\\human\\footman.mdx", "MDLX".repeat(5000))
				.add("war3.w3mod\\_hd.w3mod\\units\\a.mdx", "hd model");
	}

	private Packer.Outcome run(final FakeStorage storage, final PackOptions options, final Progress progress)
			throws Exception {
		return run(storage, options, progress, false);
	}

	private Packer.Outcome run(final FakeStorage storage, final PackOptions options, final Progress progress,
			final boolean allowMissing) throws Exception {
		final PackPlanner planner = new PackPlanner(storage.listEntries());
		final List<PackPlan> plans = new java.util.ArrayList<>();
		plans.add(planner.planClassic(options));
		for (final GraphicsSet set : options.getSets()) {
			plans.add(planner.planSet(set));
		}
		return Packer.run(this.install, storage, plans, this.out, options, progress, AppLog.open(false), allowMissing);
	}

	private static Progress progress() {
		return new Progress(1, 1, false);
	}

	@Test
	void writesVerifiedZipWithManifestBesideIt() throws Exception {
		final Packer.Outcome outcome = run(storage(), PackOptions.defaults(), progress());
		assertTrue(outcome.isSuccess());
		final Path zip = this.out.resolve("w3-data-classic.zip");
		assertEquals(List.of(zip), outcome.getPackages());
		assertEquals(5, outcome.getFileCount());
		assertFalse(outcome.isBlocksChecked());
		try (ZipFile file = new ZipFile(zip.toFile())) {
			assertEquals(6, file.size());
			assertEquals(0, file.getEntry(".nomedia").getSize());
			assertEquals(ZipEntry.STORED, file.getEntry("units/human/footman.blp").getMethod());
			assertEquals(ZipEntry.DEFLATED, file.getEntry("units/human/footman.mdx").getMethod());
			assertEquals("skins", new String(file.getInputStream(file.getEntry("ui/war3skins.txt")).readAllBytes(),
					StandardCharsets.UTF_8));
			assertTrue(file.stream().noneMatch(entry -> entry.getName().contains("_hd")));
		}
		final String manifest = Files.readString(this.out.resolve("w3-data-classic.manifest.json"));
		assertTrue(manifest.contains("\"personal copy, do not share\""));
		assertTrue(manifest.contains("\"path\": \"ui/war3skins.txt\""));
		assertTrue(manifest.contains("\"gameVersion\": \"9.9.9.12345\""));
		assertNoPartFiles();
	}

	@Test
	void sameInstallGivesSameBytes() throws Exception {
		run(storage(), PackOptions.defaults(), progress());
		final byte[] first = Files.readAllBytes(this.out.resolve("w3-data-classic.zip"));
		run(storage(), PackOptions.defaults(), progress());
		final byte[] second = Files.readAllBytes(this.out.resolve("w3-data-classic.zip"));
		assertTrue(java.util.Arrays.equals(first, second));
	}

	@Test
	void unreadableFileMeansNoPackageAndEarlierOneStays() throws Exception {
		run(storage(), PackOptions.defaults(), progress());
		final Path zip = this.out.resolve("w3-data-classic.zip");
		final byte[] earlier = Files.readAllBytes(zip);

		final FakeStorage broken = storage().breakFile("war3.w3mod\\units\\human\\footman.mdx")
				.breakFile("war3.w3mod\\ui\\war3skins.txt");
		final Packer.Outcome outcome = run(broken, PackOptions.defaults(), progress());
		assertFalse(outcome.isSuccess());
		assertEquals(List.of("ui/war3skins.txt", "units/human/footman.mdx"),
				outcome.getFailures().stream().map(ReadFailure::getPath).sorted().collect(Collectors.toList()));
		assertEquals("data missing from storage", outcome.getFailures().get(0).getReason());
		assertTrue(java.util.Arrays.equals(earlier, Files.readAllBytes(zip)));
		assertNoPartFiles();
	}

	@Test
	void notDownloadedFilesAreLeftOutAndListed() throws Exception {
		final Packer.Outcome outcome = run(storage().notDownloaded("war3.w3mod\\units\\human\\footman.mdx"),
				PackOptions.defaults(), progress());
		assertTrue(outcome.isSuccess());
		assertEquals(4, outcome.getFileCount());
		assertEquals(List.of("units/human/footman.mdx"),
				outcome.getLeftOut().stream().map(ReadFailure::getPath).collect(Collectors.toList()));
		try (ZipFile file = new ZipFile(this.out.resolve("w3-data-classic.zip").toFile())) {
			assertTrue(file.getEntry("units/human/footman.mdx") == null);
		}
		final String manifest = Files.readString(this.out.resolve("w3-data-classic.manifest.json"));
		assertTrue(manifest.contains("{\"path\": \"units/human/footman.mdx\", \"reason\": \"not downloaded by the game yet\"}"));
	}

	@Test
	void unreadableFilesCanBeLeftOutOnRequest() throws Exception {
		final FakeStorage broken = storage().breakFile("war3.w3mod\\ui\\war3skins.txt");
		assertFalse(run(broken, PackOptions.defaults(), progress()).isSuccess());
		final Packer.Outcome outcome = run(broken, PackOptions.defaults(), progress(), true);
		assertTrue(outcome.isSuccess());
		assertEquals(1, outcome.getLeftOut().size());
		assertEquals("data missing from storage", outcome.getLeftOut().get(0).getReason());
		try (ZipFile file = new ZipFile(this.out.resolve("w3-data-classic.zip").toFile())) {
			assertEquals(5, file.size());
		}
	}

	@Test
	void cancelDeletesUnfinishedPackage() throws IOException {
		final Progress progress = progress();
		progress.cancel();
		assertThrows(PackCancelledException.class, () -> run(storage(), PackOptions.defaults(), progress));
		assertFalse(Files.exists(this.out.resolve("w3-data-classic.zip")));
		assertNoPartFiles();
	}

	@Test
	void installChangingDuringRunDiscardsPackage() throws Exception {
		final FakeStorage storage = new FakeStorage() {
			private boolean touched;

			@Override
			public byte[] read(final String path) throws IOException {
				if (!this.touched) {
					this.touched = true;
					Files.writeString(PackerTest.this.install.resolve("Data").resolve("data").resolve("0000000002.idx"),
							"patched");
				}
				return super.read(path);
			}
		}.add("war3.w3mod\\units\\a.slk", "a").add("war3.w3mod\\ui\\b.txt", "b");
		final PackException error = assertThrows(PackException.class,
				() -> run(storage, PackOptions.defaults(), progress()));
		assertEquals(PackException.Kind.GAME_CHANGED, error.getKind());
		assertFalse(Files.exists(this.out.resolve("w3-data-classic.zip")));
		assertNoPartFiles();
	}

	@Test
	void graphicsSetGoesIntoItsOwnPackage() throws Exception {
		final PackOptions options = new PackOptions(PackOptions.Content.FULL_CLASSIC, false,
				EnumSet.of(GraphicsSet.HD), false);
		final Packer.Outcome outcome = run(storage(), options, progress());
		assertEquals(2, outcome.getPackages().size());
		try (ZipFile file = new ZipFile(this.out.resolve("w3-data-set-hd.zip").toFile())) {
			assertEquals(2, file.size());
			assertTrue(file.getEntry("_hd.w3mod/.nomedia") != null);
			assertTrue(file.getEntry("_hd.w3mod/units/a.mdx") != null);
		}
	}

	@Test
	void blockCheckResultReachesOutcome() throws Exception {
		final FakeStorage storage = new FakeStorage() {
			@Override
			public BlockCheck calibrate(final List<String> samplePaths) {
				return new BlockCheck(samplePaths.size(), 0, 0);
			}
		}.add("war3.w3mod\\units\\a.slk", "a").add("war3.w3mod\\ui\\b.txt", "b");
		assertTrue(run(storage, PackOptions.defaults(), progress()).isBlocksChecked());
	}

	@Test
	void desktopFolderUnpacksAndWritesIni() throws Exception {
		final PackOptions options = new PackOptions(PackOptions.Content.FULL_CLASSIC, false,
				EnumSet.noneOf(GraphicsSet.class), true);
		final Packer.Outcome outcome = run(storage(), options, new Progress(1, 1, true));
		final Path folder = this.out.resolve("w3-data-classic");
		assertEquals("skins", Files.readString(folder.resolve("ui").resolve("war3skins.txt")));
		final String ini = Files.readString(outcome.getDesktopIni());
		assertTrue(ini.startsWith("[DataSources]\r\nCount=1\r\nType00=Folder\r\nPath00=\""));
		run(storage(), options, new Progress(1, 1, true));
		assertTrue(Files.isRegularFile(folder.resolve("units").resolve("unitdata.slk")));
	}

	@Test
	void foreignFolderWithSameNameIsNotTouched() throws Exception {
		Files.createDirectories(this.out.resolve("w3-data-classic"));
		final PackOptions options = new PackOptions(PackOptions.Content.FULL_CLASSIC, false,
				EnumSet.noneOf(GraphicsSet.class), true);
		final PackException error = assertThrows(PackException.class,
				() -> run(storage(), options, new Progress(1, 1, true)));
		assertEquals(PackException.Kind.BAD_OUTPUT, error.getKind());
	}

	private void assertNoPartFiles() throws IOException {
		try (Stream<Path> files = Files.list(this.out)) {
			assertTrue(files.noneMatch(file -> file.toString().endsWith(".part")));
		}
	}
}
