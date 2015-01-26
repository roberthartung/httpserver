package rh.java.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
	
	protected URL url;
	
	protected HttpURLConnection connection = null;
	
	protected Map<String,List<String>> requestHeaders = new LinkedHashMap<String,List<String>>();
	
	public HttpRequest(String path) throws MalformedURLException {
		url = new URL(path);
	}
	
	protected void send(String method) throws IOException {
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(method);
		// connection.setRequestProperty("Content-Language", "en-US");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		for(Entry<String, List<String>> entry : requestHeaders.entrySet()) {
			for(String value : entry.getValue()) {
				connection.setRequestProperty(entry.getKey(), value);
				// os.write((entry.getKey() + ": "+value+"\r\n").getBytes());
			}
		}
	}
	
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
	
	public void addRequestHeader(String name, String value) {
		addHeader(requestHeaders, name, value);
	}
	
	public Object getContent() throws IOException {
		return connection.getContent();
	}
	
	public byte[] getResponse() throws IOException {
		int responseLength = connection.getContentLength();
		InputStream is = connection.getInputStream();
		byte[] responseAsBytes = new byte[responseLength];
		int offset = 0;
		while (responseLength - offset > 0) {
			offset += is.read(responseAsBytes, offset, responseLength - offset);
		}
		is.close();
		return responseAsBytes;
	}
	
	public void close() {
		if(connection != null) {
        connection.disconnect(); 
      }
	}
}
