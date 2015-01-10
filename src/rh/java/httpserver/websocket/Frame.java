package rh.java.httpserver.websocket;

import java.nio.ByteBuffer;

public class Frame {
	ByteBuffer buffer;
	public Frame(int length) {
		int position = 0;
		if(length > 125) {
			position += 2; // Two more payload length bytes
		}
		position += 2; // Opcode + Payload length
		buffer = ByteBuffer.allocate(position + length);
		buffer.position(position);
	}
	
	protected Frame(byte[] data) {
		buffer = ByteBuffer.wrap(data);
	}
	
	public byte[] getBytes() {
		return buffer.array();
	}
}
