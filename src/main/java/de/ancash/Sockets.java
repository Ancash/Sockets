package de.ancash;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.plugin.Plugin;

import de.ancash.misc.IFormatter;
import de.ancash.misc.LoggerUtils;
import de.ancash.plugin.IPluginManager;
import de.ancash.sockets.async.impl.packet.server.AsyncPacketServer;
import de.ancash.sockets.packet.Packet;

public class Sockets {
	
	private static AsyncPacketServer serverSocket;
	private static IPluginManager pluginManager;
	
	public static void writeAll(Packet p) {
		serverSocket.writeAllExcept(p, null);
	}
	
	public static void stop() {
		System.out.println("Stopping...");
		System.out.println("Disabling plugins...");
		pluginManager.disablePlugins();
		System.out.println("Disabled plugins!");
		try {
			serverSocket.stop();
			Thread.sleep(2000);
			System.exit(0);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String...args) throws InterruptedException, NumberFormatException, UnknownHostException, IOException {
		System.out.println("Starting Sockets...");
		LoggerUtils.setOut("[" + IFormatter.PART_DATE_TIME + "] "
				+ "[" + IFormatter.THREAD_NAME + "/"
				+ IFormatter.COLOR + IFormatter.LEVEL + IFormatter.RESET + "] "
				+ IFormatter.COLOR + IFormatter.MESSAGE + IFormatter.RESET);
		
		LoggerUtils.setErr("[" + IFormatter.PART_DATE_TIME + "] "
				+ "[" + IFormatter.THREAD_NAME + "/"
				+ IFormatter.COLOR + IFormatter.LEVEL + IFormatter.RESET + "] "
				+ IFormatter.COLOR + IFormatter.MESSAGE + IFormatter.RESET);
		LoggerUtils.setUpGlobalLogger("[" + IFormatter.PART_DATE_TIME + "] "
				+ "[" + IFormatter.THREAD_NAME + "/"
				+ IFormatter.COLOR + IFormatter.LEVEL + IFormatter.RESET + "] "
				+ IFormatter.COLOR + IFormatter.MESSAGE + IFormatter.RESET, true);
		
		System.out.println("Using " + Runtime.getRuntime().availableProcessors() + " cores");
		Map<String, String> arguments = new HashMap<>();
		for(int i = 0; i<args.length; i++) {
			if(args[i].startsWith("-")) {
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
		pluginManager = new IPluginManager("plugins");
		System.out.println("Loading plugins...");
		pluginManager.loadPlugins();
		System.out.println("Loaded Plugins!");
		serverSocket = new AsyncPacketServer(arguments.get("h"), Integer.valueOf(arguments.get("p")), Integer.valueOf(arguments.get("w")));
		try {
			serverSocket.start();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Enabling plugins...");
		pluginManager.enablePlugins();
		System.out.println("Enabled plugins!");
		Scanner in = new Scanner(System.in);
		while(true) {
			String input = in.nextLine().toLowerCase();
			
			switch (input) {
			case "stop":
				in.close();
				stop();
				return;
			case "plugins":
				StringBuilder builder = new StringBuilder();
				Arrays.asList(pluginManager.getPlugins()).stream().map(Plugin::getName).forEach(s -> builder.append(", " + s));
				System.out.println("Plugins: " + builder.toString().replaceFirst(", ", ""));
				break;
			default:
				System.out.println("Unknown command: " + input);
				break;
			}
		}
	}

	public static boolean isOpen() {
		return serverSocket.isOpen();
	}
}