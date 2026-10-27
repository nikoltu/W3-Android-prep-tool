package com.nikoltu.w3prep;

public final class StorageEntry {
	private final String path;
	private final long size;
	private final boolean local;

	public StorageEntry(final String path, final long size, final boolean local) {
		this.path = path;
		this.size = size;
		this.local = local;
	}

	public String getPath() {
		return this.path;
	}

	public long getSize() {
		return this.size;
	}

	public boolean isLocal() {
		return this.local;
	}
}
