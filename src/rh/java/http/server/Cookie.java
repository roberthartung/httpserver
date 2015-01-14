package rh.java.http.server;

public class Cookie {
	
	protected String name;
	
	protected String value;

	public Cookie(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
	public String toString() {
		// TODO(rh): 
		return name+"="+value;
	}
}