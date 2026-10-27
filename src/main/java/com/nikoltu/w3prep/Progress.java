package com.nikoltu.w3prep;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class Progress {
	public enum Phase {
		CHECK_INSTALL, COPY, VERIFY, DESKTOP_FOLDER, DONE
	}

	private final long totalBytes;
	private final int totalFiles;
	private final boolean desktopFolder;
	private final AtomicLong copiedBytes = new AtomicLong();
	private final AtomicLong verifiedBytes = new AtomicLong();
	private final AtomicLong unpackedBytes = new AtomicLong();
	private final AtomicInteger copiedFiles = new AtomicInteger();
	private final AtomicBoolean cancelled = new AtomicBoolean();
	private volatile Phase phase = Phase.CHECK_INSTALL;
	private volatile String currentFile = "";

	public Progress(final long totalBytes, final int totalFiles, final boolean desktopFolder) {
		this.totalBytes = Math.max(1, totalBytes);
		this.totalFiles = totalFiles;
		this.desktopFolder = desktopFolder;
	}

	public long getTotalBytes() {
		return this.totalBytes;
	}

	public int getTotalFiles() {
		return this.totalFiles;
	}

	public Phase getPhase() {
		return this.phase;
	}

	public void setPhase(final Phase phase) {
		this.phase = phase;
	}

	public String getCurrentFile() {
		return this.currentFile;
	}

	public void setCurrentFile(final String currentFile) {
		this.currentFile = currentFile;
	}

	public void fileCopied(final long bytes) {
		this.copiedBytes.addAndGet(bytes);
		this.copiedFiles.incrementAndGet();
	}

	public void bytesVerified(final long bytes) {
		this.verifiedBytes.addAndGet(bytes);
	}

	public void bytesUnpacked(final long bytes) {
		this.unpackedBytes.addAndGet(bytes);
	}

	public long getCopiedBytes() {
		return this.copiedBytes.get();
	}

	public int getCopiedFiles() {
		return this.copiedFiles.get();
	}

	public double getFraction() {
		if (this.phase == Phase.DONE) {
			return 1;
		}
		final double copyWeight = this.desktopFolder ? 0.62 : 0.88;
		final double verifyWeight = 0.12;
		final double unpackWeight = this.desktopFolder ? 0.26 : 0;
		final double fraction = ((copyWeight * this.copiedBytes.get()) + (verifyWeight * this.verifiedBytes.get())
				+ (unpackWeight * this.unpackedBytes.get())) / this.totalBytes;
		return Math.min(0.999, fraction);
	}

	public void cancel() {
		this.cancelled.set(true);
	}

	public boolean isCancelled() {
		return this.cancelled.get();
	}

	void checkCancelled() throws PackCancelledException {
		if (this.cancelled.get()) {
			throw new PackCancelledException();
		}
	}
}
