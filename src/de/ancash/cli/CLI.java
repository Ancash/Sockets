package de.ancash.cli;

import java.util.Scanner;
import java.util.function.Consumer;

import de.ancash.libs.org.bukkit.event.EventManager;

public class CLI implements Runnable{

	private Consumer<String> c;
	
	public void onInput(Consumer<String> c) {
		this.c = c;
	}
	
	@Override
	public void run() {
		Scanner in = new Scanner(System.in);
		while (!Thread.interrupted()) {
			String input = in.nextLine();
			EventManager.callEvent(new CLIEvent(input));
			if(c != null)
				c.accept(input);
		}
		in.close();
	}
	
}
