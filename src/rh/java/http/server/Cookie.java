package rh.java.http.server;

public class Cookie {
	
	protected String name;
	
	protected String value;
	
	protected String path = "/";

	public Cookie(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public Cookie(String name, String value, String path) {
		this.name = name;
		this.value = value;
		this.path = path;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
	public String toString() {
		return name+"="+value+"; Path="+path;
	}
}