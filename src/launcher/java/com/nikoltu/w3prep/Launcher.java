package com.nikoltu.w3prep;

import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;

public final class Launcher {
	static final int REQUIRED_JAVA = 17;
	static final String RELEASES = "https://github.com/nikoltu/W3-Android-prep-tool/releases/latest";

	private Launcher() {
	}

	public static void main(final String[] args) {
		final boolean console = GraphicsEnvironment.isHeadless() || contains(args, "--cli");
		final int version = parseJavaVersion(System.getProperty("java.specification.version", "0"));
		if (version < REQUIRED_JAVA) {
			report(console, "W3 Data Packer needs Java " + REQUIRED_JAVA + " or newer. This computer runs Java "
					+ System.getProperty("java.version") + ".\n\nDownload the package for your system instead. It "
					+ "includes its own Java and needs nothing installed:\n" + RELEASES);
			System.exit(1);
			return;
		}
		try {
			Class.forName("com.nikoltu.w3prep.Main").getMethod("main", String[].class).invoke(null, (Object) args);
		} catch (final InvocationTargetException e) {
			fail(console, e.getCause() == null ? e : e.getCause());
		} catch (final Exception | LinkageError e) {
			fail(console, e);
		}
	}

	static int parseJavaVersion(final String specification) {
		String value = specification.trim();
		if (value.startsWith("1.")) {
			value = value.substring(2);
		}
		final int dot = value.indexOf('.');
		if (dot != -1) {
			value = value.substring(0, dot);
		}
		try {
			return Integer.parseInt(value);
		} catch (final NumberFormatException e) {
			return 0;
		}
	}

	private static boolean contains(final String[] args, final String value) {
		for (final String arg : args) {
			if (value.equals(arg)) {
				return true;
			}
		}
		return false;
	}

	private static void fail(final boolean console, final Throwable error) {
		final StringWriter trace = new StringWriter();
		error.printStackTrace(new PrintWriter(trace));
		final String[] lines = trace.toString().split("\\r?\\n");
		final StringBuilder shortTrace = new StringBuilder();
		for (int i = 0; (i < lines.length) && (i < 8); i++) {
			shortTrace.append(lines[i]).append('\n');
		}
		report(console, "W3 Data Packer could not start.\n\n" + shortTrace
				+ "\nPlease report this text on the project page, or use the package for your system:\n" + RELEASES);
		System.exit(1);
	}

	private static void report(final boolean console, final String message) {
		if (console) {
			System.err.println(message);
			return;
		}
		try {
			JOptionPane.showMessageDialog(null, message, "W3 Data Packer", JOptionPane.ERROR_MESSAGE);
		} catch (final RuntimeException | LinkageError e) {
			System.err.println(message);
		}
	}
}
