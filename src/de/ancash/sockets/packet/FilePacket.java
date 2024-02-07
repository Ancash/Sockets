package de.ancash.sockets.packet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.ancash.sockets.async.client.AbstractAsyncClient;

public class FilePacket implements Serializable {

	private static final long serialVersionUID = 3685882552099359140L;

	public static final short HEADER = 100;

	private static final Callable<Void> emptyCallable = new Callable<Void>() {
		@Override
		public Void call() throws Exception {
			return null;
		}
	};
	private static final ExecutorService threadPool = Executors.newCachedThreadPool();
	private static final Map<Long, Future<Void>> running = new HashMap<>();
	private static final Map<Long, FilePacket> rootPacket = new HashMap<>();
	private static final Map<Long, Integer> runningFileCount = new HashMap<>();

	private transient Exception ex;

	private final long timestamp = System.nanoTime();
	private long root = timestamp;
	private int fileCount = 0;
	private boolean registered;
	private final String src;
	private final String target;

	private final int buf;
	private byte[] data;
	private int position;
	private boolean hasNext;

	public FilePacket(String src, String target) {
		this(src, target, 1024 * 1024);
	}

	public FilePacket(String src, String target, int buf) {
		this(src, target, true, buf);
	}

	FilePacket(String src, String target, boolean isClientSide, int buf) {
		if (src.contains("..") || src.contains("~"))
			throw new IllegalArgumentException("Invalid path " + src);
		this.position = 0;
		this.hasNext = true;
		this.src = src;
		this.target = target;
		this.registered = isClientSide;
		this.buf = buf;
		if (isClientSide) {
			synchronized (running) {
				running.put(timestamp, threadPool.submit(emptyCallable));
			}
			synchronized (runningFileCount) {
				runningFileCount.put(root, -1);
			}
			synchronized (rootPacket) {
				rootPacket.put(root, this);
			}
		}
	}

	public boolean isFinished() {
		synchronized (runningFileCount) {
			return !runningFileCount.containsKey(root) || runningFileCount.get(root) == 0;
		}
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isClientSide() {
		return rootPacket.containsKey(root);
	}

	public FilePacket getRootPacket() {
		return rootPacket.get(root);
	}

	public String getSrc() {
		return src;
	}

	public String getTarget() {
		return target;
	}

	public long getRootId() {
		return root;
	}

	public Packet toPacket() {
		Packet packet = new Packet(HEADER);
		packet.setObject(this);
		packet.setAwaitResponse(true);
		packet.isClientTarget(false);
		return packet;
	}

	private FilePacket setRoot(long root) {
		this.root = root;
		return this;
	}

	public Exception getException() {
		return ex;
	}

	public void handleSourceSide(AbstractAsyncClient client) throws IOException, InterruptedException {
		for (FilePacket fp : readFromFile())
			client.putWrite(fp.toPacket().toBytes());
	}

	private List<FilePacket> readFromFile() throws IOException {
		File srcFile = new File(getSrc());
		ArrayList<FilePacket> packets = new ArrayList<>();
		if (root == timestamp && position == 0)
			fileCount = countFiles(srcFile);
		if (!srcFile.exists()) {
			hasNext = false;
			data = null;
			packets.add(this);
			return packets;
		}

		if (srcFile.isDirectory()) {
			data = new byte[0];
			hasNext = false;
			packets.add(this);
			File[] files = srcFile.listFiles();
			if (files != null && files.length > 0)
				for (File file : files)
					packets.addAll(
							new FilePacket(src + "/" + file.getName(), target + "/" + file.getName(), false, buf).setRoot(root).readFromFile());
			return packets;
		}

		try (FileInputStream in = new FileInputStream(srcFile)) {
			for (int i = 0; i < position; i++)
				in.skip(buf);
			byte[] bytes = new byte[buf];
			int read = in.read(bytes);
			if (read != buf)
				bytes = Arrays.copyOfRange(bytes, 0, read);
			data = bytes;
			hasNext = in.available() > 0;
			position++;
			packets.add(this);
			return packets;
		}
	}

	private int countFiles(File file) {
		int cnt = 0;
		cnt++;
		if (!file.isDirectory())
			return cnt;
		File[] files = file.listFiles();
		if (files != null && files.length > 0)
			for (File f : files)
				cnt += countFiles(f);
		return cnt;
	}

	public void handleClientSide(AbstractAsyncClient client) throws IOException {
		try {
			writeToFile();
			if (hasNext)
				client.putWrite(toPacket().toBytes());
		} catch (Exception ex) {
			unregister();
			synchronized (runningFileCount) {
				runningFileCount.remove(root);
			}
			synchronized (rootPacket) {
				FilePacket fp = rootPacket.remove(root);
				if (fp != null)
					fp.ex = ex;
			}
		}
	}

	private void writeToFile() throws IOException, InterruptedException, ExecutionException {
		if (data == null)
			throw new IOException(src + " does not exist");

		if (root == timestamp && position < 2)
			synchronized (runningFileCount) {
				runningFileCount.put(root, runningFileCount.get(root) + 1 + fileCount);
			}

		if (data.length == 0) {
			Files.createDirectories(Paths.get(target));
			unregister();
			return;
		}

		if (position == 1) {
			Path path = Paths.get(target);

			if (Files.exists(path))
				Files.delete(path);

			if (!Files.exists(path)) {
				Files.createDirectories(path);
				Files.delete(path);
				Files.createFile(path);
			}
		}

		if (!registered) {
			synchronized (running) {
				running.put(timestamp, threadPool.submit(emptyCallable));
			}
			registered = true;
		}

		Future<Void> future;

		synchronized (running) {
			future = running.get(timestamp);
		}

		future.get();

		synchronized (running) {
			running.put(timestamp, threadPool.submit(new FileWriterCallable(data)));
			data = null;
		}
		if (!hasNext)
			unregister();
	}

	private void unregister() {
		synchronized (running) {
			running.remove(timestamp);
		}
		synchronized (runningFileCount) {
			runningFileCount.put(root, runningFileCount.get(root) - 1);
			if (runningFileCount.get(root) == 0)
				runningFileCount.remove(root);
		}
	}

	class FileWriterCallable implements Callable<Void> {

		private final byte[] bytes;

		public FileWriterCallable(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public Void call() throws Exception {
			Files.write(Paths.get(getTarget()), bytes, StandardOpenOption.APPEND);
			return null;
		}
	}
}