package com.nikoltu.w3prep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class PackPlannerTest {
	private static Map<String, PlanEntry> byPath(final PackPlan plan) {
		return plan.getEntries().stream().collect(Collectors.toMap(PlanEntry::getZipPath, entry -> entry));
	}

	private static PackPlan classic(final FakeStorage storage, final PackOptions options) throws PackException {
		return new PackPlanner(storage.listEntries()).planClassic(options);
	}

	@Test
	void laterLayerWins() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\ui\\strings.txt", "root")
				.add("war3.w3mod\\_deprecated.w3mod\\ui\\strings.txt", "deprecated")
				.add("war3.w3mod\\_locales\\enus.w3mod\\ui\\strings.txt", "english")
				.add("war3.w3mod\\_deprecated.w3mod\\units\\old.mdx", "old");
		final Map<String, PlanEntry> plan = byPath(classic(storage, PackOptions.defaults()));
		assertEquals("war3.w3mod\\_locales\\enus.w3mod\\ui\\strings.txt", plan.get("ui/strings.txt").getSourcePath());
		assertEquals("_locales\\enus", plan.get("ui/strings.txt").getLayer());
		assertEquals("_deprecated", plan.get("units/old.mdx").getLayer());
	}

	@Test
	void notDownloadedLayerFallsThrough() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\ui\\help.txt", "root")
				.addRemote("war3.w3mod\\_locales\\enus.w3mod\\ui\\help.txt")
				.addRemote("war3.w3mod\\ui\\remote-only.txt");
		final PackPlan plan = classic(storage, PackOptions.defaults());
		assertEquals("root", byPath(plan).get("ui/help.txt").getLayer());
		assertFalse(byPath(plan).containsKey("ui/remote-only.txt"));
		assertEquals(List.of("ui\\remote-only.txt"), plan.getSkipped());
	}

	@Test
	void subModsStayOutOfClassicButTilesetsAndBalanceStay() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\_hd.w3mod\\units\\a.mdx", "hd")
				.add("war3.w3mod\\_de.w3mod\\units\\a.mdx", "de").add("war3.w3mod\\_teen.w3mod\\units\\a.mdx", "teen")
				.add("war3.w3mod\\_locales\\dede.w3mod\\ui\\x.txt", "german")
				.add("war3.w3mod\\_locales\\enus.w3mod\\ui\\y.txt", "english")
				.add("war3.w3mod\\_tilesets\\l.w3mod\\terrainart\\t.blp", "tileset")
				.add("war3.w3mod\\_balance\\melee_v0.w3mod\\units\\b.slk", "balance");
		final Map<String, PlanEntry> plan = byPath(classic(storage, PackOptions.defaults()));
		assertTrue(plan.containsKey("_tilesets/l.w3mod/terrainart/t.blp"));
		assertTrue(plan.containsKey("_balance/melee_v0.w3mod/units/b.slk"));
		assertFalse(plan.keySet().stream().anyMatch(path -> path.startsWith("_hd") || path.startsWith("_de")
				|| path.startsWith("_teen") || path.startsWith("_locales")));
		assertTrue(plan.containsKey(".nomedia"));
		assertTrue(plan.containsKey("ui/y.txt"));
		assertFalse(plan.containsKey("ui/x.txt"));
		assertEquals(7, plan.size());
	}

	@Test
	void contentExclusions() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\movies\\intro.mp4", "movie")
				.add("war3.w3mod\\webui\\index.html", "web").add("war3.w3mod\\campaign\\human\\a.mdx", "c")
				.add("war3.w3mod\\maps\\campaign\\human01.w3m", "map").add("war3.w3mod\\maps\\(2)echo.w3x", "melee");
		final Map<String, PlanEntry> full = byPath(classic(storage, PackOptions.defaults()));
		assertFalse(full.containsKey("movies/intro.mp4"));
		assertFalse(full.containsKey("webui/index.html"));
		assertTrue(full.containsKey("campaign/human/a.mdx"));
		assertTrue(full.containsKey("maps/campaign/human01.w3m"));

		final Map<String, PlanEntry> melee = byPath(classic(storage, new PackOptions(PackOptions.Content.MELEE_ONLY,
				true, EnumSet.noneOf(GraphicsSet.class), false)));
		assertTrue(melee.containsKey("movies/intro.mp4"));
		assertFalse(melee.containsKey("webui/index.html"));
		assertFalse(melee.containsKey("campaign/human/a.mdx"));
		assertFalse(melee.containsKey("maps/campaign/human01.w3m"));
		assertTrue(melee.containsKey("maps/(2)echo.w3x"));
	}

	@Test
	void soundInHigherLayerHidesOtherExtensionBelow() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\sound\\a.ogg", "ogg")
				.add("war3.w3mod\\_locales\\enus.w3mod\\sound\\a.flac", "flac")
				.add("war3.w3mod\\sound\\b.ogg", "only");
		final Map<String, PlanEntry> plan = byPath(classic(storage, PackOptions.defaults()));
		assertTrue(plan.containsKey("sound/a.flac"));
		assertFalse(plan.containsKey("sound/a.ogg"));
		assertTrue(plan.containsKey("sound/b.ogg"));
	}

	@Test
	void pathsAreNormalizedAndSorted() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod:Units\\Zeta.MDX", "z");
		final PackPlan plan = classic(storage, PackOptions.defaults());
		final List<String> paths = plan.getEntries().stream().map(PlanEntry::getZipPath).collect(Collectors.toList());
		assertTrue(paths.contains("units/zeta.mdx"));
		assertEquals(paths.stream().sorted().collect(Collectors.toList()), paths);
		assertEquals("war3.w3mod:Units\\Zeta.MDX", byPath(plan).get("units/zeta.mdx").getSourcePath());
	}

	@Test
	void otherLanguageIsUsedWhenEnglishIsNotDownloaded() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\ui\\strings.txt", "root")
				.add("war3.w3mod\\_locales\\dede.w3mod\\ui\\strings.txt", "deutsch")
				.add("war3.w3mod\\_locales\\dede.w3mod\\sound\\a.ogg", "hallo")
				.addRemote("war3.w3mod\\_locales\\enus.w3mod\\ui\\strings.txt")
				.add("war3.w3mod\\_locales\\frfr.w3mod\\ui\\strings.txt", "francais");
		final PackPlanner planner = new PackPlanner(storage.listEntries());
		assertEquals("dede", planner.getLocale());
		assertEquals("deDE", PackPlanner.displayLocale(planner.getLocale()));
		final Map<String, PlanEntry> plan = byPath(planner.planClassic(PackOptions.defaults()));
		assertEquals("_locales\\dede", plan.get("ui/strings.txt").getLayer());
		assertTrue(plan.containsKey("sound/a.ogg"));
	}

	@Test
	void englishWinsWhenDownloaded() {
		final FakeStorage storage = FakeStorage.classic()
				.add("war3.w3mod\\_locales\\dede.w3mod\\ui\\a.txt", "1")
				.add("war3.w3mod\\_locales\\dede.w3mod\\ui\\b.txt", "2")
				.add("war3.w3mod\\_locales\\enus.w3mod\\ui\\a.txt", "3");
		assertEquals("enus", new PackPlanner(storage.listEntries()).getLocale());
		assertEquals("enus", new PackPlanner(FakeStorage.classic().listEntries()).getLocale());
	}

	@Test
	void installWithoutClassicIsRefused() {
		final FakeStorage storage = new FakeStorage().add("war3.w3mod\\units\\a.slk", "a");
		final PackException error = assertThrows(PackException.class,
				() -> classic(storage, PackOptions.defaults()));
		assertEquals(PackException.Kind.NO_CLASSIC_FILES, error.getKind());
	}

	@Test
	void graphicsSetKeepsItsFolderAndEnglishOnly() {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\_hd.w3mod\\units\\a.mdx", "hd")
				.add("war3.w3mod\\_hd.w3mod\\_locales\\enus.w3mod\\ui\\b.txt", "en")
				.add("war3.w3mod\\_hd.w3mod\\_locales\\frfr.w3mod\\ui\\b.txt", "fr")
				.add("war3.w3mod\\_hd.w3mod\\_teen.w3mod\\units\\a.mdx", "teen")
				.add("war3.w3mod\\_hd.w3mod\\_tilesets\\l.w3mod\\t.dds", "tile")
				.add("war3.w3mod\\_hd.w3mod\\movies\\m.mp4", "movie");
		final PackPlanner planner = new PackPlanner(storage.listEntries());
		final Map<String, PlanEntry> hd = byPath(planner.planSet(GraphicsSet.HD));
		assertTrue(hd.containsKey("_hd.w3mod/units/a.mdx"));
		assertTrue(hd.containsKey("_hd.w3mod/_locales/enus.w3mod/ui/b.txt"));
		assertTrue(hd.containsKey("_hd.w3mod/_tilesets/l.w3mod/t.dds"));
		assertTrue(hd.containsKey("_hd.w3mod/.nomedia"));
		assertEquals(4, hd.size());
		assertEquals(0, planner.planSet(GraphicsSet.DE).getFileCount());
	}
}
