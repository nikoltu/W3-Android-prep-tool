package com.nikoltu.w3prep;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LauncherTest {
	@Test
	void readsOldAndNewJavaVersions() {
		assertEquals(8, Launcher.parseJavaVersion("1.8"));
		assertEquals(17, Launcher.parseJavaVersion("17"));
		assertEquals(21, Launcher.parseJavaVersion("21.0.2"));
		assertEquals(0, Launcher.parseJavaVersion("unknown"));
	}
}
