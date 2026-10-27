package com.nikoltu.w3prep;

public final class BlockCheck {
	public static final int SAMPLE_SIZE = 20;

	private final int matched;
	private final int mismatched;
	private final int unreadable;
	private final boolean enabled;

	public BlockCheck(final int matched, final int mismatched, final int unreadable) {
		this.matched = matched;
		this.mismatched = mismatched;
		this.unreadable = unreadable;
		this.enabled = decide(matched, mismatched);
	}

	public static BlockCheck notAvailable() {
		return new BlockCheck(0, 0, 0);
	}

	static boolean decide(final int matched, final int mismatched) {
		return matched > 0;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public int getMatched() {
		return this.matched;
	}

	public int getMismatched() {
		return this.mismatched;
	}

	public int getUnreadable() {
		return this.unreadable;
	}

	public String describe() {
		final int tried = this.matched + this.mismatched + this.unreadable;
		if (tried == 0) {
			return "block checksums: not available, files are checked by reading the package back";
		}
		if (this.enabled) {
			return "block checksums: on (" + this.matched + " of " + tried + " sample files matched)";
		}
		return "block checksums: off, this storage does not match the expected checksum format (" + this.mismatched
				+ " of " + tried + " sample files differ but decode fine); files are checked by reading the package "
				+ "back";
	}
}
