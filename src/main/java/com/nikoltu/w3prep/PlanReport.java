package com.nikoltu.w3prep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PlanReport {
	private PlanReport() {
	}

	public static List<String> lines(final PackPlan plan) {
		final Map<String, long[]> byLayer = new LinkedHashMap<>();
		final Map<String, long[]> byFolder = new TreeMap<>();
		for (final PlanEntry entry : plan.getEntries()) {
			if (entry.isInline()) {
				continue;
			}
			add(byLayer, entry.getLayer(), entry.getSize());
			add(byFolder, folderOf(entry.getZipPath()), entry.getSize());
		}
		final List<String> lines = new ArrayList<>();
		lines.add(plan.getPackageName() + ": " + Sizes.count(plan.getFileCount()) + " files, "
				+ Sizes.format(plan.getTotalBytes()));
		final StringBuilder layers = new StringBuilder("  by layer:");
		for (final Map.Entry<String, long[]> layer : byLayer.entrySet()) {
			layers.append("  ").append(layer.getKey()).append(' ').append(Sizes.count(layer.getValue()[0]));
		}
		lines.add(layers.toString());
		lines.add("  by folder, largest first:");
		final List<Map.Entry<String, long[]>> folders = new ArrayList<>(byFolder.entrySet());
		folders.sort((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]));
		for (final Map.Entry<String, long[]> folder : folders) {
			lines.add(String.format("    %-28s %8s files %10s", folder.getKey(), Sizes.count(folder.getValue()[0]),
					Sizes.format(folder.getValue()[1])));
		}
		if (!plan.getSkipped().isEmpty()) {
			final List<String> skipped = plan.getSkipped();
			lines.add("  not on this install: " + Sizes.count(skipped.size()) + " listed files, for example "
					+ String.join(", ", skipped.subList(0, Math.min(3, skipped.size()))));
		}
		return lines;
	}

	static String folderOf(final String zipPath) {
		final String[] parts = zipPath.split("/");
		if (parts.length == 1) {
			return "(top level files)";
		}
		if (parts[0].startsWith("_") && parts[0].endsWith(".w3mod") && (parts.length > 2)) {
			return parts[0] + "/" + parts[1];
		}
		return parts[0];
	}

	private static void add(final Map<String, long[]> map, final String key, final long size) {
		final long[] totals = map.computeIfAbsent(key, k -> new long[2]);
		totals[0]++;
		totals[1] += size;
	}
}
