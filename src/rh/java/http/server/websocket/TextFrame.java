package rh.java.http.server.websocket;

public class TextFrame extends Frame {

	protected TextFrame(String contents) {
		super(contents.getBytes().length, OPCODE_TEXT_FRAME);
		buffer.put(contents.getBytes());
	}
}