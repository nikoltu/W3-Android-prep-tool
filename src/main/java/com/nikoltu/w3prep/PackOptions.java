package com.nikoltu.w3prep;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class PackOptions {
	public enum Content {
		FULL_CLASSIC, MELEE_ONLY
	}

	private final Content content;
	private final boolean movies;
	private final Set<GraphicsSet> sets;
	private final boolean desktopFolder;

	public PackOptions(final Content content, final boolean movies, final Set<GraphicsSet> sets,
			final boolean desktopFolder) {
		this.content = content;
		this.movies = movies;
		this.sets = sets.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(sets));
		this.desktopFolder = desktopFolder;
	}

	public static PackOptions defaults() {
		return new PackOptions(Content.FULL_CLASSIC, false, EnumSet.noneOf(GraphicsSet.class), false);
	}

	public Content getContent() {
		return this.content;
	}

	public boolean isMovies() {
		return this.movies;
	}

	public Set<GraphicsSet> getSets() {
		return this.sets;
	}

	public boolean isDesktopFolder() {
		return this.desktopFolder;
	}

	public String describe() {
		final StringBuilder builder = new StringBuilder(
				this.content == Content.FULL_CLASSIC ? "full-classic" : "melee-only");
		if (this.movies) {
			builder.append("+movies");
		}
		return builder.toString();
	}
}
