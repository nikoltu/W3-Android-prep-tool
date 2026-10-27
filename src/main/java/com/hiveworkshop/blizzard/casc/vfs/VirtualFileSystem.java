package com.hiveworkshop.blizzard.casc.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.hiveworkshop.ReteraCASCUtils;
import com.hiveworkshop.blizzard.casc.Key;
import com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException;
import com.hiveworkshop.blizzard.casc.storage.BankStream;
import com.hiveworkshop.blizzard.casc.storage.Storage;

public final class VirtualFileSystem {
	public final class PathResult {
		private final PathNode node;
		private final byte[][] pathFragments;

		private PathResult(final PathNode node, final byte[][] pathFragments) {
			this.node = node;
			this.pathFragments = pathFragments;
		}

		public boolean existsInStorage() {
			boolean exists = true;

			if (isFile()) {
				final FileNode fileNode = (FileNode) node;
				final int fileReferenceCount = fileNode.getFileReferenceCount();
				for (int fileReferenceIndex = 0; fileReferenceIndex < fileReferenceCount; fileReferenceIndex += 1) {
					final StorageReference fileReference = fileNode.getFileReference(fileReferenceIndex);
					exists = exists && storage.hasBanks(fileReference.getEncodingKey());
				}
			}

			return exists;
		}

		public long getFileSize() {
			long size = 0L;

			if (isFile()) {
				final FileNode fileNode = (FileNode) node;
				final int fileReferenceCount = fileNode.getFileReferenceCount();
				for (int fileReferenceIndex = 0; fileReferenceIndex < fileReferenceCount; fileReferenceIndex += 1) {
					final StorageReference fileReference = fileNode.getFileReference(fileReferenceIndex);
					size = Math.max(size, fileReference.getOffset() + fileReference.getSize());
				}
			}

			return size;
		}

		public String getPath() throws CharacterCodingException {
			return convertPathFragments(pathFragments);
		}

		public byte[][] getPathFragments() {
			return pathFragments;
		}

		public boolean isFile() {
			return node instanceof FileNode;
		}

		public boolean isTVFS() {
			if (!isFile()) {
				return false;
			}

			final FileNode fileNode = (FileNode) node;
			final StorageReference fileReference = fileNode.getFileReference(0);
			return tvfsStorageReferences.containsKey(fileReference.getEncodingKey());
		}

		public ByteBuffer readFile(final ByteBuffer destBuffer) throws IOException {
			return readFile(destBuffer, false);
		}

		public ByteBuffer readFile(ByteBuffer destBuffer, final boolean verifyBankHashes) throws IOException {
			if (!isFile()) {
				throw new FileNotFoundException("result is not a file");
			}

			final long fileSize = getFileSize();
			if (fileSize > Integer.MAX_VALUE) {
				throw new OutOfMemoryError("file too big to process");
			}

			if (destBuffer == null) {
				destBuffer = ByteBuffer.allocate((int) fileSize);
			} else if (destBuffer.remaining() < fileSize) {
				throw new BufferOverflowException();
			}

			final ByteBuffer fileBuffer = destBuffer.slice();

			final FileNode fileNode = (FileNode) node;
			final int fileReferenceCount = fileNode.getFileReferenceCount();
			for (int fileReferenceIndex = 0; fileReferenceIndex < fileReferenceCount; fileReferenceIndex += 1) {
				final StorageReference fileReference = fileNode.getFileReference(fileReferenceIndex);

				final long logicalSize = fileReference.getSize();
				if (logicalSize != fileReference.getActualSize()) {
					throw new MalformedCASCStructureException("inconsistent size");
				}
				final long logicalOffset = fileReference.getOffset();

				final BankStream bankStream = storage.getBanks(fileReference.getEncodingKey());
				if (verifyBankHashes) {
					bankStream.verifyBankHashes();
				}

				fileBuffer.limit((int) (logicalOffset + logicalSize));
				fileBuffer.position((int) logicalOffset);
				while (bankStream.hasNextBank()) {
					bankStream.getBank(fileBuffer);
				}
			}

			destBuffer.position(destBuffer.position() + (int) fileSize);
			return destBuffer;
		}
	}

	public static final String CONFIGURATION_KEY_PREFIX = "vfs-";

	public static final String ROOT_KEY = "root";

	public static final Charset PATH_ENCODING = Charset.forName("UTF8");

	public static final String PATH_SEPERATOR = "\\";

