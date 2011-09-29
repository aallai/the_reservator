package Sockets;

import ResInterface.Callback;
import java.util.ArrayList;
import java.io.Serializable;
import java.lang.Thread;

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
		
		try {
		Address rm = new Address("functor.local", 22100);
		Address self = new Address("functor.local", 22200);
		int id = 0;
		
		// addFlight
		Message m = new Message(rm, self, id++, "addFlight", data);
		com.send(m);
		Thread.sleep(100);
		
		//deleteFlight
		data.clear();
		data.add(id);
		data.add(1);
		m = new Message(rm, self, id++, "deleteFlight", data);
		com.send(m);
		Thread.sleep(100);
		
		//addRooms
		data.clear();
		data.add(id);
		data.add("HILTON");
		data.add(1);
		data.add(50);
		m = new Message(rm, self, id++, "addRooms", data);
		com.send(m);
		Thread.sleep(100);
		
		// delete rooms
		data.clear();
		data.add(id);
		data.add("HILTON");
		m = new Message(rm, self, id++, "deleteRooms", data);
		com.send(m);
		Thread.sleep(100);
		
		// addCars
		data.clear();
		data.add(id);
		data.add("HILTON");
		data.add(1);
		data.add(50);
		m = new Message(rm, self, id++, "addCars", data);
		com.send(m);
		Thread.sleep(100);
		
		//deleteCars
		data.clear();
		data.add(id);
		data.add("HILTON");
		m = new Message(rm, self, id++, "deleteCars", data);
		com.send(m);
		Thread.sleep(100);
		
		// queryFlight
		data.clear();
		data.add(id);
		data.add(1);
		data.add(20);
		data.add(50);
		m = new Message(rm, self, id++, "addFlight", data);
		com.send(m);
	    Thread.sleep(100);
	    
	    data.clear();
	    data.add(id);
	    data.add(1);
	    m = new Message(rm, self, id++, "queryFlight", data);
	    com.send(m);
	    Thread.sleep(100);
	    
	    // queryFlightPrice
	    m = new Message(rm, self, id++, "queryFlightPrice", data);
	    com.send(m);
	    Thread.sleep(100);
	    
	    // queryRooms
	    data.clear();
		data.add(id);
		data.add("HILTON");
		data.add(1);
		data.add(50);
		m = new Message(rm, self, id++, "addRooms", data);
		com.send(m);
		Thread.sleep(100);
		
		data.clear();
		data.add(id);
		data.add("HILTON");
		m = new Message(rm, self, id++, "queryRooms", data);
		com.send(m);
		Thread.sleep(100);
		
		// queryCars
		data.clear();
		data.add(id);
		data.add("HILTON");
		data.add(1);
		data.add(50);
		m = new Message(rm, self, id++, "addCars", data);
		com.send(m);
		Thread.sleep(100);
		
		data.clear();
		data.add(id);
		data.add("HILTON");
		m = new Message(rm, self, id++, "queryCars", data);
		com.send(m);
		Thread.sleep(100);
		
		// queryCarsPrice
		m = new Message(rm, self, id++, "queryCarsPrice", data);
		com.send(m);
		Thread.sleep(100);
		
		// newCustomer
		data.clear();
		data.add(id);
		m = new Message(rm, self, id++, "newCustomer", data);
		com.send(m);
		Thread.sleep(100);
		
		data.clear();
		data.add(id);
		data.add(1337);
		m = new Message(rm, self, id++, "newCustomer", data);
		com.send(m);
		Thread.sleep(100);
		
		// deleteCustomer
		m = new Message(rm, self, id++, "deleteCustomer", data);
		com.send(m);
		Thread.sleep(100);
		
		
	    
		} catch (InterruptedException e) {}
	}
	
	public void received(Message m) 
	{
		synchronized(System.out) {
			System.out.println(m.id + " -> " + m.data.get(0));
			System.out.println();
		}
	}
}
