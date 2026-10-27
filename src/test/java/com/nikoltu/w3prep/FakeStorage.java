package com.nikoltu.w3prep;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class FakeStorage implements GameStorage {
	private final Map<String, byte[]> files = new LinkedHashMap<>();
	private final Set<String> remote = new HashSet<>();
	private final Set<String> broken = new HashSet<>();
	private final Set<String> notDownloaded = new HashSet<>();

	FakeStorage add(final String path, final String content) {
		this.files.put(path, content.getBytes(StandardCharsets.UTF_8));
		return this;
	}

	FakeStorage add(final String path, final byte[] content) {
		this.files.put(path, content);
		return this;
	}

	FakeStorage addRemote(final String path) {
		this.files.put(path, new byte[] { 1, 2, 3 });
		this.remote.add(path);
		return this;
	}

	FakeStorage breakFile(final String path) {
		this.broken.add(path);
		return this;
	}

	static FakeStorage classic() {
		return new FakeStorage().add("war3.w3mod\\units\\unitdata.slk", "unit data")
				.add("war3.w3mod\\ui\\war3skins.txt", "skins").add("war3.w3mod\\fonts\\font.ttf", "font");
	}

	FakeStorage notDownloaded(final String path) {
		this.notDownloaded.add(path);
		return this;
	}

	@Override
	public String getVersion() {
		return "9.9.9.12345";
	}

	@Override
	public List<StorageEntry> listEntries() {
		final List<StorageEntry> entries = new ArrayList<>();
		for (final Map.Entry<String, byte[]> file : this.files.entrySet()) {
			entries.add(new StorageEntry(file.getKey(), file.getValue().length, !this.remote.contains(file.getKey())));
		}
		return entries;
	}

	@Override
	public byte[] read(final String path) throws IOException {
		if (this.notDownloaded.contains(path)) {
			throw new com.hiveworkshop.blizzard.casc.nio.DataNotLocalException("data not downloaded yet");
		}
		if (this.broken.contains(path)) {
			throw new IOException("data missing from storage");
		}
		if (this.remote.contains(path) || !this.files.containsKey(path)) {
			throw new IOException("not local");
		}
		return this.files.get(path);
	}

	@Override
	public void close() {
	}
}
