package com.hiveworkshop.blizzard.casc.info;

public class FieldDescriptor {
	private static final int NAME_TERMINATOR = '!';
	private static final int DATA_TYPE_TERMINATOR = ':';

	private final String name;
	private FieldDataType dataType;
	private final int size;

	public FieldDescriptor(final String encoded) {
		final int nameEnd = encoded.indexOf(NAME_TERMINATOR);
		if (nameEnd == -1) {
			throw new IllegalArgumentException("missing name terminator");
		}
		final int dataTypeEnd = encoded.indexOf(DATA_TYPE_TERMINATOR, nameEnd + 1);
		if (dataTypeEnd == -1) {
			throw new IllegalArgumentException("missing data type terminator");
		}

		name = encoded.substring(0, nameEnd);

		try {
			dataType = FieldDataType.valueOf(encoded.substring(nameEnd + 1, dataTypeEnd));
		} catch (final IllegalArgumentException e) {
			dataType = FieldDataType.UNSUPPORTED;
		}

		size = Integer.parseInt(encoded.substring(dataTypeEnd + 1));
	}

	public String getName() {
		return name;
	}

	public FieldDataType getDataType() {
		return dataType;
	}

	public int getSize() {
		return size;
	}
}
