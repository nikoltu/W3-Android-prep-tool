package com.nikoltu.w3prep;

import java.util.Collections;
import java.util.List;

public final class PackPlan {
	private final String packageName;
	private final String label;
	private final List<PlanEntry> entries;
	private final long totalBytes;
	private final List<String> skipped;

	public PackPlan(final String packageName, final String label, final List<PlanEntry> entries,
			final List<String> skipped) {
		this.packageName = packageName;
		this.label = label;
		this.entries = Collections.unmodifiableList(entries);
		this.skipped = Collections.unmodifiableList(skipped);
		long total = 0;
		for (final PlanEntry entry : entries) {
			total += entry.getSize();
		}
		this.totalBytes = total;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public String getLabel() {
		return this.label;
	}

	public List<PlanEntry> getEntries() {
		return this.entries;
	}

	public int getFileCount() {
		int count = 0;
		for (final PlanEntry entry : this.entries) {
			if (!entry.isInline()) {
				count++;
			}
		}
		return count;
	}

	public List<String> sampleForBlockCheck() {
		final List<String> small = new java.util.ArrayList<>();
		for (final PlanEntry entry : this.entries) {
			if (!entry.isInline() && (entry.getSize() > 0) && (entry.getSize() <= (1 << 20))) {
				small.add(entry.getSourcePath());
			}
		}
		final List<String> sample = new java.util.ArrayList<>();
		final int count = Math.min(BlockCheck.SAMPLE_SIZE, small.size());
		for (int i = 0; i < count; i++) {
			sample.add(small.get((int) (((long) i * small.size()) / count)));
		}
		return sample;
	}

	public long getTotalBytes() {
		return this.totalBytes;
	}

	public List<String> getSkipped() {
		return this.skipped;
	}
}