	private static int compareNodePathFragments(final byte[][] pathFragments, final int fragmentIndex,
			final int fragmentOffset, final PathNode node) {
		final int nodeFragmentCount = node.getPathFragmentCount();
		if (nodeFragmentCount == 0) {
			return 0;
		}

		final byte[] nodeFragment = node.getFragment(0);
		final byte[] fragment = pathFragments[fragmentIndex];
		if ((nodeFragment.length == 0) && ((fragment.length - fragmentOffset) > 0)) {
			return 1;
		}
		return ReteraCASCUtils.arraysCompareUnsigned(fragment, fragmentOffset,
				Math.min(fragmentOffset + nodeFragment.length, fragment.length), nodeFragment, 0, nodeFragment.length);
	}

	public static byte[][] convertFilePath(final String filePath) throws CharacterCodingException {
		final String[] fragmentStrings = filePath.toLowerCase(Locale.ROOT).split("\\" + PATH_SEPERATOR);
		final byte[][] pathFragments = new byte[fragmentStrings.length][];

		final CharsetEncoder encoder = PATH_ENCODING.newEncoder();
		encoder.onMalformedInput(CodingErrorAction.REPORT);
		encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		for (int index = 0; index < fragmentStrings.length; index += 1) {
			final ByteBuffer fragmentBuffer = encoder.encode(CharBuffer.wrap(fragmentStrings[index]));
			if (fragmentBuffer.hasArray() && (fragmentBuffer.limit() == fragmentBuffer.capacity())
					&& (fragmentBuffer.position() == 0)) {
				pathFragments[index] = fragmentBuffer.array();
			} else {
				final byte[] pathFragment = new byte[fragmentBuffer.remaining()];
				fragmentBuffer.get(pathFragment);
				pathFragments[index] = pathFragment;
			}
		}

		return pathFragments;
	}

	public static String convertPathFragments(final byte[][] pathFragments) throws CharacterCodingException {
		final String[] fragmentStrings = new String[pathFragments.length];

		final CharsetDecoder decoder = PATH_ENCODING.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPORT);
		decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

		for (int index = 0; index < fragmentStrings.length; index += 1) {
			fragmentStrings[index] = decoder.decode(ByteBuffer.wrap(pathFragments[index])).toString();
		}

