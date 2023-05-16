package de.ancash.sockets.async.forward;

import java.io.IOException;
import java.net.Socket;

public class ForwardCheckAliveThread implements Runnable {

	private final ServerDescription[] serverDesc;
	private final long interval;

	public ForwardCheckAliveThread(ServerDescription[] serverDesc, long interval) {
		this.serverDesc = serverDesc;
		this.interval = interval;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(interval);
			} catch (InterruptedException ie) {
				return;
			}
			checkAllDeadServers();
		}
	}

	private void checkAllDeadServers() {
		for (int i = 0; i < serverDesc.length; i++)
			if (!serverDesc[i].isAlive())
				if (alive(serverDesc[i].host, serverDesc[i].port))
					serverDesc[i].setAlive();
	}

	private boolean alive(String host, int port) {
		boolean result = false;
		try {
			Socket s = new Socket(host, port);
			result = true;
			s.close();
		} catch (IOException ioe) {

		}
		return result;
	}

}