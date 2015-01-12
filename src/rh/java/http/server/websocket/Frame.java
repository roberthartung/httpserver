package rh.java.http.server.websocket;

import java.nio.ByteBuffer;

public class Frame {
	protected ByteBuffer buffer;
	
	protected int opcode = 0;
	
	public final static int OPCODE_CONTINUATION_FRAME = 0;
	public final static int OPCODE_TEXT_FRAME = 1;
	public final static int OPCODE_BINARY_FRAME = 2;
	public final static int OPCODE_PING_FRAME = 9;
	public final static int OPCODE_PONG_FRAME = 10;
	
	public Frame(int length, int opcode) {
		this.opcode = opcode;
		int position = 0;
		if(length > 125) {
			position += 2; // Two more payload length bytes
		}
		position += 2; // Opcode + Payload length
		buffer = ByteBuffer.allocate(position + length);
		buffer.position(position);
		
		putByte(0, 0x80 | opcode);
		
		if(length <= 125) {
			buffer.put(1, (byte) length);
		} else {
			buffer.put(1, (byte) 126);
			buffer.putShort(2, (short) length);
		}
	}
	
	/**
	 * Writes an unsigned byte to the ByteBuffe
	 * @param value
	 */
	
	private void putByte(int position, int value) {
		byte b;
		if(value <= 127) {
			b = (byte) value;
		} else {
			b = (byte) -(128 - (value - 128));
		}
		
		buffer.put(position, b);
	}
	
	/**
	 * Construct new frame
	 * 
	 * @param length
	 * @param opcode
	 */
	
	public Frame(int length) {
		this(length, OPCODE_CONTINUATION_FRAME);
	}
	
	/**
	 * Internal constructor to create a full frame from its bytes
	 * @param data
	 */
	
	protected Frame(byte[] data) {
		buffer = ByteBuffer.wrap(data);
		// TODO(rh) Extract flags
	}
	
	public byte[] getBytes() {
		return buffer.array();
	}
}
