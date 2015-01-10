package rh.java.http.server.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Observable;

import rh.java.http.server.HttpRequest;
import rh.java.http.server.UpgradeHandler;

public class WebSocketHandler extends Observable implements UpgradeHandler {
	
	private OutputStream outputStream;
	
	private InputStream inputStream;
	
	private String key;
	
	private Socket socket;
	
	public WebSocketHandler(Socket socket, HttpRequest request) throws IOException {
		this.socket = socket;
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
		key = request.getHeader("Sec-WebSocket-Key").getContent();
		
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			byte[] hash = sha1.digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes());
			write("HTTP/1.1 101 Switching Protocols\r\n");
			write("Upgrade: websocket\r\n");
			write("Connection: Upgrade\r\n");
			write("Sec-WebSocket-Accept: "+ Base64.getEncoder().encodeToString(hash) + "\r\n\r\n");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
	}
	
	/*
	@Override
	public boolean canUpgrade(String type, HttpRequest request) {
		if(type.equals("websocket") && request.getHeader("Sec-WebSocket-Version").getContent().equals("13")) {
			key = request.getHeader("Sec-WebSocket-Key").getContent();
			return true;
		}
		
		return false;
	}
	*/
	
	
	@Override
	public void run() {
		boolean isMasked = false;
		int payloadLength = 0;
		byte[] mask = null;
		while(socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown()) {
			try {
				byte[] data;
				data = readBytes(2);
				
				if((data[1] &  0x80) == 0x80) {
					isMasked = true;
				}
				
				payloadLength = (data[1] & ~0xF0) & 0xFF;
				
				switch(payloadLength) {
				case 126 :
						data = readBytes(2);
					break;
				case 127 :
						data = readBytes(8);
					break;
				}
				
				if(isMasked) {
					mask = readBytes(4);
				}
				
				byte[] message = readBytes(payloadLength);
				for(int o=0; o<message.length; o++) {
					int maskOffset = o % 4;
					message[o] = (byte) ((message[o]) ^ (mask[maskOffset]));
				}
				
				// Lower 4 bits of first byte are the opcode
				switch(data[0] & 0x0F) {
				case 0x0 : // Continuation frame
						
					break;
				case 0x1 : // Text frame
					
					break;
				case 0x2 : // Binary frame
					
					break;
				case 0x8 : // Connection close
						this.inputStream.close();
						this.outputStream.close();
					break;
				case 0x9 : // Ping
					PingFrame frame = new PingFrame(message);
					System.out.println("New PingFrame with length " + message.length);
					this.outputStream.write((new PongFrame(frame)).getBytes());
					break;
				case 0xA : // Pong
					
					break;
					default :
						// 0x3 - 0x7 reserved (non-control)
						// 0xB - 0xF reserved (control)
						System.err.println("Unknown opcode received.");
						this.inputStream.close();
						this.outputStream.close();
						break;
				}
				
				setChanged();
				notifyObservers(message);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}
	
	public byte[] readBytes(int totalLength) throws IOException {
		byte[] data = new byte[totalLength];
		int readBytes = 0;
		while((totalLength - readBytes) > 0) {
			readBytes += inputStream.read(data, readBytes, totalLength - readBytes);
		}
		return data;
	}

	public void write(String s) throws IOException {
		outputStream.write(s.getBytes());
	}
	
	public void write(ByteBuffer buffer) throws IOException {
		outputStream.write(buffer.array());
	}
		
	public void write(byte[] a) throws IOException {
		outputStream.write(a);
	}
	
	public void write(int a) throws IOException {
		outputStream.write(a);
	}
	
	public void close() throws IOException {
		outputStream.close();
	}
}
