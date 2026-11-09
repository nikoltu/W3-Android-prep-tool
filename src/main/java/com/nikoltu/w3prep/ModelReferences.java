package com.nikoltu.w3prep;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The files a binary model (MDX) names: its textures and the models or textures its particle emitters spawn. The
 * format is the same chunked layout in Classic, DE and HD models.
 */
public final class ModelReferences {
	private static final int TEXTURE_SIZE = 268;
	private static final int PATH_SIZE = 260;
	/** A particle emitter's emission rate, gravity, longitude and latitude, between its node and its path. */
	private static final int EMITTER_FIELDS = 16;

	private ModelReferences() {
	}

	/** The named files, in the order the model names them; empty for data that is not a binary model. */
	public static List<String> of(final byte[] model) {
		final List<String> paths = new ArrayList<>();
		if ((model.length < 4) || (model[0] != 'M') || (model[1] != 'D') || (model[2] != 'L') || (model[3] != 'X')) {
			return paths;
		}
		final ByteBuffer buffer = ByteBuffer.wrap(model).order(ByteOrder.LITTLE_ENDIAN);
		int offset = 4;
		while ((offset + 8) <= model.length) {
			final String tag = new String(model, offset, 4, StandardCharsets.US_ASCII);
			final long size = buffer.getInt(offset + 4) & 0xFFFFFFFFL;
			final int start = offset + 8;
			if ((start + size) > model.length) {
				break;
			}
			final int end = (int) (start + size);
			if (tag.equals("TEXS")) {
				for (int texture = start; (texture + TEXTURE_SIZE) <= end; texture += TEXTURE_SIZE) {
					addPath(paths, model, texture + 4);
				}
			}
			else if (tag.equals("PREM")) {
				int emitter = start;
				while ((emitter + 8) <= end) {
					final long emitterSize = buffer.getInt(emitter) & 0xFFFFFFFFL;
					final long nodeSize = buffer.getInt(emitter + 4) & 0xFFFFFFFFL;
					final long path = emitter + 4 + nodeSize + EMITTER_FIELDS;
					if ((emitterSize < 8) || ((emitter + emitterSize) > end) || ((path + PATH_SIZE) > end)) {
						break;
					}
					addPath(paths, model, (int) path);
					emitter += (int) emitterSize;
				}
			}
			offset = end;
		}
		return paths;
	}

	private static void addPath(final List<String> paths, final byte[] model, final int offset) {
		int length = 0;
		while ((length < PATH_SIZE) && (model[offset + length] != 0)) {
			length++;
		}
		final String path = new String(model, offset, length, StandardCharsets.UTF_8).trim();
		if (!path.isEmpty() && !paths.contains(path)) {
			paths.add(path);
		}
	}
}
