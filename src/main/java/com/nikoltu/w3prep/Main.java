package com.nikoltu.w3prep;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import com.nikoltu.w3prep.ui.MainWindow;

public final class Main {
	private Main() {
	}

	public static void main(final String[] args) {
		// jpackage passes the launcher's own "--cli" argument only when the user gives none.
		if (Arrays.asList(args).contains("--cli") || Boolean.getBoolean("w3prep.cli")
				|| GraphicsEnvironment.isHeadless()) {
			System.exit(Cli.run(args));
			return;
		}
		MainWindow.start();
	}
}
