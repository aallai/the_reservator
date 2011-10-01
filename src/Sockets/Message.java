package Sockets;

import java.io.Serializable;
import java.util.ArrayList;


public class Message implements Serializable {
	private static final long serialVersionUID = 1337L;
	public Address to;
	public Address from;
	public Address client; // the client we need to get the request back to
	public int id;        // should be unique per client
	public String type;
	public ArrayList<Serializable> data;   // an arraylist of serializable objects
	
	public Message(Address to, Address from, Address client, int id, String type, ArrayList<Serializable> data) {
		this.to = to;
		this.from = from;
		this.client = client;
		this.id = id;
		this.type = type;
		this.data = (ArrayList<Serializable>) data.clone();
	}
}
