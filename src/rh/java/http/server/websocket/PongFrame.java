package rh.java.http.server.websocket;

public class PongFrame extends Frame {
	public PongFrame(PingFrame pingFrame) {
		super(0);
	}
}
