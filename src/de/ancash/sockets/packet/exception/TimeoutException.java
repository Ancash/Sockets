package de.ancash.sockets.packet.exception;

public class TimeoutException extends RuntimeException {

	private static final long serialVersionUID = 8790435020735545883L;

	public TimeoutException(String str, Exception ex) {
		super(str, ex);
	}
	
	public TimeoutException(String str) {
		super(str);
	}
	
	public TimeoutException() {
		super();
	}
}