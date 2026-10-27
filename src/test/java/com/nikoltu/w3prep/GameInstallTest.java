package com.nikoltu.w3prep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameInstallTest {
	@TempDir
	Path temp;

	private Path install() throws Exception {
		final Path game = Files.createDirectories(this.temp.resolve("game"));
		Files.writeString(game.resolve(".build.info"), "x");
		Files.createDirectories(game.resolve("Data"));
		return game;
	}

	@Test
	void acceptsInstallAndCorrectsPickOneLevelTooDeep() throws Exception {
		final Path game = install();
		assertEquals(game, GameInstall.resolve(game));
		assertEquals(game, GameInstall.resolve(Files.createDirectories(game.resolve("_retail_"))));
		assertEquals(game, GameInstall.locate(game));
	}

	@Test
	void findsInstallInsideWinePrefix() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeFalse(Platform.isWindows());
		final String home = System.getProperty("user.home");
		try {
			System.setProperty("user.home", this.temp.toString());
			final Path game = Files.createDirectories(
					this.temp.resolve(".wine").resolve("drive_c").resolve("Program Files (x86)").resolve("Warcraft III"));
			Files.writeString(game.resolve(".build.info"), "x");
			Files.createDirectories(game.resolve("Data"));
			assertEquals(game, GameInstall.locate(null));
		} finally {
			System.setProperty("user.home", home);
		}
	}

	@Test
	void explainsWrongFolders() throws Exception {
		final Path empty = Files.createDirectories(this.temp.resolve("empty"));
		assertTrue(assertThrows(PackException.class, () -> GameInstall.resolve(empty)).getMessage()
				.contains("Data and .build.info"));
		Files.writeString(empty.resolve("War3.mpq"), "old");
		assertTrue(assertThrows(PackException.class, () -> GameInstall.resolve(empty)).getMessage()
				.contains("older game version"));
	}

	@Test
	void refusesOutputInsideInstallOrGitProject() throws Exception {
		final Path game = install();
		final Path inside = Files.createDirectories(game.resolve("out"));
		assertEquals(PackException.Kind.BAD_OUTPUT,
				assertThrows(PackException.class, () -> GameInstall.checkOutput(inside, game, 1)).getKind());
		final Path project = Files.createDirectories(this.temp.resolve("project"));
		Files.createDirectories(project.resolve(".git"));
		final Path sub = Files.createDirectories(project.resolve("build"));
		assertTrue(assertThrows(PackException.class, () -> GameInstall.checkOutput(sub, game, 1)).getMessage()
				.contains("git project"));
		final Path fine = Files.createDirectories(this.temp.resolve("downloads"));
		GameInstall.checkOutput(fine, game, 1);
	}

	@Test
	void refusesWhenSpaceIsShort() throws Exception {
		final Path game = install();
		final Path fine = Files.createDirectories(this.temp.resolve("downloads"));
		assertEquals(PackException.Kind.NOT_ENOUGH_SPACE,
				assertThrows(PackException.class, () -> GameInstall.checkOutput(fine, game, Long.MAX_VALUE / 2))
						.getKind());
	}
}
