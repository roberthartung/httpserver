package rh.java.http.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpRequest {
	private static Logger logger = Logger.getLogger(HttpRequest.class.getName());
	
	static {
		logger.setLevel(Level.ALL);
	}
	
	protected String method;
	
	protected String path;
	
	protected String query;
	
	protected String fragment;
	
	protected String version;
	
	protected Map<String,List<String>> requestHeaders = new LinkedHashMap<String,List<String>>();
	
	protected Map<String,List<String>> responseHeaders = new LinkedHashMap<String,List<String>>();
	
	protected Integer contentLength = 0;
	
	protected byte[] content = null;

	private ClientHandler clientHandler;
	
	protected HttpRequest(ClientHandler clientHandler) {
		this.clientHandler = clientHandler;
	}
	
	public String toString() {
		return method + " " + path;
	}
	
	public Object getContent() {
		if(requestHeaders.containsKey("Content-Type")) {
			switch(requestHeaders.get("Content-Type").get(0)) {
			case "application/x-www-form-urlencoded" :
				Map<String, String> data = new HashMap<String, String>();
				String s = new String(content);
				for(String part : s.split("&")) {
					int pos = part.indexOf("=");
					try {
						data.put(part.substring(0, pos), URLDecoder.decode(part.substring(pos+1), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
				return data;
			}
		}
		
		return content;
	}
	
	private byte[] response = null;
	
	private int responseStatus = 204;
	
	public void setResponse(byte[] response) {
		this.response = response;
		responseStatus = 200;
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
	
	public void redirect(String path) throws IOException {
		setResponseStatus(307);
		addResponseHeader("Location", path);
		close();
	}
	
	/**
	 * Close this request. This means sending a response
	 * 
	 * @throws IOException
	 */
	
	private void write(String s) throws IOException {
		clientHandler.write(s);
	}
	
	public void close() throws IOException {
		// Status
		write("HTTP/1.1 "+responseStatus+" ");
		switch(responseStatus) {
			case 200 :
				write("OK");
			break;
			case 204 :
				write("No Content");
				break;
			case 307 :
				write("Temporary Redirect");
				break;
		}
		write("\r\n");
		// write("Connection: keep-alive\r\n");
		for(Entry<String, List<String>> entry : responseHeaders.entrySet()) {
			for(String value : entry.getValue()) {
				write(entry.getKey() + ": "+value+"\r\n");
			}
		}
		// Headers
		if(response != null) {
			write("Content-Length: "+response.length+"\r\n");
		} else {
			write("Content-Length: 0\r\n");
		}
		write("\r\n");
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
	
	/**
	 * 
	 * 
	 * @return
	 */
	
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
	/*
	public void addResponseHeader(HttpHeader header) {
		responseHeaders.put(header.name, header);
	}
	*/

	/**
	 * Helper function to add a header
	 * 
	 * @param target
	 * @param name
	 * @param content
	 */
	
	private void addHeader(Map<String, List<String>> target, String name, String content) {
		List<String> values = target.get(name);
		if(values == null) {
			values = new ArrayList<String>();
			target.put(name, values);
		}
		
		values.add(content);
	}
	
	/**
	 * Helper function to set (override) a header
	 * 
	 * @param target
	 * @param name
	 * @param content
	 */
	
	private void setHeader(Map<String, List<String>> target, String name, String content) {
		List<String> values = new ArrayList<String>();
		values.add(content);
		target.put(name, values);
	}
	
	// ############# RequestHeaders
	
	/**
	 * adds a request headers (used by the HttpServer class to add headers)
	 * 
	 * @param name
	 * @param value
	 */
	
	protected void addRequestHeader(String name, String value) {
		addHeader(requestHeaders, name, value);
	}
	
	public boolean hasHeader(String name) {
		return requestHeaders.containsKey(name);
	}
	
	public List<String> getHeader(String name) {
		if(!requestHeaders.containsKey(name))
			return null;
		
		return requestHeaders.get(name);
	}
	
	public String getFirstHeader(String name) {
		List<String> values = getHeader(name);
		if(values == null)
			return null;
		
		return values.get(0);
	}
	
	public Map<String, Cookie> getCookies() {
		Map<String, Cookie> cookies = new HashMap<String, Cookie>();
		if(hasHeader("Cookie")) {
			for(String value : getHeader("Cookie")) {
				// TODO(rh): Better implementation
				for(String cookie : value.trim().split(";")) {
					String[] parts = cookie.split("=");
					String k = parts[0].trim();
					String v = parts[1].trim();
					cookies.put(k, new Cookie(k, v));
				}
			}
		}
		return cookies;
	}
	
	// ############# Cookies
	
	public void setCookie(String name, String value, String path) {
		setCookie(new Cookie(name, value, path));
	}
	
	public void setCookie(String name, String value) {
		setCookie(new Cookie(name, value));
	}
	
	public void setCookie(Cookie cookie) {
		addResponseHeader("Set-Cookie", cookie.toString());
	}
	
	// ############# Response Headers
	
	public void setResponseHeader(String name, String value) {
		setHeader(responseHeaders, name, value);
	}
	
	public void addResponseHeader(String name, String value) {
		addHeader(responseHeaders, name, value);
	}
}
