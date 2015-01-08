package rh.java.httpserver;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
	protected String method;
	
	protected String path;
	
	protected String version;
	
	protected Map<String,HttpHeader> headers = new HashMap<String,HttpHeader>();
	
	protected Integer contentLength = 0;

	private ClientHandler clientHandler;
	
	protected HttpRequest(ClientHandler clientHandler) {
		this.clientHandler = clientHandler;
	}
	
	protected void addHeader(HttpHeader header) {
		System.out.println("addHeader: " + header);
		headers.put(header.name, header);
	}
	
	public String toString() {
		return method + " " + path;
	}
	
	public boolean hasHeader(String name) {
		return headers.containsKey(name);
	}
	
	public HttpHeader getHeader(String name) {
		return headers.get(name);
	}
	
	public void close() throws IOException {
		// Status
		clientHandler.write("HTTP/1.1 200 OK\r\n");
		// Headers
		clientHandler.write("Content-Length: 5\r\n");
		clientHandler.write("Connection: keep-alive\r\n\r\n");
		// Content
		clientHandler.write("abcde");
	}
	
	public boolean upgrade() throws IOException {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			String key = getHeader("Sec-WebSocket-Key").content;
			System.out.println("key: '"+key+"'");
			byte[] hash = sha1.digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes());
			clientHandler.write("HTTP/1.1 101 Switching Protocols\r\n");
			clientHandler.write("Upgrade: websocket\r\n");
			clientHandler.write("Connection: Upgrade\r\n");
			clientHandler.write("Sec-WebSocket-Accept: "+ Base64.getEncoder().encodeToString(hash) + "\r\n\r\n");
			return true;
		} catch (NoSuchAlgorithmException e) {
			
		}
		return false;
	}
	
	public static String byteArrayToHexString(byte[] b) {
	  String result = "";
	  for (int i=0; i < b.length; i++) {
	    result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
	  }
	  return result;
	}
}
