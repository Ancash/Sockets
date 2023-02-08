package de.ancash;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
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
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	@SuppressWarnings("nls")
	public static void main(String... args)
			throws InterruptedException, NumberFormatException, UnknownHostException, IOException {
		System.out.println("Starting Sockets...");
		PluginOutputFormatter pof = new PluginOutputFormatter("[" + IFormatter.PART_DATE_TIME + "] " + "["
				+ IFormatter.THREAD_NAME + "/" + IFormatter.COLOR + IFormatter.LEVEL + IFormatter.RESET + "] ["
				+ PluginOutputFormatter.PLUGIN_NAME + "] " + IFormatter.COLOR + IFormatter.MESSAGE + IFormatter.RESET,
				pluginManager, "\b\b\b");
		
		LoggerUtils.setOut(Level.INFO, pof);
		LoggerUtils.setErr(Level.SEVERE, pof);
		LoggerUtils.setGlobalLogger(pof);

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
			pluginManager.getPlugins().stream().map(SimpleLokiPluginImpl::getClass).map(Class::getClassLoader)
					.forEach(s -> builder
							.append(", " + ((LokiPluginClassLoader<?>) s).getLoader().getDescription().getName()));
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