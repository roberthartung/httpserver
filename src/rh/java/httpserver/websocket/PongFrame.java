package rh.java.httpserver.websocket;

public class PongFrame extends Frame {
	public PongFrame(PingFrame pingFrame) {
		super(0);
	}
}