		return String.join(PATH_SEPERATOR, fragmentStrings);
	}

	private static boolean equalNodePathFragments(final byte[][] pathFragments, final int fragmentIndex,
			int fragmentOffset, final PathNode node) {
		final int nodeFragmentCount = node.getPathFragmentCount();
		if (nodeFragmentCount == 0) {
			return true;
		}

		if ((nodeFragmentCount == 1) && (node.getFragment(0).length == 0)) {
			return fragmentOffset == pathFragments[fragmentIndex].length;
		} else if (pathFragments.length < (fragmentIndex + nodeFragmentCount)) {
			return false;
		}

		boolean result = true;
		int nodeFragmentIndex = 0;
		while (result && (nodeFragmentIndex < nodeFragmentCount)) {
			final byte[] fragment = pathFragments[fragmentIndex + nodeFragmentIndex];
			final byte[] nodeFragment = node.getFragment(nodeFragmentIndex);
			result = result && ReteraCASCUtils.arraysEquals(fragment, fragmentOffset,
					Math.min(fragmentOffset + nodeFragment.length, fragment.length), nodeFragment, 0,
					nodeFragment.length);
			fragmentOffset = 0;
			nodeFragmentIndex += 1;
		}

		return result;
	}

	private final Storage storage;

	private final TVFSDecoder decoder = new TVFSDecoder();

	private final TVFSFile tvfsRoot;

	private final TreeMap<Key, TVFSFile> tvfsCache = new TreeMap<>();

	private final TreeMap<Key, com.hiveworkshop.blizzard.casc.StorageReference> tvfsStorageReferences = new TreeMap<>();

	public VirtualFileSystem(final Storage storage, final Map<String, String> buildConfiguration) throws IOException {
		this.storage = storage;

		int vfsNumber = 0;
		String configurationKey;
		while (buildConfiguration
				.containsKey(configurationKey = CONFIGURATION_KEY_PREFIX + Integer.toUnsignedString(++vfsNumber))) {
			final com.hiveworkshop.blizzard.casc.StorageReference storageReference = new com.hiveworkshop.blizzard.casc.StorageReference(
					configurationKey, buildConfiguration);
			tvfsStorageReferences.put(storageReference.getEncodingKey(), storageReference);
		}

		final com.hiveworkshop.blizzard.casc.StorageReference rootReference = new com.hiveworkshop.blizzard.casc.StorageReference(
				CONFIGURATION_KEY_PREFIX + ROOT_KEY, buildConfiguration);
		final ByteBuffer rootBuffer = fetchStoredBuffer(rootReference);
		tvfsRoot = decoder.loadFile(rootBuffer);

		tvfsCache.put(rootReference.getEncodingKey(), tvfsRoot);
	}

	private ByteBuffer fetchStoredBuffer(final com.hiveworkshop.blizzard.casc.StorageReference storageReference)
			throws IOException {
		final long size = storageReference.getSize();
		if (size > Integer.MAX_VALUE) {
			throw new MalformedCASCStructureException("stored data too large to process");
		}

		final BankStream bankStream = storage.getBanks(storageReference.getEncodingKey());
		final ByteBuffer storedBuffer = ByteBuffer.allocate((int) size);
		try {
			while (bankStream.hasNextBank()) {
				bankStream.getBank(storedBuffer);
			}
		} catch (final BufferOverflowException e) {
			throw new MalformedCASCStructureException("stored data is bigger than expected");
		}

		if (storedBuffer.hasRemaining()) {
			throw new MalformedCASCStructureException("stored data is smaller than expected");
		}

		storedBuffer.rewind();
		return storedBuffer;
	}

	public List<PathResult> getAllFiles() throws IOException {
		final ArrayList<PathResult> pathStringList = new ArrayList<PathResult>();

		final int rootCount = tvfsRoot.getRootNodeCount();
		for (int rootIndex = 0; rootIndex < rootCount; rootIndex += 1) {
			final PathNode root = tvfsRoot.getRootNode(rootIndex);
			recursiveFilePathRetrieve(new byte[1][0], pathStringList, root);
		}

		return pathStringList;
	}

	private void recursiveFilePathRetrieve(final byte[][] parentPathFragments, final ArrayList<PathResult> resultList,
			final PathNode currentNode) throws IOException {
		byte[][] currentPathFragments = parentPathFragments;

		final int fragmentCount = currentNode.getPathFragmentCount();
		if (fragmentCount > 0) {
			int fragmentIndex = 0;
			final byte[] fragment = currentNode.getFragment(fragmentIndex++);

			int basePathFragmentsIndex = currentPathFragments.length;
			if ((fragmentCount > 1) || (fragment.length > 0)) {
				basePathFragmentsIndex -= 1;
			}
			currentPathFragments = Arrays.copyOf(currentPathFragments, basePathFragmentsIndex + fragmentCount);

			final byte[] sourceFragment = currentPathFragments[basePathFragmentsIndex];
			byte[] joinedFragment = fragment;
			if (sourceFragment != null) {
				joinedFragment = sourceFragment;
				if (fragment.length != 0) {
					final int joinOffset = sourceFragment.length;
					joinedFragment = Arrays.copyOf(sourceFragment, joinOffset + fragment.length);
					System.arraycopy(fragment, 0, joinedFragment, joinOffset, fragment.length);
				}
			}

			currentPathFragments[basePathFragmentsIndex] = joinedFragment;
			for (; fragmentIndex < fragmentCount; fragmentIndex += 1) {
				currentPathFragments[basePathFragmentsIndex + fragmentIndex] = currentNode.getFragment(fragmentIndex);
			}
		}

		if (currentNode instanceof PrefixNode) {
			final PrefixNode prefixNode = (PrefixNode) currentNode;

			final int childCount = prefixNode.getNodeCount();
			for (int index = 0; index < childCount; index += 1) {
				recursiveFilePathRetrieve(currentPathFragments, resultList, prefixNode.getNode(index));
			}
		} else if (currentNode instanceof FileNode) {
			final FileNode fileNode = (FileNode) currentNode;

			final int fileReferenceCount = fileNode.getFileReferenceCount();
			if (fileReferenceCount == 1) {
				final Key encodingKey = fileNode.getFileReference(0).getEncodingKey();
				final TVFSFile tvfsFile = resolveTVFS(encodingKey);

				if (tvfsFile != null) {
					final byte[][] folderPathFragments = Arrays.copyOf(currentPathFragments,
							currentPathFragments.length + 1);
					folderPathFragments[currentPathFragments.length] = new byte[0];

					final int rootCount = tvfsFile.getRootNodeCount();
					for (int rootIndex = 0; rootIndex < rootCount; rootIndex += 1) {
						final PathNode root = tvfsFile.getRootNode(rootIndex);
						recursiveFilePathRetrieve(folderPathFragments, resultList, root);
					}
				}

				resultList.add(new PathResult(currentNode, currentPathFragments));
			}
		} else {
			throw new IllegalArgumentException("unsupported node type");
		}
	}

	private FileNode recursiveResolvePathFragments(final byte[][] pathFragments, int fragmentIndex, int fragmentOffset,
			final PathNode node) throws IOException {
		if (!equalNodePathFragments(pathFragments, fragmentIndex, fragmentOffset, node)) {
			return null;
		}

		final int nodeFragmentCount = node.getPathFragmentCount();
		if (nodeFragmentCount == 1) {
			final byte[] nodeFragment = node.getFragment(0);
			if (nodeFragment.length == 0) {
				fragmentIndex += 1;
				fragmentOffset = 0;
			} else {
				fragmentOffset += nodeFragment.length;
			}
		} else if (nodeFragmentCount > 1) {
			fragmentIndex += nodeFragmentCount - 1;
			fragmentOffset = node.getFragment(nodeFragmentCount - 1).length;
		}

		if (node instanceof PrefixNode) {
			final PrefixNode prefixNode = (PrefixNode) node;
			final int childCount = prefixNode.getNodeCount();

			int low = 0;
			int high = childCount - 1;
			while (low <= high) {
				final int middle = (low + high) / 2;
				final PathNode searchNode = prefixNode.getNode(middle);
				final int result = compareNodePathFragments(pathFragments, fragmentIndex, fragmentOffset, searchNode);

				if (result == 0) {
					return recursiveResolvePathFragments(pathFragments, fragmentIndex, fragmentOffset, searchNode);
				} else if (result < 0) {
					high = middle - 1;
				} else {
					low = middle + 1;
				}
			}
		} else if (node instanceof FileNode) {
			final FileNode fileNode = (FileNode) node;

			if ((fragmentIndex == (pathFragments.length - 1))
					&& (fragmentOffset == pathFragments[pathFragments.length - 1].length)) {
				return fileNode;
			} else if (fragmentOffset == pathFragments[fragmentIndex].length) {
				final int fileReferenceCount = fileNode.getFileReferenceCount();
				if (fileReferenceCount == 1) {
					final Key encodingKey = fileNode.getFileReference(0).getEncodingKey();
					final TVFSFile tvfsFile = resolveTVFS(encodingKey);

					if (tvfsFile != null) {
						if (tvfsFile.getRootNodeCount() != 1) {
							throw new MalformedCASCStructureException("logic only defined for 1 TVFS root node");
						}

						fragmentIndex += 1;
						fragmentOffset = 0;
						return recursiveResolvePathFragments(pathFragments, fragmentIndex, fragmentOffset,
								tvfsFile.getRootNode(0));
					}
				}
			}
		} else {
			throw new IllegalArgumentException("unsupported node type");
		}

		return null;
	}

	public PathResult resolvePath(final byte[][] pathFragments) throws IOException {
		if (pathFragments.length == 0) {
			throw new IllegalArgumentException("pathFragments.length must be greater than 0");
		}

		if (tvfsRoot.getRootNodeCount() != 1) {
			throw new MalformedCASCStructureException("logic only defined for 1 root node");
		}

		final FileNode result = recursiveResolvePathFragments(pathFragments, 0, 0, tvfsRoot.getRootNode(0));
		if (result == null) {
			throw new FileNotFoundException("path not in storage");
		}

		return new PathResult(result, pathFragments);
	}

	private TVFSFile resolveTVFS(final Key encodingKey) throws IOException {
		TVFSFile tvfsFile = null;
		final com.hiveworkshop.blizzard.casc.StorageReference storageReference = tvfsStorageReferences.get(encodingKey);
		if (storageReference != null) {
			synchronized (this) {
				tvfsFile = tvfsCache.get(encodingKey);
				if (tvfsFile == null) {
					final ByteBuffer rootBuffer = fetchStoredBuffer(storageReference);
					tvfsFile = decoder.loadFile(rootBuffer);

					tvfsCache.put(storageReference.getEncodingKey(), tvfsFile);
				}
			}
		}
		return tvfsFile;
	}
}
