package com.nikoltu.w3prep;

public final class PackException extends Exception {
	private static final long serialVersionUID = 1L;

	public enum Kind {
		NOT_AN_INSTALL, UNKNOWN_FORMAT, NO_CLASSIC_FILES, NOT_ENOUGH_SPACE, BAD_OUTPUT, GAME_CHANGED, WRITE_FAILED
	}

	private final Kind kind;

	public PackException(final Kind kind, final String message) {
		super(message);
		this.kind = kind;
	}

	public PackException(final Kind kind, final String message, final Throwable cause) {
		super(message, cause);
		this.kind = kind;
	}

	public Kind getKind() {
		return this.kind;
	}
}
