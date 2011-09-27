package ResImpl;

import ResInterface.Callback;
import java.util.ArrayList;
import java.io.Serializable;

public class MessageTest implements Callback {
	public static void main(String[] args) {
		
		MessageTest t = new MessageTest();
		MessageTest s = new MessageTest();
		
		Communicator a = new Communicator(22000, t);
		a.init();
		Communicator b = new Communicator(22100, s);
		b.init();
		
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add("UNBELIEVABLE");
		
		Message m1 = new Message(new Address("localhost", 22000), new Address("localhost", 22100), 0, "FROM B", data);
		Message m2 = new Message(new Address("localhost", 22100), new Address("localhost", 22000), 0, "FROM A", data);
		
		b.send(m1);
		a.send(m2);
	}
	
	public void received(Message m) {
		System.out.println(m.type + " -> " + m.data.get(0));
	}
}
