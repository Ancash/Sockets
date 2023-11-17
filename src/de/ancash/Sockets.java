package de.ancash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

import de.ancash.cli.CLI;
import de.ancash.loki.impl.SimpleLokiPluginImpl;
import de.ancash.loki.impl.SimpleLokiPluginManagerImpl;
import de.ancash.loki.logger.PluginOutputFormatter;
import de.ancash.loki.plugin.LokiPluginClassLoader;
import de.ancash.loki.plugin.LokiPluginLoader;
import de.ancash.misc.ConversionUtil;
import de.ancash.misc.io.IFormatter;
import de.ancash.misc.io.ILoggerListener;
import de.ancash.misc.io.LoggerUtils;
import de.ancash.misc.io.SerializationUtils;
import de.ancash.sockets.async.impl.packet.client.AsyncPacketClient;
import de.ancash.sockets.async.impl.packet.client.AsyncPacketClientFactory;
import de.ancash.sockets.async.impl.packet.server.AsyncPacketServer;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCallback;
import de.ancash.sockets.packet.PacketCombiner;

public class Sockets {

	private static AsyncPacketServer serverSocket;
	private static final SimpleLokiPluginManagerImpl pluginManager = new SimpleLokiPluginManagerImpl(
			new File("plugins"));

	public static void writeAll(Packet p) throws InterruptedException {
		serverSocket.writeAllExcept(p, null);
	}

