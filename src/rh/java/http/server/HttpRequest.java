package rh.java.http.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequest {
	protected String method;
	
	protected String path;
	
	protected String query;
	
	protected String fragment;
	
	protected String version;
	
	protected Map<String,HttpHeader> headers = new LinkedHashMap<String,HttpHeader>();
	
	protected Map<String,HttpHeader> responseHeaders = new LinkedHashMap<String,HttpHeader>();
	
	protected Integer contentLength = 0;

	private ClientHandler clientHandler;
	
	protected HttpRequest(ClientHandler clientHandler) {
		this.clientHandler = clientHandler;
	}
	
	protected void addHeader(HttpHeader header) {
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
	
	private byte[] response = null;
	
	private int responseStatus = 200;
	
	public void setResponse(byte[] response) {
		this.response = response;
	}
	
	public void setResponse(File file) {
		try {
			this.response = Files.readAllBytes(file.toPath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setResponseStatus(int status) {
		responseStatus = status;
	}
	
	/**
	 * Close this request. This means sending a response
	 * 
	 * @throws IOException
	 */
	
	public void close() throws IOException {
		// Status
		clientHandler.write("HTTP/1.1 "+responseStatus+" OK\r\n");
		// Headers
		if(response != null) {
			clientHandler.write("Content-Length: "+response.length+"\r\n");
		}
		clientHandler.write("Connection: keep-alive\r\n");
		for(HttpHeader header : responseHeaders.values()) {
			clientHandler.write(header.toString() + "\r\n");
		}
		clientHandler.write("\r\n");
		// Content
		if(response != null) {
			clientHandler.write(response);
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public String getMethod() {
		return method;
	}
	
	public String getQuery() {
		return query;
	}
	
	public String getFragment() {
		return fragment;
	}
	
	public Map<String,String> getQueryArguments() {
		if(query == null || query.equals("")) {
			return null;
		}
		
		Map<String, String> arguments = new LinkedHashMap<String, String>();
		
		for(String part : query.split("&")) {
			int pos = part.indexOf("=");
			try {
				arguments.put(part.substring(0,pos), java.net.URLDecoder.decode(part.substring(pos+1), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return arguments;
	}
	
	/*
	public static String byteArrayToHexString(byte[] b) {
	  String result = "";
	  for (int i=0; i < b.length; i++) {
	    result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
	  }
	  return result;
	}
	*/
	public void addResponseHeader(HttpHeader header) {
		responseHeaders.put(header.name, header);
	}
	
	public void addResponseHeader(String name, String content) {
		addResponseHeader(new HttpHeader(name, content));
	}
}
