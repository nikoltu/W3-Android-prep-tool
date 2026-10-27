package com.nikoltu.w3prep;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface GameStorage extends Closeable {
	String getVersion();

	List<StorageEntry> listEntries() throws IOException;

	byte[] read(String path) throws IOException;

	default BlockCheck calibrate(final List<String> samplePaths) {
		return BlockCheck.notAvailable();
	}
}
