package de.ancash.sockets.async.client;

public class ReadCompleteEvent {

	AbstractAsyncClient client;
	long last;
	int cnt;

	public ReadCompleteEvent setClient(AbstractAsyncClient client) {
		this.client = client;
		return this;
	}

	public ReadCompleteEvent setLast(long last) {
		this.last = last;
		return this;
	}
	
	public ReadCompleteEvent setCnt(int cnt) {
		this.cnt = cnt;
		return this;
	}
}
