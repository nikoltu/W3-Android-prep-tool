package com.nikoltu.w3prep;

public final class WrittenFile {
	private final String zipPath;
	private final String layer;
	private final long size;
	private final long crc;
	private final String md5;

	public WrittenFile(final String zipPath, final String layer, final long size, final long crc, final String md5) {
		this.zipPath = zipPath;
		this.layer = layer;
		this.size = size;
		this.crc = crc;
		this.md5 = md5;
	}

	public String getZipPath() {
		return this.zipPath;
	}

	public String getLayer() {
		return this.layer;
	}

	public long getSize() {
		return this.size;
	}

	public long getCrc() {
		return this.crc;
	}

	public String getMd5() {
		return this.md5;
	}
}
