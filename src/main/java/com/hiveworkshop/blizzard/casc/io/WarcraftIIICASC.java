package com.hiveworkshop.blizzard.casc.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.hiveworkshop.blizzard.casc.ConfigurationFile;
import com.hiveworkshop.blizzard.casc.info.Info;
import com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException;
import com.hiveworkshop.blizzard.casc.storage.Storage;
import com.hiveworkshop.blizzard.casc.vfs.VirtualFileSystem;
import com.hiveworkshop.blizzard.casc.vfs.VirtualFileSystem.PathResult;

public class WarcraftIIICASC implements AutoCloseable {
	public class FileSystem {
		private FileSystem() {
		}

		public List<String> enumerateFiles() throws IOException {
			final List<PathResult> pathResults = vfs.getAllFiles();
			final ArrayList<String> filePathStrings = new ArrayList<String>(pathResults.size());

			for (final PathResult pathResult : pathResults) {
				filePathStrings.add(pathResult.getPath());
			}

			return filePathStrings;
		}

		public List<PathResult> enumerateEntries() throws IOException {
			return vfs.getAllFiles();
		}

		public boolean isFile(final String filePath) throws IOException {
			final byte[][] pathFragments = VirtualFileSystem.convertFilePath(filePath);
			try {
				final PathResult resolveResult = vfs.resolvePath(pathFragments);
				return resolveResult.isFile();
			} catch (final FileNotFoundException e) {
				return false;
			}
		}

		public boolean isFileAvailable(final String filePath) throws IOException {
			final byte[][] pathFragments = VirtualFileSystem.convertFilePath(filePath);
			final PathResult resolveResult = vfs.resolvePath(pathFragments);
			return resolveResult.existsInStorage();
		}

		public boolean isNestedFileSystem(final String filePath) throws IOException {
			final byte[][] pathFragments = VirtualFileSystem.convertFilePath(filePath);
			try {
				final PathResult resolveResult = vfs.resolvePath(pathFragments);
				return resolveResult.isTVFS();
			} catch (final FileNotFoundException e) {
				return false;
			}
		}

		public ByteBuffer readFileData(final String filePath) throws IOException {
			final byte[][] pathFragments = VirtualFileSystem.convertFilePath(filePath);
			final PathResult resolveResult = vfs.resolvePath(pathFragments);

			if (!resolveResult.isFile()) {
				throw new FileNotFoundException("the specified file path does not resolve to a file");
			} else if (!resolveResult.existsInStorage()) {
				throw new FileNotFoundException("the specified file is not in local storage");
			}

			final ByteBuffer fileBuffer = resolveResult.readFile(null);
			fileBuffer.flip();
			return fileBuffer;
		}
	}

	private static final String WC3_DATA_FOLDER_NAME = "Data";

	private final Info buildInfo;

	private final int activeInfoRecord;

	private final ConfigurationFile buildConfiguration;

	private final Path dataPath;

	private final Storage localStorage;

	private final VirtualFileSystem vfs;

	public WarcraftIIICASC(final Path installFolder, final boolean useMemoryMapping) throws IOException {
		final Path infoFilePath = installFolder.resolve(Info.BUILD_INFO_FILE_NAME);
		buildInfo = new Info(ByteBuffer.wrap(Files.readAllBytes(infoFilePath)));

		final int recordCount = buildInfo.getRecordCount();
		if (recordCount < 1) {
			throw new MalformedCASCStructureException("build info contains no records");
		}

		final int activeFiledIndex = buildInfo.getFieldIndex("Active");
		if (activeFiledIndex == -1) {
			throw new MalformedCASCStructureException("build info contains no active field");
		}
		int recordIndex = 0;
		for (; recordIndex < recordCount; recordIndex += 1) {
			if (Integer.parseInt(buildInfo.getField(recordIndex, activeFiledIndex)) == 1) {
				break;
			}
		}
		if (recordIndex == recordCount) {
			throw new MalformedCASCStructureException("build info contains no active record");
		}
		activeInfoRecord = recordIndex;

		final int buildKeyFieldIndex = buildInfo.getFieldIndex("Build Key");
		if (buildKeyFieldIndex == -1) {
			throw new MalformedCASCStructureException("build info contains no build key field");
		}
		final String buildKey = buildInfo.getField(activeInfoRecord, buildKeyFieldIndex);

		dataPath = installFolder.resolve(WC3_DATA_FOLDER_NAME);
		if (!Files.isDirectory(dataPath)) {
			throw new MalformedCASCStructureException("data folder is missing");
		}

		buildConfiguration = ConfigurationFile.lookupConfigurationFile(dataPath, buildKey);

		localStorage = new Storage(dataPath, false, useMemoryMapping);

		VirtualFileSystem vfs = null;
		try {
			vfs = new VirtualFileSystem(localStorage, buildConfiguration.getConfiguration());
		} finally {
			if (vfs == null) {
				localStorage.close();
			}
		}
		this.vfs = vfs;
	}

	@Override
	public void close() throws IOException {
		localStorage.close();
	}

	public int getActiveRecordIndex() {
		return activeInfoRecord;
	}

	public String getBranch() throws IOException {
		final int branchFieldIndex = buildInfo.getFieldIndex("Branch");
		if (branchFieldIndex == -1) {
			throw new MalformedCASCStructureException("build info contains no branch field");
		}
		return buildInfo.getField(activeInfoRecord, branchFieldIndex);
	}

	public Info getBuildInfo() {
		return buildInfo;
	}

	public FileSystem getRootFileSystem() {
		return new FileSystem();
	}
}
