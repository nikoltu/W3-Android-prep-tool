package com.nikoltu.w3prep;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.hiveworkshop.blizzard.casc.nio.DataNotLocalException;
import com.hiveworkshop.blizzard.casc.nio.HashMismatchException;

public final class PackWriter {
	private static final Set<String> STORED_EXTENSIONS = new HashSet<>(Arrays.asList("blp", "dds", "ogg", "mp3",
			"flac", "w3x", "w3m", "w3n", "jpg", "png", "mp4", "avi", "bik", "webm", "ttf", "otf", "zip"));

	private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);

	private static final int BUFFER_SIZE = 1 << 20;

	public static final class Written {
		private final List<WrittenFile> files;
		private final List<ReadFailure> failures;
		private final List<ReadFailure> notDownloaded;

		Written(final List<WrittenFile> files, final List<ReadFailure> failures, final List<ReadFailure> notDownloaded) {
			this.files = files;
			this.failures = failures;
			this.notDownloaded = notDownloaded;
		}

		public List<ReadFailure> getNotDownloaded() {
			return this.notDownloaded;
		}

		public List<WrittenFile> getFiles() {
			return this.files;
		}

		public List<ReadFailure> getFailures() {
			return this.failures;
		}
	}

	private PackWriter() {
	}

	public static Written write(final PackPlan plan, final GameStorage storage, final Path output,
			final Progress progress) throws IOException, PackCancelledException {
		return write(plan, storage, output, progress, false);
	}

	public static Written write(final PackPlan plan, final GameStorage storage, final Path output,
			final Progress progress, final boolean allowMissing) throws IOException, PackCancelledException {
		final List<WrittenFile> files = new ArrayList<>(plan.getFileCount());
		final List<ReadFailure> failures = new ArrayList<>();
		final List<ReadFailure> notDownloaded = new ArrayList<>();
		final MessageDigest md5 = newMd5();
		final CRC32 crc = new CRC32();
		boolean complete = false;
		try (FileOutputStream fileOut = new FileOutputStream(output.toFile());
				ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(fileOut, BUFFER_SIZE))) {
			zip.setLevel(Deflater.BEST_SPEED);
			for (final PlanEntry entry : plan.getEntries()) {
				progress.checkCancelled();
				progress.setCurrentFile(entry.isInline() ? entry.getZipPath() : entry.getSourcePath());
				final byte[] data;
				try {
					data = entry.isInline() ? entry.getInlineContent() : storage.read(entry.getSourcePath());
				} catch (final DataNotLocalException e) {
					notDownloaded.add(new ReadFailure(entry.getZipPath(), "not downloaded by the game yet"));
					progress.fileCopied(entry.getSize());
					continue;
				} catch (final IOException | RuntimeException e) {
					failures.add(new ReadFailure(entry.getZipPath(), describe(e)));
					progress.fileCopied(entry.getSize());
					continue;
				}
				if (!failures.isEmpty() && !allowMissing) {
					progress.fileCopied(entry.getSize());
					continue;
				}

				crc.reset();
				crc.update(data);
				md5.reset();
				final String hash = hex(md5.digest(data));

				final ZipEntry zipEntry = new ZipEntry(entry.getZipPath());
				zipEntry.setTimeLocal(ENTRY_TIME);
				if (isStored(entry.getZipPath())) {
					zipEntry.setMethod(ZipEntry.STORED);
					zipEntry.setSize(data.length);
					zipEntry.setCompressedSize(data.length);
					zipEntry.setCrc(crc.getValue());
				} else {
					zipEntry.setMethod(ZipEntry.DEFLATED);
				}
				zip.putNextEntry(zipEntry);
				zip.write(data);
				zip.closeEntry();

				files.add(new WrittenFile(entry.getZipPath(), entry.getLayer(), data.length, crc.getValue(), hash));
				progress.fileCopied(entry.getSize());
			}
			zip.finish();
			zip.flush();
			fileOut.getChannel().force(true);
			complete = true;
		} finally {
			if (!complete || (!failures.isEmpty() && !allowMissing)) {
				java.nio.file.Files.deleteIfExists(output);
			}
		}
		return new Written(files, failures, notDownloaded);
	}

	public static void verify(final Path zipFile, final List<WrittenFile> files, final Progress progress)
			throws PackException, IOException, PackCancelledException {
		final byte[] buffer = new byte[BUFFER_SIZE];
		final CRC32 crc = new CRC32();
		try (ZipFile zip = new ZipFile(zipFile.toFile())) {
			if (zip.size() != files.size()) {
				throw new PackException(PackException.Kind.WRITE_FAILED, "The package holds " + zip.size()
						+ " files, " + files.size() + " were written. The disk may be failing.");
			}
			for (final WrittenFile file : files) {
				progress.checkCancelled();
				progress.setCurrentFile(file.getZipPath());
				final ZipEntry entry = zip.getEntry(file.getZipPath());
				if (entry == null) {
					throw new PackException(PackException.Kind.WRITE_FAILED,
							"A file is missing from the package after writing: " + file.getZipPath());
				}
				crc.reset();
				long size = 0;
				try (InputStream in = zip.getInputStream(entry)) {
					int read;
					while ((read = in.read(buffer)) != -1) {
						crc.update(buffer, 0, read);
						size += read;
					}
				}
				if ((size != file.getSize()) || (crc.getValue() != file.getCrc())) {
					throw new PackException(PackException.Kind.WRITE_FAILED,
							"A file in the package does not match what was written: " + file.getZipPath()
									+ ". The disk may be failing.");
				}
				progress.bytesVerified(size);
			}
		}
	}

	static boolean isStored(final String zipPath) {
		final int dot = zipPath.lastIndexOf('.');
		return (dot != -1) && STORED_EXTENSIONS.contains(zipPath.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

	static String describe(final Throwable error) {
		if (error instanceof HashMismatchException) {
			return "content does not match the install";
		}
		if ((error instanceof FileNotFoundException) || String.valueOf(error.getMessage()).contains("missing")) {
			return "data missing from storage";
		}
		if (error instanceof UnsupportedEncodingException) {
			return "stored in a form this tool can't read";
		}
		return "could not be read (" + error.getClass().getSimpleName()
				+ (error.getMessage() == null ? "" : ": " + error.getMessage()) + ")";
	}

	static MessageDigest newMd5() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	static String hex(final byte[] bytes) {
		final StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (final byte b : bytes) {
			builder.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		return builder.toString();
	}
}
