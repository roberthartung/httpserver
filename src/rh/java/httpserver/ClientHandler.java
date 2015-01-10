package rh.java.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Observable;

import rh.java.httpserver.websocket.Frame;
import rh.java.httpserver.websocket.PingFrame;
import rh.java.httpserver.websocket.PongFrame;

class ClientHandler extends Observable implements Runnable {
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private ClientState state = ClientState.METHOD;
	private HttpRequest httpRequest;
	private HttpHeader httpHeader = new HttpHeader();
	private boolean upgraded = false;
	private String buffer = "";
	
	ClientHandler(Socket socket) throws IOException {
		this.socket = socket;
		this.inputStream = socket.getInputStream();
		this.outputStream = socket.getOutputStream();
	}
	
	public boolean isUpgraded() {
		return upgraded;
	}
	
	public void run() {
		while(!upgraded && socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown()) {
			try {
				int b = this.inputStream.read();
				
				if(b == -1) {
					socket.close();
					System.err.println("Client closed connection.");
					return;
				}
				
				char c = (char) b;
				// '" + c + "'
				// System.out.println(state + " ("+b+") -> '" + buffer + "'");
				
				switch(state) {
				case METHOD :
					if(c == ' ') {
						httpRequest = new HttpRequest(this);
						httpRequest.method = buffer;
						buffer = "";
						state = ClientState.PATH;
					} else {
						buffer += c;
					}
					break;
				case PATH :
					if(c == ' ') {
						httpRequest.path = buffer;
						buffer = "";
						state = ClientState.VERSION;
					} else {
						buffer += c;
					}
					break;
				case VERSION :
					if(c == '\n') {
						httpRequest.version = buffer;
						buffer = "";
						state = ClientState.HEADER_NAME;
					} else if(c != '\r') {
						buffer += c;
					}
					break;
				case HEADER_NAME :
					if(c == '\n') {
						if(buffer.equals("\r")) {
							// TODO(rh): Check if we received the content length
							if(!httpRequest.hasHeader("Content-Length")) {
								completeRequest();
								state = ClientState.METHOD;
							} else {
								state = ClientState.BODY;
								httpRequest.contentLength = Integer.parseInt(httpRequest.getHeader("Content-Length").content);
							}
						}
						buffer = "";
					} else if(c == ':') {
						state = ClientState.HEADER_CONTENT;
						httpHeader.name = buffer;
						buffer = "";
					} else {
						buffer += c;
					}
					break;
				case HEADER_CONTENT :
					if(c == '\n') {
						state = ClientState.HEADER_NAME;
						httpHeader.content = buffer;
						httpRequest.addHeader(httpHeader);
						httpHeader = new HttpHeader();
						buffer = "";
					} else if(c != '\r') {
						if(c != ' ' || buffer.length() > 0) {
							buffer += c;
						}
					}
					break;
				case BODY :
					completeRequest();
					state = ClientState.METHOD;
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		
		if(upgraded) {
			//WebSocketState webSocketState = WebSocketState.FRAME_HEADER_FLAGS_AND_OPCODE;
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
					
					/*
					int b = this.inputStream.read();
					if(b == -1) {
						socket.close();
						System.err.println("Client closed connection.");
						return;
					}
					*/
					// System.out.println(webSocketState + " " + b);
					/*
					switch(webSocketState) {
					case FRAME_HEADER_FLAGS_AND_OPCODE :
							webSocketState = WebSocketState.FRAME_START_MASK_AND_PAYLOAD_LENGTH;
						break;
					case FRAME_START_MASK_AND_PAYLOAD_LENGTH :
						if((b & 0x80) == 0x80) {
							isMasked = true;
						}
						
						payloadLength = b & ~0xF0;
						
						switch(payloadLength) {
						case 126 :
							webSocketState = WebSocketState.FRAME_EXTENDED_PAYLOAD_16;
							break;
						case 127 :
							webSocketState = WebSocketState.FRAME_EXTENDED_PAYLOAD_64;
							break;
						default :
								if(isMasked) {
									webSocketState = WebSocketState.FRAME_MASKING_KEY;
								} else {
									webSocketState = WebSocketState.DATA;
								}
								break;
						}
						break;
					case FRAME_EXTENDED_PAYLOAD_16 :
						byte[] data1 = readRemainingBytes(b, 2);
						payloadLength = (data1[0] << 8) | (data1[1] << 0);
						if(isMasked) {
							webSocketState = WebSocketState.FRAME_MASKING_KEY;
						} else {
							webSocketState = WebSocketState.DATA;
						}
						break;
					case FRAME_EXTENDED_PAYLOAD_64 :
						// byte[] data2 = readRemainingBytes(b, 8);
						payloadLength = (data2[0] << 8) | (data2[1] << 0);
						if(isMasked) {
							webSocketState = WebSocketState.FRAME_MASKING_KEY;
						} else {
							webSocketState = WebSocketState.DATA;
						}
						break;
					case FRAME_MASKING_KEY :
						// mask = readRemainingBytes(b, 4);
						webSocketState = WebSocketState.DATA;
						break;
					case DATA :
						byte[] message = readRemainingBytes(b, payloadLength);
						// System.out.println(b + " " + message[0]);
						for(int o=0; o<message.length; o++) {
							int maskOffset = o % 4;
							message[o] = (byte) (message[o] & mask[maskOffset]);
						}
						System.out.println(new String(message));
						break;
					}
					*/
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	/*
	private byte[] readRemainingBytes(int firstByte, int totalLength) throws IOException {
		byte[] data = new byte[totalLength];
		data[0] = (byte) firstByte;
		int readBytes = 1;
		while((totalLength - readBytes) > 0) {
			readBytes += inputStream.read(data, readBytes, totalLength - readBytes);
		}
		return data;
	}
	*/
	public byte[] readBytes(int totalLength) throws IOException {
		byte[] data = new byte[totalLength];
		int readBytes = 0;
		while((totalLength - readBytes) > 0) {
			readBytes += inputStream.read(data, readBytes, totalLength - readBytes);
		}
		return data;
	}
		
	private void completeRequest() {
		buffer = "";
		setChanged();
		
		if(httpRequest.hasHeader("Upgrade")) {
			HttpHeader upgradeHeader = httpRequest.getHeader("Upgrade");
			switch(upgradeHeader.content) {
			case "websocket" :
					switch(httpRequest.getHeader("Sec-WebSocket-Version").content) {
					case "13" :
						try {
							httpRequest.upgrade();
							System.out.println("Upgrade to websocket!");
							upgraded = true;
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					}
				break;
			}
		}
		
		notifyObservers(httpRequest);
		httpRequest = null;
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