package rh.java.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpRequest {
	protected URL url;
	
	protected HttpURLConnection connection = null;
	
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
	}
	
	public byte[] getResponse() throws IOException {
		int responseLength = Integer.parseInt(connection.getHeaderField("Content-Length"));
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
