package rh.java.http.server;

public class HttpHeader {
	protected String name;
	
	protected String content;
	
	protected HttpHeader() {
		
	}
	
	protected HttpHeader(String name, String content) {
		this.name = name;
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return name + ": " + content;
	}
}
