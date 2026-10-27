package com.nikoltu.w3prep;

public enum GraphicsSet {
	DE("_de.w3mod", "DE set", "w3-data-set-de.zip"),
	HD("_hd.w3mod", "HD set", "w3-data-set-hd.zip");

	private final String folder;
	private final String label;
	private final String packageName;

	GraphicsSet(final String folder, final String label, final String packageName) {
		this.folder = folder;
		this.label = label;
		this.packageName = packageName;
	}

	public String getFolder() {
		return this.folder;
	}

	public String getLabel() {
		return this.label;
	}

	public String getPackageName() {
		return this.packageName;
	}
}
