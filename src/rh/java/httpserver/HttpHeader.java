package rh.java.httpserver;

public class HttpHeader {
	protected String name;
	
	protected String content;
	
	public String toString() {
		return name + ": " + content;
	}
}
