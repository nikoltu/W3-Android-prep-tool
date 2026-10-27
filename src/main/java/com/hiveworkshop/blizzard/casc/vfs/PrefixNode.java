package com.hiveworkshop.blizzard.casc.vfs;

import java.util.List;

public class PrefixNode extends PathNode {
	private final PathNode[] nodes;

	protected PrefixNode(final List<byte[]> pathFragments, final List<PathNode> nodes) {
		super(pathFragments);
		this.nodes = nodes.toArray(new PathNode[0]);
	}

	public int getNodeCount() {
		return nodes.length;
	}

	public PathNode getNode(final int index) {
		return nodes[index];
	}
}
