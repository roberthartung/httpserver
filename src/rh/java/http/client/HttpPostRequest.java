package rh.java.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Map.Entry;

public class HttpPostRequest extends HttpRequest {
	public HttpPostRequest(String path) throws MalformedURLException {
		super(path);
	}

	public void send(byte[] data) throws IOException {
		super.send("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Content-Length", "" + data.length);
		// Write actual data
		connection.getOutputStream().write(data);
	}

	public void send(Map<String, String> map) throws IOException {
		StringBuffer data = new StringBuffer();
		for (Entry<String, String> entry : map.entrySet()) {
			if (data.length() > 0) {
				data.append("&");
			}

			try {
				data.append(entry.getKey() + "="
						+ java.net.URLDecoder.decode(entry.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		send(data.toString().getBytes());
	}
}