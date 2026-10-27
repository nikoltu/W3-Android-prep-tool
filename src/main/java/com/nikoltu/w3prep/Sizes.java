package com.nikoltu.w3prep;

import java.util.Locale;

public final class Sizes {
	private Sizes() {
	}

	public static String format(final long bytes) {
		final double kb = 1024;
		if (bytes < kb) {
			return bytes + " B";
		}
		if (bytes < (kb * kb)) {
			return String.format(Locale.ROOT, "%.0f KB", bytes / kb);
		}
		if (bytes < (kb * kb * kb)) {
			return String.format(Locale.ROOT, "%.0f MB", bytes / (kb * kb));
		}
		return String.format(Locale.ROOT, "%.2f GB", bytes / (kb * kb * kb));
	}

	public static String count(final long number) {
		final String digits = Long.toString(number);
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < digits.length(); i++) {
			if ((i > 0) && (((digits.length() - i) % 3) == 0)) {
				builder.append(' ');
			}
			builder.append(digits.charAt(i));
		}
		return builder.toString();
	}

	public static String duration(final long seconds) {
		if (seconds < 60) {
			return "about " + Math.max(1, seconds) + " s";
		}
		return "about " + Math.round(seconds / 60.0) + " min";
	}
}
