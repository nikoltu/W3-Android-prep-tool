package com.hiveworkshop.blizzard.casc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException;
import com.hiveworkshop.nio.ByteBufferInputStream;

public class ConfigurationFile {
	public static final String CONFIGURATION_FOLDER_NAME = "config";

	public static final Charset FILE_ENCODING = Charset.forName("UTF8");

	public static final int BUCKET_NAME_LENGTH = 2;

	public static final int BUCKET_TIERS = 2;

	public static ConfigurationFile lookupConfigurationFile(final Path dataFolder, final String keyHex)
			throws IOException {
		Path file = dataFolder.resolve(CONFIGURATION_FOLDER_NAME);
		for (int tier = 0; tier < BUCKET_TIERS; tier += 1) {
			final int keyOffset = tier * BUCKET_NAME_LENGTH;
			final String bucketFolderName = keyHex.substring(keyOffset, keyOffset + BUCKET_NAME_LENGTH);
			file = file.resolve(bucketFolderName);
		}

		file = file.resolve(keyHex);

		final ByteBuffer fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file));

		return new ConfigurationFile(fileBuffer);
	}

	private final Map<String, String> configuration = new HashMap<>();

	public ConfigurationFile(final ByteBuffer fileBuffer) throws IOException {
		try (final ByteBufferInputStream fileStream = new ByteBufferInputStream(fileBuffer);
				final Scanner lineScanner = new Scanner(new InputStreamReader(fileStream, FILE_ENCODING))) {
			while (lineScanner.hasNextLine()) {
				final String line = lineScanner.nextLine().trim();
				final int lineLength = line.indexOf('#');
				final String record;
				if (lineLength != -1) {
					record = line.substring(0, lineLength);
				} else {
					record = line;
				}

				if (!record.equals("")) {
					final int assignmentIndex = record.indexOf('=');
					if (assignmentIndex == -1) {
						throw new MalformedCASCStructureException(
								"configuration file line contains record with no assignment");
					}

					final String key = record.substring(0, assignmentIndex).trim();
					final String value = record.substring(assignmentIndex + 1).trim();

					if (configuration.putIfAbsent(key, value) != null) {
						throw new MalformedCASCStructureException(
								"configuration file contains duplicate key declarations");
					}
				}
			}
		}
	}

	public Map<String, String> getConfiguration() {
		return configuration;
	}
}
