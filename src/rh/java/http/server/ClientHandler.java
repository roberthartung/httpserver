package rh.java.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

class ClientHandler extends Observable implements Runnable {
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private ClientState state = ClientState.METHOD;
	private HttpRequest httpRequest;
	private HttpHeader httpHeader = new HttpHeader();
	private String buffer = "";
	
	ClientHandler(Socket socket) throws IOException {
		this.socket = socket;
		this.inputStream = socket.getInputStream();
		this.outputStream = socket.getOutputStream();
	}
	
	/*
	public boolean isUpgraded() {
		return upgradeHandler != null;
	}
	*/
	
	public void run() {
		// (upgradeHandler == null) && 
		while(socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown()) {
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
						state = ClientState.URL_PATH;
					} else {
						buffer += c;
					}
					break;
				case URL_PATH :
					if(c == '?') {
						httpRequest.path = buffer;
						buffer = "";
						state = ClientState.URL_QUERY;
					}
					else if(c == '#') {
						httpRequest.path = buffer;
						buffer = "";
						state = ClientState.URL_FRAGMENT;
					}
					else if(c == ' ') {
						httpRequest.path = buffer;
						buffer = "";
						state = ClientState.VERSION;
					} else {
						buffer += c;
					}
					break;
				case URL_QUERY :
					if(c == '#') {
						httpRequest.query = buffer;
						buffer = "";
						state = ClientState.URL_FRAGMENT;
					}
					else if(c == ' ') {
						httpRequest.query = buffer;
						buffer = "";
						state = ClientState.VERSION;
					} else {
						buffer += c;
					}
					break;
				case URL_FRAGMENT :
					if(c == ' ') {
						httpRequest.fragment = buffer;
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
		
		/*
		if(upgradeHandler != null) {
			try {
				upgradeHandler.receive(socket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
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
	
	public UpgradeHandler upgrade(Class<? extends UpgradeHandler> upgradeHandlerClass, HttpRequest request) {
		UpgradeHandler handler = null;
		try {
			Constructor<? extends UpgradeHandler> c = upgradeHandlerClass.getConstructor(Socket.class, HttpRequest.class);
			handler = c.newInstance(socket, request);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return handler;
	}
	
	private void completeRequest() {
		buffer = "";
		setChanged();
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