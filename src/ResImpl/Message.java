package ResImpl;

import java.io.Serializable;
import java.util.ArrayList;

public class Message {
	int id;
	String type;
	ArrayList<Serializable> data;   // an arraylist of serializable objects
	
	public Message(int id, String type, ArrayList<Serializable> data) {
		this.id = id;
		this.type = type;
		this.data = (ArrayList<Serializable>) data.clone();
	}
}
