package com.nikoltu.w3prep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PackPlanner {
	static final String ROOT = "war3.w3mod";
	static final String DEFAULT_LOCALE = "enus";
	static final String NOMEDIA = ".nomedia";
	static final String MARKER = "w3-datapacker.txt";
	private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList("flac", "ogg", "mp3", "wav"));
	private static final List<String> MENU_SCENES = Arrays.asList("mainmenu", "mainmenu_tft", "roc_prologue",
			"roc_human", "roc_undead", "roc_orc", "roc_nightelf", "tft_nightelf", "tft_human", "tft_undead", "tft_orc");
	/**
	 * The webui files the engine and its launchers load: the Reforged launcher skin, the Reforged loading screen and
	 * the profile portraits. The rest of webui is the Battle.net web client.
	 */
	private static final Set<String> WEBUI_FILES = new HashSet<>(Arrays.asList("webui\\common\\homescreen.jpg",
			"webui\\common\\wc3tft-logo.png", "webui\\common\\wc3roc-logo.png",
			"webui\\baseframes\\war3_metal_frame_4k.png", "webui\\baseframes\\war3_stone_bg_landscape_4k.png",
			"webui\\widgets\\buttons\\wc3_btn_blue_normal_4k.png", "webui\\widgets\\buttons\\wc3_btn_blue_down_4k.png",
			"webui\\widgets\\buttons\\wc3_btn_disabled_normal_4k.png",
			"webui\\widgets\\buttons\\wc3_btn_red_normal_4k.png", "webui\\widgets\\buttons\\wc3_btn_red_down_4k.png",
			"webui\\widgets\\buttons\\wc3_btn_green_normal_4k.png",
			"webui\\widgets\\buttons\\wc3_btn_green_down_4k.png", "webui\\widgets\\buttons\\checkbox-checked.png",
			"webui\\widgets\\buttons\\checkbox-unchecked.png",
			"webui\\widgets\\buttons\\war3_button_frame_end_left_alt.png",
			"webui\\widgets\\buttons\\war3_button_frame_end_right_alt.png",
			"webui\\widgets\\playerwidget\\portrait_frame_hover_4k.png",
			"webui\\widgets\\playerwidget\\portrait_frame_large.png", "webui\\widgets\\slider\\war3_slider_normal_4k.png",
			"webui\\widgets\\textfields\\war3_text_normal_4k.png",
			"webui\\social\\clan\\clan_settings_members_playerrow_bg.png", "webui\\modal\\errormodal\\error_icon.png",
			"webui\\campaign\\campaign-select-border.png", "webui\\campaign\\campaign-select-border_hover.png",
			"webui\\campaign\\campaign-mission-checkmark.png", "webui\\icons\\versus\\human-icon-resting.png",
			"webui\\icons\\versus\\orc-icon-resting.png", "webui\\icons\\versus\\undead-icon-resting.png",
			"webui\\icons\\versus\\nightelf-icon-resting.png", "webui\\icons\\versus\\random-icon-resting.png",
			"webui\\loadingscreen\\betaloading_screen_v2.jpg", "webui\\loadingscreen\\loadscreen_dragonlineart.png",
			"webui\\loadingscreen\\loadscreen_mapframe.png", "webui\\loadingscreen\\top_center_goth_piece.png",
			"webui\\loadingscreen\\bottom_center_goth_piece.png", "webui\\loadingscreen\\loading-bar-4k.png",
			"webui\\loadingscreen\\loadingscreen-fill-flat.png", "webui\\loadingscreen\\loadingscreen-bar-endcap_4k.png"));
	private static final List<String> WEBUI_FOLDERS = Arrays.asList("webui\\portraits\\",
			"webui\\loadingscreen\\campaign\\crests\\");
	/** The Classic campaigns' mission pictures, for the launcher's mission list and the loading screen. */
	private static final List<String> WEBUI_CAMPAIGN = Arrays.asList("webui\\loadingscreen\\campaign\\roc\\",
			"webui\\loadingscreen\\campaign\\tft\\", "webui\\loadingscreen\\campaign\\roc_credits.jpg");
	/**
	 * Read by neither the engine nor its launchers: Reforged shaders, the Reforged and Forsaken Kingdom campaigns
	 * (their campaign lists, CampaignInfoReforged.txt and CampaignInfoDLC01.txt, are not loaded) with the in-engine
	 * cinematics only they play, and the Battle.net client's data.
	 */
	private static final List<String> UNUSED_FOLDERS = Arrays.asList("shaders\\", "campaign\\reforged\\",
			"campaign\\forsakenkingdom\\", "maps\\forsakenkingdom\\", "sound\\dialogue\\humanrecamp\\",
			"sound\\dialogue\\undeadrecamp\\", "cinematics\\", "achievement\\", "battlenet\\", "collection\\",
			"editor\\");
	/** The spoken lines of the Classic campaigns, which only their maps play. */
	private static final List<String> CAMPAIGN_DIALOGUE = Arrays.asList("sound\\dialogue\\tutorialcampaign\\",
			"sound\\dialogue\\humancampaign\\", "sound\\dialogue\\orccampaign\\", "sound\\dialogue\\undeadcampaign\\",
			"sound\\dialogue\\nightelfcampaign\\", "sound\\dialogue\\humanexpcamp\\", "sound\\dialogue\\orcexpcamp\\",
			"sound\\dialogue\\undeadexpcamp\\", "sound\\dialogue\\nightelfexpcamp\\");
	/** PopcornFX particle effects of the Reforged models; the engine draws the Classic emitters. */
	private static final Set<String> UNUSED_EXTENSIONS = new HashSet<>(Arrays.asList("pkb", "pkmm", "pkcf"));
	/**
	 * Fonts only the Korean, Chinese and Japanese text uses (the skin's and chat fonts of those languages), about 110
	 * MB; every other language draws with the Latin and Cyrillic fonts.
	 */
	private static final Set<String> CJK_FONTS = new HashSet<>(Arrays.asList("2002.ttf", "2002b.ttf", "bl.ttf",
			"dfheimd.ttf", "dfheilt.ttf", "dfkgothicmd.ttf", "dffn_b31.ttf", "dfst-m3u.ttf", "arheigb_bd.ttf",
			"arheiu20m.ttf", "arheiu20_md.ttf", "arlishuu20_db.ttf", "arlishuu30_md.ttf", "ghei00b_t.ttf",
			"tt5500m_.ttf", "ulei00m.ttf"));
	private static final Set<String> CJK_LOCALES = new HashSet<>(Arrays.asList("kokr", "zhcn", "zhtw", "jajp"));

	/**
	 * Files of the graphics packages (_de.w3mod, _hd.w3mod) that the Classic data names and the Classic layers do not
	 * have: the engine shows Classic graphics but looks a file up in the packages when the Classic layers lack it, as
	 * the game does. The list is what the Classic skin fields (not the ":hd" ones), the Classic campaign maps and the
	 * Classic models resolve to, and the Reforged cinematic panel art; the textures and spawned models of the listed
	 * models are found by reading them.
	 */
	static final String CLASSIC_FALLBACK = "/extractor/classic-fallback.txt";
	/**
	 * Files of the other graphics package that a set's graphics need (set-de-extra.txt, set-hd-extra.txt): in those
	 * graphics a name the Classic layers and the set lack is looked up in the other package, so each set carries what
	 * its graphics reach there, under the same paths, and the folder needs no second set.
	 */
	private static final String SET_EXTRA = "/extractor/set-%s-extra.txt";
	/** The graphics packages in the order the engine looks a missing file up in them. */
	private static final String[] GRAPHICS_PACKAGES = { "_de.w3mod", "_hd.w3mod" };
	private static final Set<String> TEXTURE_EXTENSIONS = new HashSet<>(Arrays.asList("blp", "dds", "tga", "tif"));
	private static final String DIFFUSE_SUFFIX = "_diffuse";

	private final List<StorageEntry> entries;
	private final String gameVersion;
	private final String locale;
	private final String[] classicLayers;
	private final String[] classicLayerNames;
	private final GameStorage storage;
	/** The files each package model names, read once for all plans. */
	private final Map<String, List<String>> modelReferences = new HashMap<>();
	private Map<String, StorageEntry> packageEntries;

	public PackPlanner(final List<StorageEntry> entries) {
		this(entries, null, null);
	}

	public PackPlanner(final List<StorageEntry> entries, final String gameVersion) {
		this(entries, gameVersion, null);
	}

	/**
	 * @param storage reads the package models of the Classic package, whose textures and spawned models it then
	 *                carries too; null to take only the listed files
	 */
	public PackPlanner(final List<StorageEntry> entries, final String gameVersion, final GameStorage storage) {
		this.entries = entries;
		this.gameVersion = gameVersion;
		this.storage = storage;
		this.locale = pickLocale(entries);
		this.classicLayers = new String[] { ROOT, ROOT + "\\_deprecated.w3mod",
				ROOT + "\\_locales\\" + this.locale + ".w3mod" };
		this.classicLayerNames = new String[] { "root", "_deprecated", "_locales\\" + this.locale };
	}

	static String pickLocale(final List<StorageEntry> entries) {
		final String prefix = ROOT + "\\_locales\\";
		final Map<String, Integer> localFiles = new HashMap<>();
		for (final StorageEntry entry : entries) {
			final String path = normalize(entry.getPath());
			if (!entry.isLocal() || !path.startsWith(prefix)) {
				continue;
			}
			final String folder = firstSegment(path.substring(prefix.length()));
			if (folder.endsWith(".w3mod")) {
				localFiles.merge(folder.substring(0, folder.length() - ".w3mod".length()), 1, Integer::sum);
			}
		}
		if (localFiles.containsKey(DEFAULT_LOCALE) || localFiles.isEmpty()) {
			return DEFAULT_LOCALE;
		}
		String best = null;
		for (final Map.Entry<String, Integer> candidate : localFiles.entrySet()) {
			if ((best == null) || (candidate.getValue() > localFiles.get(best))
					|| (candidate.getValue().equals(localFiles.get(best)) && (candidate.getKey().compareTo(best) < 0))) {
				best = candidate.getKey();
			}
		}
		return best;
	}

	public String getLocale() {
		return this.locale;
	}

	public static String displayLocale(final String locale) {
		if (locale.length() != 4) {
			return locale;
		}
		return locale.substring(0, 2) + locale.substring(2).toUpperCase(Locale.ROOT);
	}

	public PackPlan planClassic(final PackOptions options) throws PackException {
		final Map<String, StorageEntry[]> byPath = new HashMap<>();
		for (final StorageEntry entry : this.entries) {
			final String path = normalize(entry.getPath());
			final int layer = classicLayerOf(path);
			if (layer == -1) {
				continue;
			}
			final String relative = path.substring(this.classicLayers[layer].length() + 1);
			if (relative.isEmpty() || isSubMod(firstSegment(relative)) || isExcluded(relative, options, this.locale)) {
				continue;
			}
			byPath.computeIfAbsent(relative, key -> new StorageEntry[this.classicLayers.length])[layer] = entry;
		}

		final Map<String, Integer> winnerLayer = new HashMap<>();
		final Map<String, StorageEntry> winner = new HashMap<>();
		final List<String> skipped = new ArrayList<>();
		for (final Map.Entry<String, StorageEntry[]> candidates : byPath.entrySet()) {
			final StorageEntry[] layers = candidates.getValue();
			for (int layer = layers.length - 1; layer >= 0; layer--) {
				if ((layers[layer] != null) && layers[layer].isLocal()) {
					winnerLayer.put(candidates.getKey(), layer);
					winner.put(candidates.getKey(), layers[layer]);
					break;
				}
			}
			if (!winner.containsKey(candidates.getKey())) {
				skipped.add(candidates.getKey());
			}
		}

		dropLosingAudio(winner, winnerLayer);

		final List<PlanEntry> planEntries = new ArrayList<>(winner.size());
		addClassicFallback(planEntries, options, skipped, winner.keySet());
		boolean hasUnits = false;
		boolean hasUi = false;
		for (final Map.Entry<String, StorageEntry> file : winner.entrySet()) {
			final String zipPath = file.getKey().replace('\\', '/');
			hasUnits |= zipPath.startsWith("units/");
			hasUi |= zipPath.startsWith("ui/");
			final int layer = winnerLayer.get(file.getKey());
			planEntries.add(new PlanEntry(zipPath, file.getValue().getPath(), this.classicLayerNames[layer],
					file.getValue().getSize()));
		}
		if (!hasUnits || !hasUi) {
			throw new PackException(PackException.Kind.NO_CLASSIC_FILES,
					"The Classic game files were not found in this install.");
		}
		planEntries.add(PlanEntry.inline(NOMEDIA, new byte[0]));
		planEntries.sort(Comparator.comparing(PlanEntry::getZipPath));
		// Last in the package, so a folder that was only partly unpacked or copied has no marker.
		planEntries.add(PlanEntry.inline(MARKER, markerText(options, planEntries.size() + 1)));
		Collections.sort(skipped);
		return new PackPlan("w3-data-classic.zip", "Classic", planEntries, skipped);
	}

	private void addClassicFallback(final List<PlanEntry> planEntries, final PackOptions options,
			final List<String> skipped, final Set<String> classicFiles) {
		final Set<String> wanted = classicFallback();
		if (wanted.isEmpty()) {
			return;
		}
		final Map<String, StorageEntry> packaged = packageEntries();
		final Set<String> taken = new HashSet<>();
		final List<String> queue = new ArrayList<>();
		for (final String relative : wanted) {
			if (packaged.containsKey(relative)) {
				queue.add(relative);
			}
		}
		while (!queue.isEmpty()) {
			final String relative = queue.remove(queue.size() - 1);
			if (!taken.add(relative)) {
				continue;
			}
			final StorageEntry entry = packaged.get(relative);
			final int slash = relative.indexOf('\\');
			if (isExcluded(relative.substring(slash + 1), options, this.locale)) {
				continue;
			}
			if (!entry.isLocal()) {
				skipped.add(relative);
				continue;
			}
			planEntries.add(new PlanEntry(relative.replace('\\', '/'), entry.getPath(), relative.substring(0, slash),
					entry.getSize()));
			for (final String reference : referencesOf(relative, entry)) {
				final String found = findInPackages(reference, classicFiles, packaged);
				if ((found != null) && !taken.contains(found)) {
					queue.add(found);
				}
			}
		}
	}

	/** The package files below war3.w3mod by their normalized path, without the packages' language folders. */
	private Map<String, StorageEntry> packageEntries() {
		if (this.packageEntries == null) {
			this.packageEntries = new HashMap<>();
			for (final StorageEntry entry : this.entries) {
				final String path = normalize(entry.getPath());
				if (!path.startsWith(ROOT + "\\")) {
					continue;
				}
				final String relative = path.substring(ROOT.length() + 1);
				for (final String graphicsPackage : GRAPHICS_PACKAGES) {
					if (relative.startsWith(graphicsPackage + "\\")
							&& !isSubMod(firstSegment(relative.substring(graphicsPackage.length() + 1)))) {
						this.packageEntries.put(relative, entry);
					}
				}
			}
		}
		return this.packageEntries;
	}

	private List<String> referencesOf(final String relative, final StorageEntry entry) {
		if ((this.storage == null) || !relative.endsWith(".mdx")) {
			return Collections.emptyList();
		}
		return this.modelReferences.computeIfAbsent(relative, key -> {
			try {
				return ModelReferences.of(this.storage.read(entry.getPath()));
			}
			catch (final IOException | RuntimeException e) {
				// The model itself is read again when packing, which reports the failure.
				return Collections.emptyList();
			}
		});
	}

	/**
	 * The package file the engine shows for a name a package model gives, as it looks the name up in Classic
	 * graphics: the Classic files first, then the packages in order, a texture also as .dds and as the "_diffuse"
	 * .dds of the new materials (the HD materials name .tif files that the data keeps as .dds). Null when the Classic
	 * files have it or no package does.
	 */
	static String findInPackages(final String reference, final Set<String> classicFiles,
			final Map<String, StorageEntry> packaged) {
		final List<String> names = referenceNames(normalize(reference.trim()));
		for (final String name : names) {
			if (classicFiles.contains(name)) {
				return null;
			}
		}
		for (final String graphicsPackage : GRAPHICS_PACKAGES) {
			for (final String name : names) {
				final String path = graphicsPackage + "\\" + name;
				if (packaged.containsKey(path)) {
					return path;
				}
			}
		}
		return null;
	}

	private static List<String> referenceNames(final String path) {
		String name = path;
		while (name.startsWith("\\")) {
			name = name.substring(1);
		}
		final String extension = extension(name);
		final String base = name.substring(0, name.length() - extension.length() - (extension.isEmpty() ? 0 : 1));
		if (extension.equals("mdl")) {
			return Collections.singletonList(base + ".mdx");
		}
		if (!TEXTURE_EXTENSIONS.contains(extension)) {
			return Collections.singletonList(name);
		}
		final List<String> names = new ArrayList<>(3);
		if (!extension.equals("tif")) {
			names.add(name);
		}
		if (!extension.equals("dds")) {
			names.add(base + ".dds");
		}
		if (!base.endsWith(DIFFUSE_SUFFIX)) {
			names.add(base + DIFFUSE_SUFFIX + ".dds");
		}
		return names;
	}

	static Set<String> classicFallback() {
		return readList(CLASSIC_FALLBACK);
	}

	static Set<String> setExtra(final GraphicsSet set) {
		return readList(String.format(Locale.ROOT, SET_EXTRA, set.name().toLowerCase(Locale.ROOT)));
	}

	private static Set<String> readList(final String resource) {
		final Set<String> paths = new HashSet<>();
		try (InputStream stream = PackPlanner.class.getResourceAsStream(resource)) {
			if (stream == null) {
				return paths;
			}
			final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("#")) {
					paths.add(normalize(line));
				}
			}
		}
		catch (final IOException e) {
			throw new IllegalStateException("The file list " + resource + " cannot be read", e);
		}
		return paths;
	}

	/** The engine's launchers show the tool, the game version and the text language from these fields. */
	byte[] markerText(final PackOptions options, final int entryCount) {
		final StringBuilder marker = new StringBuilder();
		marker.append("tool=").append(AppInfo.NAME).append(' ').append(AppInfo.version()).append('\n');
		if ((this.gameVersion != null) && !this.gameVersion.isEmpty() && !"unknown".equals(this.gameVersion)) {
			marker.append("gameVersion=").append(this.gameVersion).append('\n');
		}
		marker.append("profile=").append(options.describe()).append('\n');
		marker.append("locale=").append(displayLocale(this.locale)).append('\n');
		marker.append("entries=").append(entryCount).append('\n');
		return marker.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	public PackPlan planSet(final GraphicsSet set) {
		final String prefix = ROOT + "\\" + set.getFolder() + "\\";
		final List<PlanEntry> planEntries = new ArrayList<>();
		final List<String> skipped = new ArrayList<>();
		for (final StorageEntry entry : this.entries) {
			final String path = normalize(entry.getPath());
			if (!path.startsWith(prefix)) {
				continue;
			}
			final String rest = path.substring(prefix.length());
			final String first = firstSegment(rest);
			if (first.equals("_locales") && !rest.startsWith("_locales\\" + this.locale + ".w3mod\\")) {
				continue;
			}
			if (!first.equals("_locales") && isSubMod(first)) {
				continue;
			}
			if (hasSegment(rest, "movies") || hasSegment(rest, "webui")) {
				continue;
			}
			if (!entry.isLocal()) {
				skipped.add(rest);
				continue;
			}
			final String zipPath = set.getFolder() + "/" + rest.replace('\\', '/');
			planEntries.add(new PlanEntry(zipPath, entry.getPath(), set.getFolder(), entry.getSize()));
		}
		addSetExtra(set, planEntries, skipped);
		if (!planEntries.isEmpty()) {
			planEntries.add(PlanEntry.inline(set.getFolder() + "/" + NOMEDIA, new byte[0]));
		}
		planEntries.sort(Comparator.comparing(PlanEntry::getZipPath));
		Collections.sort(skipped);
		return new PackPlan(set.getPackageName(), set.getLabel(), planEntries, skipped);
	}

	private void addSetExtra(final GraphicsSet set, final List<PlanEntry> planEntries, final List<String> skipped) {
		final Set<String> wanted = setExtra(set);
		for (final StorageEntry entry : this.entries) {
			final String path = normalize(entry.getPath());
			if (!path.startsWith(ROOT + "\\")) {
				continue;
			}
			final String relative = path.substring(ROOT.length() + 1);
			if (!wanted.contains(relative)) {
				continue;
			}
			if (!entry.isLocal()) {
				skipped.add(relative);
				continue;
			}
			planEntries.add(new PlanEntry(relative.replace('\\', '/'), entry.getPath(),
					relative.substring(0, relative.indexOf('\\')), entry.getSize()));
		}
	}

	private static void dropLosingAudio(final Map<String, StorageEntry> winner, final Map<String, Integer> winnerLayer) {
		final Map<String, Integer> bestLayerByStem = new HashMap<>();
		for (final Map.Entry<String, Integer> file : winnerLayer.entrySet()) {
			final String stem = audioStem(file.getKey());
			if (stem != null) {
				bestLayerByStem.merge(stem, file.getValue(), Math::max);
			}
		}
		final List<String> losers = new ArrayList<>();
		for (final Map.Entry<String, Integer> file : winnerLayer.entrySet()) {
			final String stem = audioStem(file.getKey());
			if ((stem != null) && (file.getValue() < bestLayerByStem.get(stem))) {
				losers.add(file.getKey());
			}
		}
		for (final String loser : losers) {
			winner.remove(loser);
			winnerLayer.remove(loser);
		}
	}

	private static String audioStem(final String path) {
		final int dot = path.lastIndexOf('.');
		if ((dot == -1) || (dot < path.lastIndexOf('\\'))) {
			return null;
		}
		return AUDIO_EXTENSIONS.contains(path.substring(dot + 1)) ? path.substring(0, dot) : null;
	}

	private int classicLayerOf(final String path) {
		for (int layer = this.classicLayers.length - 1; layer >= 0; layer--) {
			if (path.startsWith(this.classicLayers[layer] + "\\")) {
				return layer;
			}
		}
		return -1;
	}

	private static boolean isExcluded(final String relative, final PackOptions options, final String locale) {
		final String top = firstSegment(relative);
		if (top.equals("webui")) {
			return !isLauncherWebui(relative, options);
		}
		for (final String folder : UNUSED_FOLDERS) {
			if (relative.startsWith(folder)) {
				return true;
			}
		}
		if (UNUSED_EXTENSIONS.contains(extension(relative))) {
			return true;
		}
		if (top.equals("fonts") && CJK_FONTS.contains(relative.substring("fonts\\".length()))
				&& !CJK_LOCALES.contains(locale)) {
			return true;
		}
		if (top.equals("movies")) {
			return !options.isMovies() || !isMovieWidth(relative, options.getMovieWidth());
		}
		if (options.getContent() == PackOptions.Content.MELEE_ONLY) {
			if (top.equals("campaign")) {
				return true;
			}
			if (top.equals("maps") && hasSegment(relative, "campaign")) {
				return true;
			}
			for (final String folder : CAMPAIGN_DIALOGUE) {
				if (relative.startsWith(folder)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Each movie comes as name_640.avi, name_1280.avi and name_1920.avi; one width is enough. */
	static boolean isMovieWidth(final String relative, final int width) {
		if (!relative.endsWith(".avi")) {
			return true;
		}
		final int underscore = relative.lastIndexOf('_');
		if ((underscore == -1) || (underscore < relative.lastIndexOf('\\'))) {
			return true;
		}
		final String suffix = relative.substring(underscore + 1, relative.length() - ".avi".length());
		if (suffix.isEmpty() || !suffix.chars().allMatch(Character::isDigit)) {
			return true;
		}
		return Integer.parseInt(suffix) == width;
	}

	private static String extension(final String path) {
		final int dot = path.lastIndexOf('.');
		return (dot == -1) || (dot < path.lastIndexOf('\\')) ? "" : path.substring(dot + 1);
	}

	/**
	 * The launchers play the menu and campaign scenes Blizzard recorded as webm (Classic "_sd" and Reforged) and draw
	 * their Reforged skin, the loading screen and the profile portraits from a few webui images.
	 */
	static boolean isLauncherWebui(final String relative, final PackOptions options) {
		if (relative.startsWith("webui\\webms\\")) {
			final String name = relative.substring("webui\\webms\\".length());
			for (final String scene : MENU_SCENES) {
				if (name.equals(scene + ".webm") || name.equals(scene + "_sd.webm")) {
					return true;
				}
			}
			return false;
		}
		for (final String campaign : WEBUI_CAMPAIGN) {
			if (relative.startsWith(campaign)) {
				return options.getContent() == PackOptions.Content.FULL_CLASSIC;
			}
		}
		for (final String folder : WEBUI_FOLDERS) {
			if (relative.startsWith(folder)) {
				return true;
			}
		}
		return WEBUI_FILES.contains(relative);
	}

	static boolean isSubMod(final String segment) {
		return segment.equals("_locales") || (segment.startsWith("_") && segment.endsWith(".w3mod"));
	}

	static String normalize(final String path) {
		return path.toLowerCase(Locale.ROOT).replace(':', '\\').replace('/', '\\');
	}

	private static String firstSegment(final String path) {
		final int slash = path.indexOf('\\');
		return slash == -1 ? path : path.substring(0, slash);
	}

	private static boolean hasSegment(final String path, final String segment) {
		for (final String part : path.split("\\\\")) {
			if (part.equals(segment)) {
				return true;
			}
		}
		return false;
	}
}
