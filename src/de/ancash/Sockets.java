package de.ancash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import de.ancash.cli.CLI;
import de.ancash.loki.impl.SimpleLokiPluginImpl;
import de.ancash.loki.impl.SimpleLokiPluginManagerImpl;
import de.ancash.loki.logger.PluginOutputFormatter;
import de.ancash.loki.plugin.LokiPluginClassLoader;
import de.ancash.loki.plugin.LokiPluginLoader;
import de.ancash.misc.io.IFormatter;
import de.ancash.misc.io.ILoggerListener;
import de.ancash.misc.io.LoggerUtils;
import de.ancash.misc.io.SerializationUtils;
import de.ancash.sockets.async.impl.packet.server.AsyncPacketServer;
import de.ancash.sockets.packet.Packet;

public class Sockets {

	private static AsyncPacketServer serverSocket;
	private static final SimpleLokiPluginManagerImpl pluginManager = new SimpleLokiPluginManagerImpl(
			new File("plugins"));

	public static void writeAll(Packet p) {
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

//	static void testLatency() throws IOException, InterruptedException {
//		AsyncPacketServer aps = new AsyncPacketServer("localhost", 54321, 6);
//		aps.start();
//		Thread.sleep(1000);
//		for (int i = 0; i < 10; i++) {
//			int o = i;
//			new Thread(() -> {
//				try {
//					Thread.currentThread().setName("cl - " + o);
//					AsyncPacketClient cl = new AsyncPacketClientFactory().newInstance("localhost", 54321, 1024 * 8,
//							1024 * 8, 1);
//					Thread.sleep(1000);
//					testLatency0(cl);
//				} catch (Exception e) {
//					// TODO: handle exception
//				}
//			}).start();
//		}
//	}
//
//	static void testLatency0(AsyncPacketClient cl) throws InterruptedException {
//		Packet packet = new Packet(Packet.PING_PONG);
//		packet.setAwaitResponse(true);
//		packet.isClientTarget(false);
//		long total = 0;
//		int f = 50000;
//		for (int i = 0; i < f; i++) {
//			packet.setSerializable(System.nanoTime());
//			cl.write(packet);
//			Optional<Packet> opt = packet.awaitResponse(100);
//			total += System.nanoTime() - (long) opt.get().getSerializable();
////			if(i % 10000 == 0)
////				System.out.println(i);
//			packet.resetResponse();
//		}
//		System.out.println(total / f / 1000D + " micros/packet");
//		testLatency0(cl);
//	}
//
//	static void testThroughput() throws IOException, InterruptedException {
//		AsyncPacketServer aps = new AsyncPacketServer("localhost", 54321, 4);
//		aps.setThreads(4);
//		aps.start();
//		Thread.sleep(1000);
//		for (int i = 0; i < 10; i++) {
//			int o = i;
//			new Thread(() -> {
//				try {
//					Thread.currentThread().setName("cl - " + o);
//					AsyncPacketClient cl = new AsyncPacketClientFactory().newInstance("localhost", 54321, 1024 * 8,
//							1024 * 8, 1);
//					Thread.sleep(1000);
//					now = System.currentTimeMillis();
//					testThroughput0(cl);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}).start();
//		}
//	}
//
//	static long now = System.currentTimeMillis();
//	static AtomicLong cnt = new AtomicLong();;
//
//	static void testThroughput0(AsyncPacketClient cl) throws InterruptedException {
//
//		AtomicLong sent = new AtomicLong();
//
//		Packet packet = new Packet(Packet.PING_PONG);
//		int pl = 1024 * 64;
//		packet.setSerializable(new byte[pl]);
//		int size = packet.toBytes().remaining();
//		int f = 10000;
//		for (int i = 0; i < f; i++) {
//			packet = new Packet(Packet.PING_PONG);
//			packet.isClientTarget(false);
//			packet.setSerializable(new byte[pl]);
//			packet.setPacketCallback(new PacketCallback() {
//
//				@Override
//				public void call(Object result) {
//					sent.decrementAndGet();
//					if (cnt.incrementAndGet() % 1000 == 0)
//						System.out.println(
//								((cnt.get() * size * 2) / 1024D) / ((System.currentTimeMillis() - now + 1D) / 1000D)
//										+ " kbytes/s");
//				}
//			});
//			cl.write(packet);
//			sent.incrementAndGet();
//		}
//		testThroughput0(cl);
//	}

	private static File log;
	private static FileOutputStream fos;

	@SuppressWarnings("nls")
	public static void main(String... args)
			throws InterruptedException, NumberFormatException, UnknownHostException, IOException {
		System.out.println("Starting Sockets...");
//		testThroughput();
//		testLatency();
//		if(true)
//			return;
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