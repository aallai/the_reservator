package Sockets;

import ResInterface.Callback;
import java.util.ArrayList;
import java.io.Serializable;

public class MessageTest implements Callback 
{

	public static void main(String[] args) {
		MessageTest t = new MessageTest();
		
		Communicator com = new Communicator(22200, t);
		com.init();
		
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add(0);
		data.add(1);
		data.add(20);
		data.add(50);
		
		Address rm = new Address("functor.local", 22100);
		Address self = new Address("functor.local", 22200);
		int id = 0;
		
		// addFlight
		Message m = new Message(rm, self, id++, "addFlight", data);
		com.send(m);
		
		//deleteFlight
		data.clear();
		data.add(1);
		m = new Message(rm, self, id++, "deleteFlight", data);
		
		
		
	}
	
	public void received(Message m) 
	{
		System.out.println(m.type);
		for (Serializable s : m.data) {
			System.out.println(s);
		}
		System.out.println();
	}
}
