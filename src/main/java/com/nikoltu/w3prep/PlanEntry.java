package com.nikoltu.w3prep;

public final class PlanEntry {
	private final String zipPath;
	private final String sourcePath;
	private final String layer;
	private final long size;
	private final byte[] inlineContent;

	public PlanEntry(final String zipPath, final String sourcePath, final String layer, final long size) {
		this(zipPath, sourcePath, layer, size, null);
	}

	private PlanEntry(final String zipPath, final String sourcePath, final String layer, final long size,
			final byte[] inlineContent) {
		this.zipPath = zipPath;
		this.sourcePath = sourcePath;
		this.layer = layer;
		this.size = size;
		this.inlineContent = inlineContent;
	}

	public static PlanEntry inline(final String zipPath, final byte[] content) {
		return new PlanEntry(zipPath, null, "tool", content.length, content.clone());
	}

	public boolean isInline() {
		return this.inlineContent != null;
	}

	public byte[] getInlineContent() {
		return this.inlineContent == null ? null : this.inlineContent.clone();
	}

	public String getZipPath() {
		return this.zipPath;
	}

	public String getSourcePath() {
		return this.sourcePath;
	}

	public String getLayer() {
		return this.layer;
	}

	public long getSize() {
		return this.size;
	}
}
