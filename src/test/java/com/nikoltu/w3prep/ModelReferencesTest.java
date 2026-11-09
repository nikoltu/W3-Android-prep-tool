package com.nikoltu.w3prep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ModelReferencesTest {
	private static final int NODE_SIZE = 4 + 80 + 12;
	private static final int EMITTER_SIZE = 4 + NODE_SIZE + 16 + 260 + 8;

	/** A binary model with these textures and particle emitters (spawned models), and a chunk that names nothing. */
	static byte[] model(final List<String> textures, final List<String> spawned) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes("MDLX".getBytes(StandardCharsets.US_ASCII));
		out.writeBytes(chunk("VERS", buffer(4).putInt(1000)));
		final ByteBuffer texs = buffer(268 * textures.size());
		for (final String texture : textures) {
			texs.putInt(0).put(fixed(texture, 260)).putInt(0);
		}
		out.writeBytes(chunk("TEXS", texs));
		final ByteBuffer prem = buffer(EMITTER_SIZE * spawned.size());
		for (final String path : spawned) {
			prem.putInt(EMITTER_SIZE).putInt(NODE_SIZE).put(fixed("emitter", 80)).putInt(0).putInt(-1).putInt(0);
			prem.putFloat(1).putFloat(0).putFloat(0).putFloat(0).put(fixed(path, 260)).putFloat(1).putFloat(0);
		}
		out.writeBytes(chunk("PREM", prem));
		return out.toByteArray();
	}

	private static ByteBuffer buffer(final int size) {
		return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
	}

	private static byte[] fixed(final String text, final int size) {
		return Arrays.copyOf(text.getBytes(StandardCharsets.UTF_8), size);
	}

	private static byte[] chunk(final String tag, final ByteBuffer data) {
		final ByteBuffer chunk = buffer(8 + data.capacity());
		chunk.put(tag.getBytes(StandardCharsets.US_ASCII)).putInt(data.capacity()).put(data.array());
		return chunk.array();
	}

	@Test
	void readsTexturesAndSpawnedModels() {
		final byte[] model = model(List.of("Textures\\A.blp", "", "Units\\B_Diffuse.tif", "Textures\\A.blp"),
				List.of("Abilities\\C.mdl"));
		assertEquals(List.of("Textures\\A.blp", "Units\\B_Diffuse.tif", "Abilities\\C.mdl"), ModelReferences.of(model));
	}

	@Test
	void namesNothingInOtherOrBrokenData() {
		assertTrue(ModelReferences.of("Version { }".getBytes(StandardCharsets.US_ASCII)).isEmpty());
		final byte[] model = model(List.of("Textures\\A.blp"), List.of());
		assertTrue(ModelReferences.of(Arrays.copyOf(model, model.length - 100)).isEmpty());
		assertEquals(List.of("Textures\\A.blp"), ModelReferences.of(model));
	}
}
