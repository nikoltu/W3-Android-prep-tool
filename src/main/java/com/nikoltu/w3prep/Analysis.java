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
			throw new PackException(PackException.Kind.UNKNOWN_FORMAT,
					"This game version stores files in a way this tool does not know yet (" + e.getMessage()
							+ "). Nothing was written.",
					e);
		} catch (final IOException | RuntimeException e) {
			throw new PackException(PackException.Kind.UNKNOWN_FORMAT,
					"The game's storage could not be read (" + e.getClass().getSimpleName()
							+ (e.getMessage() == null ? "" : ": " + e.getMessage()) + "). Nothing was written.",
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

	public Path getInstall() {
		return this.install;
	}

	public GameStorage getStorage() {
		return this.storage;
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
