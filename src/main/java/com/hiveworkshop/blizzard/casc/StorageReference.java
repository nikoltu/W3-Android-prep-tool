package com.hiveworkshop.blizzard.casc;

import java.util.Map;

public class StorageReference {
	private static final String SIZES_SUFFIX = "-size";

	private final long storedSize;
	private final long size;
	private final Key encodingKey;
	private final Key contentKey;

	public StorageReference(final String name, final Map<String, String> configuration) {
		final String keys = configuration.get(name);
		if (keys == null) {
			throw new IllegalArgumentException("name does not exist in configuration");
		}
		final String sizes = configuration.get(name + SIZES_SUFFIX);
		if (sizes == null) {
			throw new IllegalArgumentException("size missing in configuration");
		}

		final String[] keyStrings = keys.split(" ");
		contentKey = new Key(keyStrings[0]);
		encodingKey = new Key(keyStrings[1]);

		final String[] sizeStrings = sizes.split(" ");
		size = Long.parseLong(sizeStrings[0]);
		storedSize = Long.parseLong(sizeStrings[1]);
	}

	public Key getContentKey() {
		return contentKey;
	}

	public Key getEncodingKey() {
		return encodingKey;
	}

	public long getSize() {
		return size;
	}

	public long getStoredSize() {
		return storedSize;
	}
}
