package com.nikoltu.w3prep;

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
	// The launcher's Reforged skin: frames, buttons, doors, loading screen parts and the mission pictures.
	private static final Set<String> WEBUI_SKIN = new HashSet<>(Arrays.asList("webui\\common\\homescreen.jpg",
			"webui\\common\\wc3tft-logo.png", "webui\\common\\wc3roc-logo.png",
			"webui\\common\\header-decoration-gold.png", "webui\\campaign\\campaign-select-border.png",
			"webui\\campaign\\campaign-select-border_hover.png", "webui\\campaign\\campaign-mission-checkmark.png",
			"webui\\campaign\\rightpanel\\right-panel.png", "webui\\doors\\doors_left.png",
			"webui\\doors\\doors_right.png", "webui\\doors\\doors_lion_center.png",
			"webui\\baseframes\\war3_metal_frame_4k.png", "webui\\baseframes\\war3_metal_frame_header_4k.png",
			"webui\\baseframes\\chain_clasp_4k.png", "webui\\modal\\errormodal\\shrf-head-plate.png",
			"webui\\modal\\errormodal\\error_icon.png", "webui\\widgets\\buttons\\wc3_btn_blue_normal_4k.png",
			"webui\\widgets\\buttons\\wc3_btn_blue_down_4k.png", "webui\\widgets\\buttons\\wc3_btn_blue_hover_4k.png",
			"webui\\widgets\\buttons\\wc3_btn_disabled_normal_4k.png", "webui\\loadingscreen\\loading-bar-4k.png",
			"webui\\loadingscreen\\loadingscreen-fill-flat.png", "webui\\loadingscreen\\loadingscreen-bar-endcap_4k.png",
			"webui\\loadingscreen\\top_center_goth_piece.png", "webui\\loadingscreen\\bottom_center_goth_piece.png"));
	// Read by neither the engine nor the launcher: Reforged shaders, and the Reforged and Forsaken Kingdom
	// campaigns, whose campaign list (CampaignInfoReforged.txt, CampaignInfoDLC01.txt) the engine does not load.
	private static final List<String> UNUSED_FOLDERS = Arrays.asList("shaders\\", "campaign\\reforged\\",
			"campaign\\forsakenkingdom\\", "maps\\forsakenkingdom\\", "sound\\dialogue\\humanrecamp\\",
			"sound\\dialogue\\undeadrecamp\\");

	private final List<StorageEntry> entries;
	private final String locale;
	private final String[] classicLayers;
	private final String[] classicLayerNames;

	public PackPlanner(final List<StorageEntry> entries) {
		this.entries = entries;
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
			if (relative.isEmpty() || isSubMod(firstSegment(relative)) || isExcluded(relative, options)) {
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
		final String marker = "tool=W3 Data Packer " + AppInfo.version() + "\nprofile=" + options.describe()
				+ "\nlocale=" + displayLocale(this.locale) + "\nentries=" + planEntries.size() + "\n";
		planEntries.add(PlanEntry.inline(MARKER, marker.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		Collections.sort(skipped);
		return new PackPlan("w3-data-classic.zip", "Classic", planEntries, skipped);
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
		if (!planEntries.isEmpty()) {
			planEntries.add(PlanEntry.inline(set.getFolder() + "/" + NOMEDIA, new byte[0]));
		}
		planEntries.sort(Comparator.comparing(PlanEntry::getZipPath));
		Collections.sort(skipped);
		return new PackPlan(set.getPackageName(), set.getLabel(), planEntries, skipped);
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

	private static boolean isExcluded(final String relative, final PackOptions options) {
		final String top = firstSegment(relative);
		if (top.equals("webui")) {
			return !isLauncherWebui(relative, options);
		}
		for (final String folder : UNUSED_FOLDERS) {
			if (relative.startsWith(folder)) {
				return true;
			}
		}
		if (top.equals("movies") && !options.isMovies()) {
			return true;
		}
		if (options.getContent() == PackOptions.Content.MELEE_ONLY) {
			if (top.equals("campaign")) {
				return true;
			}
			if (top.equals("maps") && hasSegment(relative, "campaign")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The launcher plays the menu and campaign scenes Blizzard recorded as webm (Classic "_sd" and Reforged) and draws
	 * its Reforged skin from a few webui images; the rest of webui is the Battle.net web client.
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
		if (relative.startsWith("webui\\loadingscreen\\campaign\\roc\\")
				|| relative.startsWith("webui\\loadingscreen\\campaign\\tft\\")) {
			return options.getContent() == PackOptions.Content.FULL_CLASSIC;
		}
		return WEBUI_SKIN.contains(relative);
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
