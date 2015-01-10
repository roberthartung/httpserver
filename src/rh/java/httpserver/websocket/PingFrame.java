package rh.java.httpserver.websocket;

public class PingFrame extends Frame {
	public PingFrame() {
		super(0);
	}
	
	public PingFrame(byte[] data) {
		super(data.length);
	}
}
