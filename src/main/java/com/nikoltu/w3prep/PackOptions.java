package com.nikoltu.w3prep;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class PackOptions {
	public enum Content {
		FULL_CLASSIC, MELEE_ONLY
	}

	/** The engine plays the smallest copy at least as wide as the screen, else the widest there is. */
	public static final int[] MOVIE_WIDTHS = { 1920, 1280, 640 };
	public static final int DEFAULT_MOVIE_WIDTH = 1920;

	private final Content content;
	private final boolean movies;
	private final int movieWidth;
	private final Set<GraphicsSet> sets;
	private final boolean desktopFolder;

	public PackOptions(final Content content, final boolean movies, final Set<GraphicsSet> sets,
			final boolean desktopFolder) {
		this(content, movies, DEFAULT_MOVIE_WIDTH, sets, desktopFolder);
	}

	public PackOptions(final Content content, final boolean movies, final int movieWidth,
			final Set<GraphicsSet> sets, final boolean desktopFolder) {
		if (!isMovieWidth(movieWidth)) {
			throw new IllegalArgumentException("movie width " + movieWidth);
		}
		this.content = content;
		this.movies = movies;
		this.movieWidth = movieWidth;
		this.sets = sets.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(sets));
		this.desktopFolder = desktopFolder;
	}

	public static PackOptions defaults() {
		return new PackOptions(Content.FULL_CLASSIC, false, EnumSet.noneOf(GraphicsSet.class), false);
	}

	public static boolean isMovieWidth(final int width) {
		for (final int known : MOVIE_WIDTHS) {
			if (known == width) {
				return true;
			}
		}
		return false;
	}

	public Content getContent() {
		return this.content;
	}

	public boolean isMovies() {
		return this.movies;
	}

	public int getMovieWidth() {
		return this.movieWidth;
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
			builder.append("+movies").append(this.movieWidth);
		}
		return builder.toString();
	}
}
