package com.nikoltu.w3prep;

public final class ReadFailure {
	private final String path;
	private final String reason;

	public ReadFailure(final String path, final String reason) {
		this.path = path;
		this.reason = reason;
	}

	public String getPath() {
		return this.path;
	}

	public String getReason() {
		return this.reason;
	}

	@Override
	public String toString() {
		return this.path + " · " + this.reason;
	}
}
