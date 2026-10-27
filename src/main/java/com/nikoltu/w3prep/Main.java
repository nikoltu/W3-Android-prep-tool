package com.nikoltu.w3prep;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import com.nikoltu.w3prep.ui.MainWindow;

public final class Main {
	private Main() {
	}

	public static void main(final String[] args) {
		if (Arrays.asList(args).contains("--cli") || GraphicsEnvironment.isHeadless()) {
			System.exit(Cli.run(args));
			return;
		}
		MainWindow.start();
	}
}
