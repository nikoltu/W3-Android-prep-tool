package com.hiveworkshop.blizzard.casc.info;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException;
import com.hiveworkshop.nio.ByteBufferInputStream;

public class Info {
	public static final String BUILD_INFO_FILE_NAME = ".build.info";

	public static final Charset FILE_ENCODING = Charset.forName("UTF8");

	private static final String FIELD_SEPARATOR_REGEX = "\\|";

	private static String[] separateFields(final String encodedLine) {
		return encodedLine.split(FIELD_SEPARATOR_REGEX);
	}

	private final ArrayList<FieldDescriptor> fieldDescriptors = new ArrayList<>();

	private final ArrayList<ArrayList<String>> records = new ArrayList<>();

	public Info(final ByteBuffer fileBuffer) throws IOException {
		try (final ByteBufferInputStream fileStream = new ByteBufferInputStream(fileBuffer);
				final Scanner lineScanner = new Scanner(new InputStreamReader(fileStream, FILE_ENCODING))) {
			final String[] encodedFieldDescriptors = separateFields(lineScanner.nextLine());
			for (final String encodedFieldDescriptor : encodedFieldDescriptors) {
				fieldDescriptors.add(new FieldDescriptor(encodedFieldDescriptor));
			}

			while (lineScanner.hasNextLine()) {
				records.add(new ArrayList<>(Arrays.asList(separateFields(lineScanner.nextLine()))));
			}
		} catch (final NoSuchElementException e) {
			throw new MalformedCASCStructureException("missing headers");
		}
	}

	public String getField(final int recordIndex, final int fieldIndex) {
		return records.get(recordIndex).get(fieldIndex);
	}

	public String getField(final int recordIndex, final String fieldName) {
		final int fieldIndex = getFieldIndex(fieldName);
		if (fieldIndex == -1) {
			return null;
		}

		return getField(recordIndex, fieldIndex);
	}

	public int getFieldCount() {
		return fieldDescriptors.size();
	}

	public FieldDescriptor getFieldDescriptor(final int fieldIndex) {
		return fieldDescriptors.get(fieldIndex);
	}

	public int getFieldIndex(final String name) {
		for (int i = 0; i < fieldDescriptors.size(); i += 1) {
			if (fieldDescriptors.get(i).getName().equals(name)) {
				return i;
			}
		}

		return -1;
	}

	public int getRecordCount() {
		return records.size();
	}
}
