package com.nikoltu.w3prep;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CascNamesTest {
	@Test
	void namesThatAreNotUtf8StillGetAPrintableForm() {
		final byte[][] fragments = { "war3.w3mod".getBytes(StandardCharsets.UTF_8),
				new byte[] { 'm', 'a', 'p', (byte) 0xC0, '.', 'w', '3', 'x' } };
		assertEquals("war3.w3mod\\map�.w3x", CascGameStorage.lenientPath(fragments));
	}
}
