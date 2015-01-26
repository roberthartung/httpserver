package rh.java.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientHandler extends Observable implements Runnable {
	private static Logger logger = Logger.getLogger(ClientHandler.class.getName());
	
	static {
		logger.setLevel(Level.ALL);
	}
	
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private ClientState state = ClientState.METHOD;
	private HttpRequest httpRequest;
	// private HttpHeader httpHeader = new HttpHeader();
	private String buffer = "";
	
	private String headerName = null;
	
	ClientHandler(Socket socket) throws IOException {
		logger.finest("New Client from " + socket.getRemoteSocketAddress());
		this.socket = socket;
		this.inputStream = socket.getInputStream();
		this.outputStream = socket.getOutputStream();
	}
	
	public void run() {
		while(socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown()) {
			try {
				int b = this.inputStream.read();
				
				if(b == -1) {
					logger.info("Stream finished");
					break;
				}
				
				char c = (char) b;
				
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
							if(!httpRequest.hasHeader("Content-Length")) {
								completeRequest();
							} else {
								// state = ClientState.BODY;
								httpRequest.contentLength = Integer.parseInt(httpRequest.getFirstHeader("Content-Length"));
								byte[] content = readBytes(httpRequest.contentLength);
								httpRequest.content = content;
								completeRequest();
							}
						}
						buffer = "";
					} else if(c == ':') {
						state = ClientState.HEADER_CONTENT;
						// httpHeader.name = buffer;
						headerName = buffer;
						buffer = "";
					} else {
						buffer += c;
					}
					break;
				case HEADER_CONTENT :
					if(c == '\n') {
						state = ClientState.HEADER_NAME;
						// httpHeader.content = buffer;
						// httpRequest.addRequestHeader(httpHeader);
						httpRequest.addRequestHeader(headerName, buffer);
						// httpHeader = new HttpHeader();
						buffer = "";
					} else if(c != '\r') {
						if(c != ' ' || buffer.length() > 0) {
							buffer += c;
						}
					}
					break;
					/*
				case BODY :
					byte[] content = readBytes(httpRequest.contentLength-1);
					httpRequest.content = content;
					completeRequest();
					state = ClientState.METHOD; // Reset
					break;
					*/
				}
			} catch (IOException e) {
				break;
			}
		}
		logger.finest("Connection closed. ("+socket.getRemoteSocketAddress() + ")");
		try {
			close();
		} catch (IOException e) {
			logger.severe("Error while closing ClientHandler.");
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	
	public byte[] readBytes(int totalLength) throws IOException {
		try {
			byte[] data = new byte[totalLength];
			int readBytes = 0;
			while((totalLength - readBytes) > 0) {
				readBytes += inputStream.read(data, readBytes, totalLength - readBytes);
			}
			return data;
		} catch(IOException ex) {
			logger.severe("IOException " + ex.getMessage());
			close();
			return null;
		}
	}
	
	/**
	 * Request to upgrade the default client hander to another class
	 * 
	 * @param upgradeHandlerClass	Class Information of the handler
	 * @param request				The original HTTP request
	 * 
	 * TODO(rh) This should be done directly in the HttpServer
	 * 
	 * @return UpgradeHandler
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
		state = ClientState.METHOD;
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
		inputStream.close();
	}
}