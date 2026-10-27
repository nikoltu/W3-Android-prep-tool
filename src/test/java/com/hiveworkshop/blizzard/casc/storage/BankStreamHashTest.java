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
		buffer.put(new byte[16]).putInt(30 + body.length).putShort((short) 0).putInt(0).putInt(0).put(body);
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
