package com.hiveworkshop.blizzard.casc.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.jupiter.api.Test;

import com.hiveworkshop.blizzard.casc.nio.HashMismatchException;

class BankStreamHashTest {
	private static byte[] md5(final byte[] data) throws Exception {
		return MessageDigest.getInstance("MD5").digest(data);
	}

	private static ByteBuffer container(final byte[][] banks, final byte[][] hashes, final int[] decodedSizes) {
		return container(new byte[16], banks, hashes, decodedSizes);
	}

	private static ByteBuffer container(final byte[] headerKey, final byte[][] banks, final byte[][] hashes,
			final int[] decodedSizes) {
		final ByteArrayOutputStream blte = new ByteArrayOutputStream();
		final int headerSize = 8 + 4 + (banks.length * 24);
		final ByteBuffer header = ByteBuffer.allocate(headerSize).order(ByteOrder.BIG_ENDIAN);
		header.put("BLTE".getBytes(StandardCharsets.US_ASCII)).putInt(headerSize);
		header.put((byte) 0x0F).put((byte) 0).put((byte) 0).put((byte) banks.length);
		for (int i = 0; i < banks.length; i++) {
			header.putInt(banks[i].length).putInt(decodedSizes[i]).put(hashes[i]);
		}
		blte.writeBytes(header.array());
		for (final byte[] bank : banks) {
			blte.writeBytes(bank);
		}
		final byte[] body = blte.toByteArray();
		final ByteBuffer buffer = ByteBuffer.allocate(30 + body.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(headerKey).putInt(30 + body.length).putShort((short) 0).putInt(0).putInt(0).put(body);
		buffer.flip();
		return buffer;
	}

	private static byte[] plain(final String text) {
		final byte[] data = text.getBytes(StandardCharsets.UTF_8);
		final byte[] bank = new byte[data.length + 1];
		bank[0] = 'N';
		System.arraycopy(data, 0, bank, 1, data.length);
		return bank;
	}

	@Test
	void goodHashesPassAndDataDecodes() throws Exception {
		final byte[] a = plain("hello ");
		final byte[] b = plain("world");
		final BankStream stream = new BankStream(container(new byte[][] { a, b }, new byte[][] { md5(a), md5(b) },
				new int[] { 6, 5 }), null);
		assertTrue(stream.verifyBankHashes());
		final ByteBuffer out = ByteBuffer.allocate(11);
		while (stream.hasNextBank()) {
			stream.getBank(out);
		}
		assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), out.array());
	}

	@Test
	void changedByteIsCaught() throws Exception {
		final byte[] a = plain("hello ");
		final byte[] b = plain("world");
		final byte[] hashB = md5(b);
		b[3] = 'X';
		final BankStream stream = new BankStream(container(new byte[][] { a, b }, new byte[][] { md5(a), hashB },
				new int[] { 6, 5 }), null);
		assertThrows(HashMismatchException.class, stream::verifyBankHashes);
	}

	@Test
	void zeroHeaderMeansNotDownloaded() throws Exception {
		final byte[] a = plain("abc");
		final com.hiveworkshop.blizzard.casc.Key expected = new com.hiveworkshop.blizzard.casc.Key(
				new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		assertThrows(com.hiveworkshop.blizzard.casc.nio.DataNotLocalException.class,
				() -> new BankStream(container(new byte[][] { a }, new byte[][] { md5(a) }, new int[] { 3 }), expected));
	}

	@Test
	void otherKeyMismatchNamesBothKeys() throws Exception {
		final byte[] a = plain("abc");
		final byte[] header = new byte[16];
		header[15] = 0x77;
		final com.hiveworkshop.blizzard.casc.Key expected = new com.hiveworkshop.blizzard.casc.Key(
				new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		final com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException error = assertThrows(
				com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException.class,
				() -> new BankStream(container(header, new byte[][] { a }, new byte[][] { md5(a) }, new int[] { 3 }),
						expected));
		assertFalse(error instanceof com.hiveworkshop.blizzard.casc.nio.DataNotLocalException);
		assertTrue(error.getMessage().contains("index 010203040506070809"));
	}

	@Test
	void blteWithoutContainerHeaderDecodesAndChecks() throws Exception {
		final byte[] a = plain("hello ");
		final byte[] b = plain("world");
		final ByteBuffer full = container(new byte[][] { a, b }, new byte[][] { md5(a), md5(b) }, new int[] { 6, 5 });
		full.position(30);
		final ByteBuffer blteOnly = full.slice();
		assertTrue(BankStream.startsWithBlte(blteOnly));
		final int headerSize = blteOnly.getInt(4);
		final byte[] headerBytes = new byte[headerSize];
		blteOnly.duplicate().get(headerBytes);
		final byte[] digest = md5(headerBytes);
		final com.hiveworkshop.blizzard.casc.Key key = new com.hiveworkshop.blizzard.casc.Key(
				java.util.Arrays.copyOf(digest, 9));
		assertThrows(com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException.class,
				() -> BankStream.withoutContainer(blteOnly,
						new com.hiveworkshop.blizzard.casc.Key(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
		final BankStream stream = BankStream.withoutContainer(blteOnly, key);
		assertTrue(stream.verifyBankHashes());
		final ByteBuffer out = ByteBuffer.allocate(11);
		while (stream.hasNextBank()) {
			stream.getBank(out);
		}
		assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), out.array());
		assertFalse(BankStream.startsWithBlte(ByteBuffer.wrap(new byte[] { 0, 0, 0, 0 })));
	}

	@Test
	void zeroHashesAreSkipped() throws Exception {
		final byte[] a = plain("abc");
		final BankStream stream = new BankStream(container(new byte[][] { a }, new byte[][] { new byte[16] },
				new int[] { 3 }), null);
		assertFalse(stream.verifyBankHashes());
	}

	@Test
	void encryptedBankIsNeverDecoded() throws Exception {
		final byte[] encrypted = new byte[] { 'E', 1, 2, 3, 4 };
		final BankStream stream = new BankStream(container(new byte[][] { encrypted },
				new byte[][] { md5(encrypted) }, new int[] { 4 }), null);
		assertTrue(stream.verifyBankHashes());
		assertThrows(UnsupportedEncodingException.class, () -> stream.getBank(ByteBuffer.allocate(4)));
	}
}
