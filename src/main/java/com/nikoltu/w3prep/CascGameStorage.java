package com.nikoltu.w3prep;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hiveworkshop.blizzard.casc.info.Info;
import com.hiveworkshop.blizzard.casc.io.WarcraftIIICASC;
import com.hiveworkshop.blizzard.casc.nio.HashMismatchException;
import com.hiveworkshop.blizzard.casc.vfs.VirtualFileSystem.PathResult;

public final class CascGameStorage implements GameStorage {
	private final WarcraftIIICASC casc;
	private final String version;
	private final Map<String, PathResult> results = new HashMap<>();
	private boolean verifyBlocks = true;

	public CascGameStorage(final Path installFolder) throws IOException {
		this.casc = new WarcraftIIICASC(installFolder, false);
		this.version = readVersion(this.casc.getBuildInfo(), this.casc.getActiveRecordIndex());
	}

	private static String readVersion(final Info info, final int record) {
		final int field = info.getFieldIndex("Version");
		if (field == -1) {
			return "unknown";
		}
		final String value = info.getField(record, field);
		return (value == null) || value.isEmpty() ? "unknown" : value;
	}

	@Override
	public String getVersion() {
		return this.version;
	}

	@Override
	public List<StorageEntry> listEntries() throws IOException {
		final List<PathResult> all = this.casc.getRootFileSystem().enumerateEntries();
		final List<StorageEntry> entries = new ArrayList<>(all.size());
		this.results.clear();
		for (final PathResult result : all) {
			if (!result.isFile() || result.isTVFS()) {
				continue;
			}
			final String path = result.getPath();
			this.results.put(path, result);
			entries.add(new StorageEntry(path, result.getFileSize(), result.existsInStorage()));
		}
		return entries;
	}

	@Override
	public byte[] read(final String path) throws IOException {
		final PathResult result = this.results.get(path);
		if (result == null) {
			throw new IOException("not in the file list");
		}
		if (!result.existsInStorage()) {
			throw new IOException("data missing from storage");
		}
		final ByteBuffer buffer = result.readFile(null, this.verifyBlocks);
		buffer.flip();
		if (buffer.hasArray() && (buffer.arrayOffset() == 0) && (buffer.remaining() == buffer.array().length)) {
			return buffer.array();
		}
		final byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		return data;
	}

	@Override
	public BlockCheck calibrate(final List<String> samplePaths) {
		int matched = 0;
		int mismatched = 0;
		int unreadable = 0;
		for (final String path : samplePaths) {
			final PathResult result = this.results.get(path);
			if ((result == null) || !result.existsInStorage()) {
				continue;
			}
			try {
				result.readFile(null, true);
				matched++;
			} catch (final HashMismatchException e) {
				try {
					result.readFile(null, false);
					mismatched++;
				} catch (final IOException | RuntimeException again) {
					unreadable++;
				}
			} catch (final IOException | RuntimeException e) {
				unreadable++;
			}
		}
		final BlockCheck check = new BlockCheck(matched, mismatched, unreadable);
		this.verifyBlocks = check.isEnabled();
		return check;
	}

	@Override
	public void close() throws IOException {
		this.casc.close();
	}
}
