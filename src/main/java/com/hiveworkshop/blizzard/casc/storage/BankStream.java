package com.hiveworkshop.blizzard.casc.storage;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.hiveworkshop.blizzard.casc.Key;
import com.hiveworkshop.blizzard.casc.nio.DataNotLocalException;
import com.hiveworkshop.blizzard.casc.nio.HashMismatchException;
import com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException;

public class BankStream {
	private final StorageContainer container;
	private final BLTEContent[] content;
	private final ByteBuffer streamBuffer;
	private int bank = 0;
	private boolean hasBanks;

	private BankStream(final ByteBuffer blteBuffer) throws IOException {
		container = null;
		final ByteBuffer buffer = blteBuffer.slice();
		content = BLTEContent.decodeContent(buffer);
		hasBanks = true;
		streamBuffer = buffer;
	}

	public static BankStream withoutContainer(final ByteBuffer blteBuffer, final Key encodingKey) throws IOException {
		if (!startsWithBlte(blteBuffer) || (blteBuffer.remaining() < 8)) {
			throw new MalformedCASCStructureException("missing BLTE identifier");
		}
		final int headerSize = blteBuffer.getInt(blteBuffer.position() + 4);
		if ((headerSize <= 8) || (headerSize > blteBuffer.remaining())) {
			throw new MalformedCASCStructureException("BLTE without block table can't be matched to its key");
		}
		final MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			throw new IOException("MD5 not available", e);
		}
		final ByteBuffer header = blteBuffer.duplicate();
		header.limit(header.position() + headerSize);
		md5.update(header);
		if (!new Key(md5.digest()).equals(encodingKey)) {
			throw new MalformedCASCStructureException("BLTE data belongs to another key");
		}
		return new BankStream(blteBuffer);
	}

	public static boolean startsWithBlte(final ByteBuffer buffer) {
		return (buffer.remaining() >= 4) && (buffer.get(buffer.position()) == 'B')
				&& (buffer.get(buffer.position() + 1) == 'L') && (buffer.get(buffer.position() + 2) == 'T')
				&& (buffer.get(buffer.position() + 3) == 'E');
	}

	public BankStream(final ByteBuffer storageBuffer, final Key encodingKey) throws IOException {
		ByteBuffer streamBuffer = storageBuffer.slice();
		container = new StorageContainer(streamBuffer);
		if ((encodingKey != null) && !container.getKey().equals(encodingKey)) {
			if (isZero(container.getKey().getKey())) {
				throw new DataNotLocalException("data not downloaded yet (index " + encodingKey + ")");
			}
			throw new MalformedCASCStructureException("container encoding key mismatch (index " + encodingKey
					+ ", data " + container.getKey() + ", size " + container.getSize() + ")");
		}

		final int storageSize = (int) container.getSize();
		final int storageSizeDiff = Integer.compare(streamBuffer.capacity(), storageSize);

		if (storageSizeDiff < 0) {
			throw new MalformedCASCStructureException("container buffer smaller than container");
		} else if ((encodingKey != null) && (storageSizeDiff != 0)) {
			throw new MalformedCASCStructureException("container buffer size mismatch");
		} else if (storageSizeDiff > 0) {
			final int streamPos = streamBuffer.position();
			streamBuffer.limit(storageSize);
			streamBuffer.position(0);
			streamBuffer = streamBuffer.slice();
			streamBuffer.position(streamPos);
		}

		if (streamBuffer.hasRemaining()) {
			content = BLTEContent.decodeContent(streamBuffer);
			hasBanks = true;
		} else {
			content = null;
			hasBanks = false;
		}

		this.streamBuffer = streamBuffer;
		storageBuffer.position(storageBuffer.position() + streamBuffer.capacity());
	}

	public long getNextBankLength() throws EOFException {
		if (!hasNextBank()) {
			throw new EOFException("no more banks to decode");
		}

		return content.length != 0 ? content[bank].getDecompressedSize() : streamBuffer.remaining();
	}

	public ByteBuffer getBank(ByteBuffer bankBuffer) throws IOException {
		if (!hasNextBank()) {
			throw new EOFException("no more banks to decode");
		}

		if (content.length != 0) {
			final BLTEContent blteEntry = content[bank];
			final long encodedSize = blteEntry.getCompressedSize();
			final long decodedSize = blteEntry.getDecompressedSize();

			if (streamBuffer.remaining() < encodedSize) {
				throw new MalformedCASCStructureException("encoded data beyond end of file");
			} else if (bankBuffer == null) {
				if (decodedSize > Integer.MAX_VALUE) {
					throw new MalformedCASCStructureException("bank too large for Java to manipulate");
				}
				bankBuffer = ByteBuffer.allocate((int) decodedSize);
			} else if (bankBuffer.remaining() < decodedSize) {
				throw new BufferOverflowException();
			}

			final ByteBuffer encodedBuffer = ((ByteBuffer) streamBuffer.slice().limit((int) encodedSize)).slice();
			final ByteBuffer decodedBuffer = ((ByteBuffer) bankBuffer.slice().limit((int) decodedSize)).slice();
			final byte[] intermediateEncodedCopy = new byte[encodedBuffer.remaining()];
			final byte[] intermediateDecodedCopy = new byte[decodedBuffer.remaining()];

			final char encodingMode = (char) encodedBuffer.get();
			switch (encodingMode) {
			case 'N':
				if (encodedBuffer.remaining() != decodedSize) {
					throw new MalformedCASCStructureException("not enough uncompressed bytes");
				}
				decodedBuffer.put(encodedBuffer);
				break;
			case 'Z':
				final Inflater zlib = new Inflater();
				encodedBuffer.get(intermediateEncodedCopy, 0, encodedBuffer.remaining());
				zlib.setInput(intermediateEncodedCopy);
				final int resultSize;
				try {
					resultSize = zlib.inflate(intermediateDecodedCopy);
					decodedBuffer.put(intermediateDecodedCopy, 0, resultSize);
				} catch (final DataFormatException e) {
					throw new MalformedCASCStructureException("zlib inflate exception", e);
				}
				if (resultSize != decodedSize) {
					throw new MalformedCASCStructureException("not enough bytes generated: " + resultSize + "B");
				} else if (!zlib.finished()) {
					throw new MalformedCASCStructureException("unfinished inflate operation");
				}
				break;
			default:
				throw new UnsupportedEncodingException("unsupported encoding mode: " + encodingMode);
			}

			streamBuffer.position(streamBuffer.position() + encodedBuffer.position());
			bankBuffer.position(bankBuffer.position() + decodedBuffer.position());

			bank += 1;
			if (bank == content.length) {
				hasBanks = false;
			}
		} else {
			if (bankBuffer == null) {
				bankBuffer = ByteBuffer.allocate(streamBuffer.remaining());
			} else if (bankBuffer.remaining() < streamBuffer.remaining()) {
				throw new MalformedCASCStructureException("bank buffer too small");
			}

			bankBuffer.put(streamBuffer);
			hasBanks = false;
		}

		return bankBuffer;
	}

	public boolean hasNextBank() {
		return hasBanks;
	}

	public boolean verifyBankHashes() throws IOException {
		if (!hasBanks || (content.length == 0)) {
			return false;
		}

		final MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			throw new IOException("MD5 not available", e);
		}

		boolean checked = false;
		int offset = streamBuffer.position();
		for (int index = bank; index < content.length; index += 1) {
			final BLTEContent blteEntry = content[index];
			final int encodedSize = (int) blteEntry.getCompressedSize();
			if ((offset + encodedSize) > streamBuffer.limit()) {
				throw new MalformedCASCStructureException("encoded data beyond end of file");
			}

			final byte[] expected = blteEntry.getHash();
			if (!isZero(expected)) {
				final ByteBuffer encodedBuffer = streamBuffer.duplicate();
				encodedBuffer.limit(offset + encodedSize);
				encodedBuffer.position(offset);
				md5.reset();
				md5.update(encodedBuffer);
				if (!Arrays.equals(expected, md5.digest())) {
					throw new HashMismatchException("bank " + index + " hash mismatch");
				}
				checked = true;
			}
			offset += encodedSize;
		}
		return checked;
	}

	private static boolean isZero(final byte[] bytes) {
		for (final byte b : bytes) {
			if (b != 0) {
				return false;
			}
		}
		return true;
	}
}
