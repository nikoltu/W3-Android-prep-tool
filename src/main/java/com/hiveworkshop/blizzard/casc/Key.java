package com.hiveworkshop.blizzard.casc;

import com.hiveworkshop.ReteraCASCUtils;
import com.hiveworkshop.lang.Hex;

public final class Key implements Comparable<Key> {
	private final byte[] key;

	public Key(final byte[] key) {
		this.key = key;
	}

	public Key(final CharSequence key) {
		this.key = Hex.decodeHex(key);
	}

	@Override
	public int compareTo(final Key o) {
		final int commonLength = Math.min(key.length, o.key.length);
		return ReteraCASCUtils.arraysCompareUnsigned(key, 0, commonLength, o.key, 0, commonLength);
	}

	@Override
	public boolean equals(final Object obj) {
		if ((obj == null) || !(obj instanceof Key)) {
			return false;
		}

		final Key otherKey = (Key) obj;
		final int commonLength = Math.min(key.length, otherKey.key.length);
		if (!ReteraCASCUtils.arraysEquals(key, 0, commonLength, otherKey.key, 0, commonLength)) {
			return false;
		}

		return true;
	}

	public byte[] getKey() {
		return key.clone();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException("key hash code not safe to use due to variable sizes between systems");
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder((key.length + 1));
		Hex.stringBufferAppendHex(builder, key);
		return builder.toString();
	}
}
