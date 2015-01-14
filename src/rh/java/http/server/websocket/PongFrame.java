package rh.java.http.server.websocket;

public class PongFrame extends Frame {
	public PongFrame(PingFrame pingFrame) {
		super(OPCODE_PONG_FRAME);
	}
}
