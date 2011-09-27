package ResImpl;

import java.io.Serializable;

public class Address implements Serializable {
	
	private static final long serialVersionUID = 1337L;
	public String host;
	public int port;
	
	public Address(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public String toString() {
		return "( " + host + ", " + String.valueOf(port) + " )";  
	}
}
