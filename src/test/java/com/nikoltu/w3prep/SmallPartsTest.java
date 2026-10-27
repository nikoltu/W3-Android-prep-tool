package com.nikoltu.w3prep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SmallPartsTest {
	@Test
	void parsesAdbDevices() {
		final List<Adb.Device> devices = Adb.parseDevices("* daemon started successfully\n"
				+ "List of devices attached\n"
				+ "R52T1234ABC    device usb:1-1 product:gts8uwifi model:SM_X900 device:gts8u transport_id:1\n"
				+ "emulator-5554  unauthorized transport_id:2\n\n");
		assertEquals(2, devices.size());
		assertEquals("SM-X900", devices.get(0).getName());
		assertTrue(devices.get(0).isReady());
		assertEquals("emulator-5554", devices.get(1).getName());
		assertFalse(devices.get(1).isReady());
	}

	@Test
	void formatsLikeExplorer() {
		assertEquals("21 627", Sizes.count(21627));
		assertEquals("999", Sizes.count(999));
		assertEquals("2.20 GB", Sizes.format((long) (2.2 * 1024 * 1024 * 1024)));
		assertEquals("about 40 s", Sizes.duration(40));
		assertEquals("about 2 min", Sizes.duration(110));
	}

	@Test
	void iniMatchesEngineShape() {
		assertEquals("[DataSources]\r\nCount=2\r\nType00=Folder\r\nPath00=\"C:\\a\"\r\nType01=Folder\r\n"
				+ "Path01=\"C:\\a\\_hd.w3mod\"\r\n", DesktopFolder.iniText(List.of("C:\\a", "C:\\a\\_hd.w3mod")));
	}

	@Test
	void storesOnlyCompressedFormats() {
		assertTrue(PackWriter.isStored("units/a.blp"));
		assertTrue(PackWriter.isStored("sound/a.OGG"));
		assertFalse(PackWriter.isStored("units/a.mdx"));
		assertFalse(PackWriter.isStored("ui/a.txt"));
	}

	@Test
	void blockCheckStaysOnWhenAnySampleMatches() {
		assertTrue(new BlockCheck(3, 2, 0).isEnabled());
		assertFalse(new BlockCheck(0, 20, 0).isEnabled());
		assertFalse(BlockCheck.notAvailable().isEnabled());
		assertTrue(new BlockCheck(0, 20, 0).describe().contains("off"));
	}

	@Test
	void reportGroupsByFolderAndLayer() {
		final PackPlan plan = new PackPlan("p.zip", "P", List.of(new PlanEntry("units/a.mdx", "s1", "root", 10),
				new PlanEntry("units/b.mdx", "s2", "_deprecated", 30), new PlanEntry("ui/c.txt", "s3", "root", 5),
				new PlanEntry("_tilesets/l.w3mod/t.blp", "s4", "root", 7), PlanEntry.inline(".nomedia", new byte[0])),
				List.of("ui\\gone.txt"));
		final String text = String.join("\n", PlanReport.lines(plan));
		assertTrue(text.startsWith("p.zip: 4 files, 52 B"));
		assertTrue(text.contains("root 3"));
		assertTrue(text.contains("_deprecated 1"));
		assertTrue(text.indexOf("units") < text.indexOf("ui "));
		assertTrue(text.contains("_tilesets"));
		assertEquals("_hd.w3mod/units", PlanReport.folderOf("_hd.w3mod/units/a.mdx"));
		assertTrue(text.contains("not on this install: 1 listed files"));
		assertEquals("(top level files)", PlanReport.folderOf("readme.txt"));
	}

	@Test
	void blockSampleIsSpreadAndSmall() {
		final java.util.List<PlanEntry> entries = new java.util.ArrayList<>();
		for (int i = 0; i < 100; i++) {
			entries.add(new PlanEntry("f" + i, "s" + i, "root", i == 50 ? 50_000_000 : 100));
		}
		final List<String> sample = new PackPlan("p.zip", "P", entries, List.of()).sampleForBlockCheck();
		assertEquals(BlockCheck.SAMPLE_SIZE, sample.size());
		assertFalse(sample.contains("s50"));
		assertEquals("s0", sample.get(0));
	}

	@Test
	void manifestQuotesSafely() {
		assertEquals("\"a\\\\b \\\"c\\\"\\n\"", Manifest.quote("a\\b \"c\"\n"));
	}
}