	@SuppressWarnings("nls")
	public static void stop() {
		try {
			System.out.println("Stopping...");
			System.out.println("Disabling plugins...");
			pluginManager.unload();
			System.out.println("Disabled plugins!");
			serverSocket.stop();
			Thread.sleep(1800);
			fos.close();
			Thread.sleep(200);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	static void testLatency() throws IOException, InterruptedException {
		AsyncPacketServer aps = new AsyncPacketServer("localhost", 54321, 1);
		aps.start();
		Thread.sleep(1000);
		for (int i = 0; i < 1; i++) {
			int o = i;
			new Thread(() -> {
				try {
					Thread.currentThread().setName("cl - " + o);
					AsyncPacketClient cl = new AsyncPacketClientFactory().newInstance("localhost", 54321, 1024 * 8, 1024 * 8, 1);
					Thread.sleep(1000);
					testLatency0(cl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}
	}

	static void testLatency0(AsyncPacketClient cl) throws InterruptedException {
		Packet packet = new Packet(Packet.PING_PONG);
		packet.isClientTarget(false);
		AtomicLong total = new AtomicLong();
		AtomicInteger cnt = new AtomicInteger();
		packet.setObject(System.nanoTime());
		packet.setPacketCallback(new PacketCallback() {

			@Override
			public void call(Object result) {
				Packet p = new Packet(Packet.PING_PONG);
				total.addAndGet(System.nanoTime() - (long) result);
				if (cnt.incrementAndGet() % 100 == 0) {
					System.out.println((total.get() / cnt.get()) + " ns/req");

				}
				p.resetResponse();
				p.setObject(System.nanoTime());
				p.isClientTarget(false);
				p.setPacketCallback(this);
				try {
					cl.write(p);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		cl.write(packet);
//		System.out.println(total.get() / f / 1000D + " micros/packet");
//		Thread.sleep(1000);
//		testLatency0(cl);
	}

	static void testThroughput() throws IOException, InterruptedException {
		AsyncPacketServer aps = new AsyncPacketServer("ryzen2400g", 54321, 1);
		aps.start();
		Thread.sleep(1000);
		for (int i = 0; i < 1; i++) {
			int o = i;
			new Thread(() -> {
				try {
					Thread.currentThread().setName("cl - " + o);
					AsyncPacketClient cl = new AsyncPacketClientFactory().newInstance("ryzen2400g", 54321, 1024 * 32,
							1024 * 32, 1);
					while (!cl.isConnected())
						Thread.sleep(1);
					testThroughput0(cl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}
	}

	static long now = System.currentTimeMillis();
	static AtomicLong cnt = new AtomicLong();

	static void testThroughput0(AsyncPacketClient cl) throws InterruptedException {

		while(true) {
			AtomicLong sent = new AtomicLong();

			Packet packet = new Packet(Packet.PING_PONG);
			int pl = 1024 * 16;
			packet.setObject(new byte[pl]);
			int size = packet.toBytes().remaining();
			int f = 10000;
			byte[] bb = new byte[pl];
			for (int i = 0; i < f; i++) {
				packet = new Packet(Packet.PING_PONG);
				packet.isClientTarget(false);
				packet.setObject(bb);
				packet.setPacketCallback(new PacketCallback() {

					@Override
					public void call(Object result) {
						sent.decrementAndGet();
						if (cnt.incrementAndGet() % 1000 == 0)
							System.out.println(
									((cnt.get() * size * 2) / 1024D) / ((System.currentTimeMillis() - now + 1D) / 1000D)
											+ " kbytes/s");
					}
				});
				cl.write(packet);
				sent.incrementAndGet();
			}
		}
	}

	private static File log;
	private static FileOutputStream fos;

	public static void sleep(long nanos) {
		long stop = System.nanoTime() + nanos;
		while (stop > System.nanoTime()) {
			LockSupport.parkNanos(1_000);
		}
	}

	@SuppressWarnings("nls")
	public static void main(String... args)
			throws InterruptedException, NumberFormatException, UnknownHostException, IOException {
		System.out.println("Starting Sockets...");
		testThroughput();
//		testLatency();
		if (true)
			return;
		PluginOutputFormatter pof = new PluginOutputFormatter("[" + IFormatter.PART_DATE_TIME + "] " + "["
				+ IFormatter.THREAD_NAME + "/" + IFormatter.COLOR + IFormatter.LEVEL + IFormatter.RESET + "] ["
				+ PluginOutputFormatter.PLUGIN_NAME + "] " + IFormatter.COLOR + IFormatter.MESSAGE + IFormatter.RESET,
				pluginManager, "\b\b\b");

		LoggerUtils.setOut(Level.INFO, pof);
		LoggerUtils.setErr(Level.SEVERE, pof);
		LoggerUtils.setGlobalLogger(pof);
		log = new File("logs/" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime())
				+ ".log");
		log.mkdirs();
		log.delete();
		log.createNewFile();
		fos = new FileOutputStream(log);
		pof.addListener(new ILoggerListener() {

			@Override
			public void onLog(String arg0) {
				try {
					fos.write(("\n" + arg0.replace("\t", "   ").replaceAll("\u001B\\[[;\\d]*m", "")
							.replaceAll("\\P{Print}", "")).getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		System.out.println("Using " + Runtime.getRuntime().availableProcessors() + " cores");
		Map<String, String> arguments = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				arguments.put(args[i].replaceFirst("-", ""), args[i + 1]);
				i++;
				continue;
			}
		}
		arguments.computeIfAbsent("h", s -> "localhost");
		arguments.computeIfAbsent("p", s -> "25000");
		arguments.computeIfAbsent("w", w -> "8");
		System.out.println("Address: " + arguments.get("h"));
		System.out.println("Port: " + arguments.get("p"));
		System.out.println("Packet Worker: " + arguments.get("w"));
		System.out.println("Loading plugins...");
		pluginManager.loadJars();
		pluginManager.getPluginLoader().stream().map(LokiPluginLoader::getClassLoader)
				.forEach(SerializationUtils::addClazzLoader);
		System.out.println("Loaded Plugins!");
		serverSocket = new AsyncPacketServer(arguments.get("h"), Integer.valueOf(arguments.get("p")),
				Integer.valueOf(arguments.get("w")));
		try {
			serverSocket.start();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Enabling plugins...");
		pluginManager.loadPlugins();
		System.out.println("Enabled plugins!");

		CLI cli = new CLI();
		cli.onInput(Sockets::onInput);
		cli.run();

//		while (true) {
//			String input = in.nextLine().toLowerCase();
//
//			switch (input) {
//			case "stop":
//				in.close();
//				stop();
//				return;
//			case "plugins":
//				StringBuilder builder = new StringBuilder();
//				pluginManager.getPlugins().stream().map(SimpleLokiPluginImpl::getClass).map(Class::getClassLoader)
//						.forEach(s -> builder
//								.append(", " + ((LokiPluginClassLoader<?>) s).getLoader().getDescription().getName()));
//				System.out.println("Plugins: " + builder.toString().replaceFirst(", ", ""));
//				break;
//			default:
//				System.out.println("Unknown command: " + input);
//				break;
//			}
//		}
	}

	private static void onInput(String input) {
		switch (input) {
		case "stop":
			stop();
			return;
		case "plugins":
			StringBuilder builder = new StringBuilder();
			pluginManager.getPlugins().stream().map(SimpleLokiPluginImpl::getClass).map(Class::getClassLoader).forEach(
					s -> builder.append(", " + ((LokiPluginClassLoader<?>) s).getLoader().getDescription().getName()));
			System.out.println("Plugins: " + builder.toString().replaceFirst(", ", ""));
			break;
		default:
			break;
		}
	}

	public static boolean isOpen() {
		return serverSocket.isOpen();
	}
}