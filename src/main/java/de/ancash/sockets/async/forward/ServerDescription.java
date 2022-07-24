package de.ancash.sockets.async.forward;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerDescription {

	public final String host;
    public final int port;
    
    private AtomicBoolean isAlive = new AtomicBoolean(true);
    private final Set<AsyncForwardConnection> connected = new HashSet<>();
    
    public ServerDescription(String host, int port) {
       this.host = host;
       this.port = port;
    }
    
    public int countConnected() {
    	return connected.size();
    }
    
    public void clientConnected(AsyncForwardConnection asyncForwardConnection) {
    	synchronized (connected) {
    		connected.add(asyncForwardConnection);
    	}
    }
    
    public boolean isAlive() {
    	return isAlive.get();
    }
    
    public void setAlive() {
    	isAlive.set(true);
    }
    
    public void dead() {
    	synchronized (connected) {
    		if(!connected.isEmpty()) return;
    		connected.clear();
    		isAlive.set(false);
    	}
    }

	public void clientDisconnected(AsyncForwardConnection asyncForwardConnection) {
		synchronized (connected) {
			connected.remove(asyncForwardConnection);
		}
	}
}