package com.hiveworkshop.blizzard.casc.vfs;

import com.hiveworkshop.blizzard.casc.Key;

public class StorageReference {
	private long offset = 0;

	private long size = 0;

	private Key encodingKey = null;

	private long physicalSize = 0;

	private long actualSize = 0;

	public StorageReference(final long offset, final long size, final Key encodingKey, final int physicalSize,
			final int actualSize) {
		this.offset = offset;
		this.size = size;
		this.encodingKey = encodingKey;
		this.physicalSize = physicalSize;
		this.actualSize = actualSize;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("FileReference{encodingKey=");
		builder.append(encodingKey);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", size=");
		builder.append(size);
		builder.append(", physicalSize=");
		builder.append(physicalSize);
		builder.append(", actualSize=");
		builder.append(actualSize);
		builder.append("}");

		return builder.toString();
	}

	public long getOffset() {
		return offset;
	}

	public long getSize() {
		return size;
	}

	public Key getEncodingKey() {
		return encodingKey;
	}

	public long getPhysicalSize() {
		return physicalSize;
	}

	public long getActualSize() {
		return actualSize;
	}
}
