package ResImpl;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
	private static final long serialVersionUID = 1337L;
	public Address to;
	public Address from;
	public int id;
	public String type;
	public ArrayList<Serializable> data;   // an arraylist of serializable objects
	
	public Message(Address to, Address from, int id, String type, ArrayList<Serializable> data) {
		this.to = to;
		this.from = from;
		this.id = id;
		this.type = type;
		this.data = (ArrayList<Serializable>) data.clone();
	}
}
