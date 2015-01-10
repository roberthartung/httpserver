package rh.java.http.client;

import java.io.IOException;
import java.net.MalformedURLException;

public class HttpGetRequest extends HttpRequest {
	public HttpGetRequest(String path) throws MalformedURLException {
		super(path);
	}
	
	public void send() throws IOException {
		super.send("GET");
	}
}
