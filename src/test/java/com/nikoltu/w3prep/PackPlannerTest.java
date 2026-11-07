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
		assertEquals(8, plan.size());
	}

	@Test
	void launcherWebuiAndUnusedFolders() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\webui\\webms\\mainmenu_sd.webm", "a")
				.add("war3.w3mod\\webui\\webms\\tft_orc.webm", "b").add("war3.w3mod\\webui\\webms\\spinner.webm", "c")
				.add("war3.w3mod\\webui\\campaign\\campaign-select-border.png", "d")
				.add("war3.w3mod\\webui\\loadingscreen\\campaign\\roc\\02_human\\h1.jpg", "e")
				.add("war3.w3mod\\webui\\loadingscreen\\campaign\\rebirth\\01_human\\r.jpg", "f")
				.add("war3.w3mod\\shaders\\ps\\a.bls", "g").add("war3.w3mod\\campaign\\reforged\\roc\\h.w3m", "h")
				.add("war3.w3mod\\campaign\\classic\\roc\\h.w3m", "i");
		final Map<String, PlanEntry> full = byPath(classic(storage, PackOptions.defaults()));
		assertTrue(full.containsKey("webui/webms/mainmenu_sd.webm"));
		assertTrue(full.containsKey("webui/webms/tft_orc.webm"));
		assertFalse(full.containsKey("webui/webms/spinner.webm"));
		assertTrue(full.containsKey("webui/campaign/campaign-select-border.png"));
		assertTrue(full.containsKey("webui/loadingscreen/campaign/roc/02_human/h1.jpg"));
		assertFalse(full.containsKey("webui/loadingscreen/campaign/rebirth/01_human/r.jpg"));
		assertFalse(full.containsKey("shaders/ps/a.bls"));
		assertFalse(full.containsKey("campaign/reforged/roc/h.w3m"));
		assertTrue(full.containsKey("campaign/classic/roc/h.w3m"));

		final PackPlan meleePlan = classic(storage, new PackOptions(PackOptions.Content.MELEE_ONLY, false,
				EnumSet.noneOf(GraphicsSet.class), false));
		final Map<String, PlanEntry> melee = byPath(meleePlan);
		assertTrue(melee.containsKey("webui/webms/mainmenu_sd.webm"));
		assertFalse(melee.containsKey("webui/loadingscreen/campaign/roc/02_human/h1.jpg"));
		final List<PlanEntry> entries = meleePlan.getEntries();
		assertEquals(PackPlanner.MARKER, entries.get(entries.size() - 1).getZipPath());
	}

	@Test
	void versionsBeforeReforgedAreRecognized() {
		assertTrue(Analysis.isBeforeReforged("1.30.4.11274"));
		assertTrue(Analysis.isBeforeReforged("1.31.1.12164"));
		assertFalse(Analysis.isBeforeReforged("1.32.10.18820"));
		assertFalse(Analysis.isBeforeReforged("3.0.1.24371"));
		assertFalse(Analysis.isBeforeReforged(null));
	}

	@Test
	void contentExclusions() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\movies\\intro.mp4", "movie")
				.add("war3.w3mod\\webui\\index.html", "web").add("war3.w3mod\\campaign\\human\\a.mdx", "c")
				.add("war3.w3mod\\maps\\campaign\\human01.w3m", "map").add("war3.w3mod\\maps\\(2)echo.w3x", "melee")
				.add("war3.w3mod\\sound\\dialogue\\humancampaign\\h.mp3", "line");
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
		assertFalse(melee.containsKey("sound/dialogue/humancampaign/h.mp3"));
		assertTrue(full.containsKey("sound/dialogue/humancampaign/h.mp3"));
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

	@Test
	void engineWebuiFilesAreKept() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\webui\\portraits\\p001.png", "a")
				.add("war3.w3mod\\webui\\baseframes\\war3_stone_bg_landscape_4k.png", "b")
				.add("war3.w3mod\\webui\\icons\\versus\\orc-icon-resting.png", "c")
				.add("war3.w3mod\\webui\\icons\\versus\\orc-icon-hover.png", "d")
				.add("war3.w3mod\\webui\\loadingscreen\\loadscreen_dragonlineart.png", "e")
				.add("war3.w3mod\\webui\\loadingscreen\\campaign\\crests\\wc3_map_raceicon_orc.png", "f")
				.add("war3.w3mod\\webui\\loadingscreen\\campaign\\roc_credits.jpg", "g")
				.add("war3.w3mod\\webui\\doors\\doors_left.png", "h")
				.add("war3.w3mod\\webui\\credits\\a.png", "i");
		final Map<String, PlanEntry> full = byPath(classic(storage, PackOptions.defaults()));
		assertTrue(full.containsKey("webui/portraits/p001.png"));
		assertTrue(full.containsKey("webui/baseframes/war3_stone_bg_landscape_4k.png"));
		assertTrue(full.containsKey("webui/icons/versus/orc-icon-resting.png"));
		assertFalse(full.containsKey("webui/icons/versus/orc-icon-hover.png"));
		assertTrue(full.containsKey("webui/loadingscreen/loadscreen_dragonlineart.png"));
		assertTrue(full.containsKey("webui/loadingscreen/campaign/crests/wc3_map_raceicon_orc.png"));
		assertTrue(full.containsKey("webui/loadingscreen/campaign/roc_credits.jpg"));
		assertFalse(full.containsKey("webui/doors/doors_left.png"));
		assertFalse(full.containsKey("webui/credits/a.png"));

		final Map<String, PlanEntry> melee = byPath(classic(storage, new PackOptions(PackOptions.Content.MELEE_ONLY,
				false, EnumSet.noneOf(GraphicsSet.class), false)));
		assertTrue(melee.containsKey("webui/loadingscreen/campaign/crests/wc3_map_raceicon_orc.png"));
		assertFalse(melee.containsKey("webui/loadingscreen/campaign/roc_credits.jpg"));
	}

	@Test
	void dataTheEngineDoesNotReadIsLeftOut() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\cinematics\\a0m1\\a.mdx", "a")
				.add("war3.w3mod\\units\\human\\fx.pkb", "b").add("war3.w3mod\\units\\human\\fx.pkmm", "c")
				.add("war3.w3mod\\collection\\collection.json", "d").add("war3.w3mod\\battlenet\\bn.ini", "e")
				.add("war3.w3mod\\achievement\\a.json", "f").add("war3.w3mod\\editor\\t\\c.pkcf", "g")
				.add("war3.w3mod\\fonts\\ulei00m.ttf", "h").add("war3.w3mod\\fonts\\frizqt__.ttf", "i")
				.add("war3.w3mod\\fonts\\nimrodmt.ttf", "j").add("war3.w3mod\\filealiases.json", "k");
		final Map<String, PlanEntry> full = byPath(classic(storage, PackOptions.defaults()));
		assertFalse(full.containsKey("cinematics/a0m1/a.mdx"));
		assertFalse(full.containsKey("units/human/fx.pkb"));
		assertFalse(full.containsKey("units/human/fx.pkmm"));
		assertFalse(full.containsKey("collection/collection.json"));
		assertFalse(full.containsKey("battlenet/bn.ini"));
		assertFalse(full.containsKey("achievement/a.json"));
		assertFalse(full.containsKey("editor/t/c.pkcf"));
		assertFalse(full.containsKey("fonts/ulei00m.ttf"));
		assertTrue(full.containsKey("fonts/frizqt__.ttf"));
		assertTrue(full.containsKey("fonts/nimrodmt.ttf"));
		assertTrue(full.containsKey("filealiases.json"));
	}

	@Test
	void eastAsianLanguagesKeepTheirFonts() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\fonts\\ulei00m.ttf", "h")
				.add("war3.w3mod\\_locales\\kokr.w3mod\\ui\\a.txt", "ko");
		final PackPlanner planner = new PackPlanner(storage.listEntries());
		assertEquals("kokr", planner.getLocale());
		assertTrue(byPath(planner.planClassic(PackOptions.defaults())).containsKey("fonts/ulei00m.ttf"));
	}

	@Test
	void moviesComeInOneWidthWithTheirSoundAndCaptions() throws Exception {
		final FakeStorage storage = FakeStorage.classic().add("war3.w3mod\\movies\\humaned_640.avi", "a")
				.add("war3.w3mod\\movies\\humaned_1280.avi", "b").add("war3.w3mod\\movies\\humaned_1920.avi", "c")
				.add("war3.w3mod\\movies\\audio\\blizzardlogo.ogg", "d")
				.add("war3.w3mod\\_locales\\enus.w3mod\\movies\\audio\\humaned.ogg", "e")
				.add("war3.w3mod\\_locales\\enus.w3mod\\movies\\captions\\humaned.srt", "f")
				.add("war3.w3mod\\_locales\\dede.w3mod\\movies\\audio\\humaned.ogg", "g")
				.add("war3.w3mod\\_teen.w3mod\\movies\\humaned_1920.avi", "h");
		assertFalse(byPath(classic(storage, PackOptions.defaults())).containsKey("movies/humaned_1920.avi"));
		final Map<String, PlanEntry> movies = byPath(classic(storage, new PackOptions(
				PackOptions.Content.FULL_CLASSIC, true, EnumSet.noneOf(GraphicsSet.class), false)));
		assertTrue(movies.containsKey("movies/humaned_1920.avi"));
		assertFalse(movies.containsKey("movies/humaned_1280.avi"));
		assertFalse(movies.containsKey("movies/humaned_640.avi"));
		assertTrue(movies.containsKey("movies/audio/blizzardlogo.ogg"));
		assertEquals("_locales\\enus", movies.get("movies/audio/humaned.ogg").getLayer());
		assertTrue(movies.containsKey("movies/captions/humaned.srt"));
		final Map<String, PlanEntry> small = byPath(classic(storage, new PackOptions(PackOptions.Content.FULL_CLASSIC,
				true, 640, EnumSet.noneOf(GraphicsSet.class), false)));
		assertTrue(small.containsKey("movies/humaned_640.avi"));
		assertFalse(small.containsKey("movies/humaned_1920.avi"));
		assertThrows(IllegalArgumentException.class, () -> new PackOptions(PackOptions.Content.FULL_CLASSIC, true,
				800, EnumSet.noneOf(GraphicsSet.class), false));
	}

	@Test
	void markerNamesToolGameVersionAndLanguage() throws Exception {
		final PackPlan plan = new PackPlanner(FakeStorage.classic().listEntries(), "3.0.1.24371")
				.planClassic(PackOptions.defaults());
		final List<PlanEntry> entries = plan.getEntries();
		final PlanEntry marker = entries.get(entries.size() - 1);
		assertEquals(PackPlanner.MARKER, marker.getZipPath());
		final String text = new String(marker.getInlineContent(), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(text.startsWith("tool=Reforged Extractor toolkit "));
		assertTrue(text.contains("\ngameVersion=3.0.1.24371\n"));
		assertTrue(text.contains("\nlocale=enUS\n"));
		assertTrue(text.contains("\nentries=" + entries.size() + "\n"));
		final String unknown = new String(new PackPlanner(FakeStorage.classic().listEntries(), "unknown")
				.markerText(PackOptions.defaults(), 1), java.nio.charset.StandardCharsets.UTF_8);
		assertFalse(unknown.contains("gameVersion"));
	}
	@Test
	void classicFallbackTakesOnlyListedPackageFiles() throws Exception {
		final FakeStorage storage = FakeStorage.classic()
				.add("war3.w3mod\\_de.w3mod\\environment\\weather\\rainheavy.mdx", "listed")
				.add("war3.w3mod\\_de.w3mod\\units\\human\\footman\\footman.mdx", "not listed")
				.add("war3.w3mod\\_hd.w3mod\\environment\\weather\\rainheavy.mdx", "other package");
		final Map<String, PlanEntry> plan = byPath(classic(storage, PackOptions.defaults()));
		assertEquals("_de.w3mod", plan.get("_de.w3mod/environment/weather/rainheavy.mdx").getLayer());
		assertFalse(plan.containsKey("_de.w3mod/units/human/footman/footman.mdx"));
		assertFalse(plan.containsKey("_hd.w3mod/environment/weather/rainheavy.mdx"));
		assertTrue(PackPlanner.classicFallback().size() > 50);
	}
}
