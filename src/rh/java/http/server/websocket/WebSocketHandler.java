package rh.java.http.server.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

import rh.java.http.server.HttpRequest;
import rh.java.http.server.UpgradeHandler;

public class WebSocketHandler extends Observable implements UpgradeHandler {
	
	private static Logger logger = Logger.getLogger(WebSocketHandler.class.getName());
	
	static {
		logger.setLevel(Level.ALL);
	}
	
	private OutputStream outputStream;
	
	private InputStream inputStream;
	
	private String key;
	
	private Socket socket;
	
	public WebSocketHandler(Socket socket, HttpRequest request) throws IOException {
		logger.finest("WebSocket upgrade for " + socket.getRemoteSocketAddress());
		this.socket = socket;
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
		key = request.getFirstHeader("Sec-WebSocket-Key");
		
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
				
				if(data == null) {
					break;
				}
				
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
				if(message == null) {
					logger.severe("message is null");
				}
				
				try {
					for(int o=0; o<message.length; o++) {
						int maskOffset = o % 4;
						message[o] = (byte) ((message[o]) ^ (mask[maskOffset]));
					}
				} catch(NullPointerException e) {
					logger.severe("Error while retreiving frame of length " + payloadLength);
					/*
					for(int o=0; o<message.length; o++) {
						int maskOffset = o % 4;
						logger.finest("message["+o+"] = message["+o+"]("+message[o]+") ^ mask["+maskOffset+"]("+mask[maskOffset]+")");
					}
					*/
				}
				
				// Lower 4 bits of first byte are the opcode
				switch(data[0] & 0x0F) {
				case 0x0 : // Continuation frame
					logger.finest("Continuation frame received.");
					break;
				case 0x1 : // Text frame
					logger.finest("Text frame received.");
					break;
				case 0x2 : // Binary frame
					logger.finest("Binary frame received.");
					break;
				case 0x8 : // Connection close
					logger.finest("close frame received.");
						this.inputStream.close();
						this.outputStream.close();
					break;
				case 0x9 : // Ping
					// TODO(RH)
					logger.finest("WebSocket ping frame received.");
					PingFrame frame = new PingFrame(message);
					write(new PongFrame(frame));
					break;
				case 0xA : // Pong
					logger.finest("Pongframe received.");
					break;
					default :
						// 0x3 - 0x7 reserved (non-control)
						// 0xB - 0xF reserved (control)
						logger.severe("Unknown opcode received.");
						close();
						break;
				}
				
				setChanged();
				notifyObservers(message);
			} catch (IOException e) {
				logger.info("IOException at WebSocket: " + e.getMessage());
				break;
			}
		}
		
		logger.info("WebSocket at "+ socket.getRemoteSocketAddress() +" closed.");
		
		try {
			close();
		} catch (IOException e) {
			logger.severe("IOException while closing WebSocketHandler: " + e.getMessage());
		}
	}
	
	/**
	 * Write frame
	 * @param frame
	 * @throws IOException
	 */
	
	public void write(Frame frame) throws IOException {
		write((frame).getBytes());
	}
	
	/**
	 * Read a fixed number of bytes
	 * 
	 * @param totalLength
	 * @return
	 * @throws IOException
	 */
	
	public byte[] readBytes(int totalLength) throws IOException {
		byte[] data = new byte[totalLength];
		int readBytes = 0;
		while((totalLength - readBytes) > 0) {
			readBytes += inputStream.read(data, readBytes, totalLength - readBytes);
		}
		return data;
	}

	private void write(String s) throws IOException {
		write(s.getBytes());
	}
	
	/**
	 * Sends a textframe
	 * @param text
	 * @throws IOException 
	 */
	
	public void send(String text) throws IOException {
		write(new TextFrame(text));
	}
		
	public void write(byte[] a) throws IOException {
		try {
			outputStream.write(a);
		} catch(IOException e) {
			close();
		}
	}
	
	public void close() throws IOException {
		outputStream.close();
		inputStream.close();
	}
}
