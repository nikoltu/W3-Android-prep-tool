package com.nikoltu.w3prep;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException;

public final class Analysis implements AutoCloseable {
	private final Path install;
	private final GameStorage storage;
	private final PackPlanner planner;
	private final Map<GraphicsSet, PackPlan> setPlans = new EnumMap<>(GraphicsSet.class);

	public Analysis(final Path install, final GameStorage storage) throws IOException {
		this.install = install;
		this.storage = storage;
		this.planner = new PackPlanner(storage.listEntries());
		for (final GraphicsSet set : GraphicsSet.values()) {
			this.setPlans.put(set, this.planner.planSet(set));
		}
	}

	public static Analysis open(final Path install) throws PackException {
		GameStorage storage = null;
		try {
			storage = new CascGameStorage(install);
			final Analysis analysis = new Analysis(install, storage);
			storage = null;
			return analysis;
		} catch (final MalformedCASCStructureException e) {
			final String version = GameInstall.readVersion(install);
			throw new PackException(PackException.Kind.UNKNOWN_FORMAT,
					(version == null ? "This game version" : "Game version " + version)
							+ " stores its files in a way this tool can't read (" + e.getMessage()
							+ "). The tool works with the current game version from the game launcher. "
							+ "Nothing was written.",
					e);
		} catch (final IOException | RuntimeException e) {
			throw new PackException(PackException.Kind.UNKNOWN_FORMAT,
					"The game's storage could not be read (" + e.getClass().getSimpleName()
							+ (e.getMessage() == null ? "" : ": " + e.getMessage()) + where(e) + "). Nothing was written.",
					e);
		} finally {
			if (storage != null) {
				try {
					storage.close();
				} catch (final IOException e) {
				}
			}
		}
	}

	static String where(final Throwable error) {
		for (final StackTraceElement frame : error.getStackTrace()) {
			if (frame.getClassName().startsWith("com.hiveworkshop") || frame.getClassName().startsWith("com.nikoltu")) {
				final String name = frame.getClassName();
				return " at " + name.substring(name.lastIndexOf('.') + 1) + "." + frame.getMethodName() + ":"
						+ frame.getLineNumber();
			}
		}
		return "";
	}

	public Path getInstall() {
		return this.install;
	}

	public GameStorage getStorage() {
		return this.storage;
	}

	public List<String> describeUnreadableNames() {
		final List<String> names = this.storage.getUnreadableNames();
		final List<String> lines = new ArrayList<>();
		if (names.isEmpty()) {
			return lines;
		}
		lines.add(names.size() + " listed files have names that are not valid text and were skipped, for example:");
		for (int i = 0; i < Math.min(5, names.size()); i++) {
			lines.add("  " + names.get(i));
		}
		return lines;
	}

	public String getLocale() {
		return PackPlanner.displayLocale(this.planner.getLocale());
	}

	public String getVersion() {
		return this.storage.getVersion();
	}

	public PackPlan planClassic(final PackOptions options) throws PackException {
		return this.planner.planClassic(options);
	}

	public PackPlan planSet(final GraphicsSet set) {
		return this.setPlans.get(set);
	}

	public List<PackPlan> plans(final PackOptions options) throws PackException {
		final List<PackPlan> plans = new ArrayList<>();
		plans.add(planClassic(options));
		for (final GraphicsSet set : options.getSets()) {
			final PackPlan plan = planSet(set);
			if (plan.getFileCount() > 0) {
				plans.add(plan);
			}
		}
		return plans;
	}

	@Override
	public void close() throws IOException {
		this.storage.close();
	}
}
