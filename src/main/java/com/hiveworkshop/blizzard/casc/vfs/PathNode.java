package com.hiveworkshop.blizzard.casc.vfs;

import java.util.List;

public abstract class PathNode {
	private final byte[][] pathFragments;

	protected PathNode(final List<byte[]> pathFragments) {
		this.pathFragments = pathFragments.toArray(new byte[0][]);
	}

	public int getPathFragmentCount() {
		return pathFragments.length;
	}

	public byte[] getFragment(final int index) {
		return pathFragments[index];
	}
}
