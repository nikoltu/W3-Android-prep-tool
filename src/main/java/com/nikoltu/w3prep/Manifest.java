package com.nikoltu.w3prep;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class Manifest {
	private Manifest() {
	}

	public static Path pathFor(final Path packageFile) {
		final String name = packageFile.getFileName().toString();
		final String stem = name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
		return packageFile.resolveSibling(stem + ".manifest.json");
	}

	public static void write(final Path file, final String gameVersion, final String profile, final String packageName,
			final List<WrittenFile> files) throws IOException {
		try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			out.write("{\n");
			field(out, "tool", "W3 Data Packer");
			field(out, "toolVersion", AppInfo.version());
			field(out, "package", packageName);
			field(out, "gameVersion", gameVersion);
			field(out, "profile", profile);
			field(out, "created", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			field(out, "note", "personal copy, do not share");
			out.write("  \"fileCount\": " + files.size() + ",\n");
			out.write("  \"files\": [\n");
			for (int i = 0; i < files.size(); i++) {
				final WrittenFile entry = files.get(i);
				out.write("    {\"path\": " + quote(entry.getZipPath()) + ", \"size\": " + entry.getSize()
						+ ", \"crc32\": " + quote(String.format("%08x", entry.getCrc())) + ", \"md5\": "
						+ quote(entry.getMd5()) + ", \"layer\": " + quote(entry.getLayer()) + "}");
				out.write(i + 1 < files.size() ? ",\n" : "\n");
			}
			out.write("  ]\n}\n");
		}
	}

	private static void field(final BufferedWriter out, final String name, final String value) throws IOException {
		out.write("  " + quote(name) + ": " + quote(value) + ",\n");
	}

	static String quote(final String value) {
		final StringBuilder builder = new StringBuilder("\"");
		for (final char c : value.toCharArray()) {
			switch (c) {
			case '"':
				builder.append("\\\"");
				break;
			case '\\':
				builder.append("\\\\");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\t':
				builder.append("\\t");
				break;
			default:
				if (c < 0x20) {
					builder.append(String.format("\\u%04x", (int) c));
				} else {
					builder.append(c);
				}
			}
		}
		return builder.append('"').toString();
	}
}
