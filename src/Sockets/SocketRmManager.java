package Sockets;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.io.Serializable;

public class SocketRmManager extends BaseRm 
{
	private Address flight_rm;
	private Address car_rm;
	private Address room_rm;
	private HashMap<Integer, Serializable> results;
	/* increment ids by 2 everytime, client and server use even or odd numbers so we have 
	 * system wide unique ids.*/
	private int id = -1;
	
	public static void main(String[] args) 
	{
		// first argument is port to listen on, the rest is a list of active rm in host:port format

		String usage = "Usage : " + args[0] + " server_port rm_host1:port1 rm_host2:port2 ...";
		int port = 0;
		
		try {
			port = Integer.parseInt(args[0]); 
		} catch (NumberFormatException e) {
			System.err.println(usage);
			System.exit(-1);
		}
		
		ArrayList<Address> active_rms =  new ArrayList<Address>();
		for (int i = 1; i < args.length; i ++) {
			try {
				active_rms.add(new Address(args[i].split(":")[0], Integer.parseInt(args[i].split(":")[1])));
			} catch (Exception e) {
				System.err.println(usage);
				System.exit(-1);
			}
		}
		
		SocketRmManager manager = new SocketRmManager(port, active_rms);
		manager.run();
		
		System.out.println("Middleware server ready.");
	}
	
	public SocketRmManager(int port, ArrayList<Address> active_rms) 
	{
		super(port);
		
		Address[] rms = new Address[3];
		int i = 0;
		
		for (Address a : active_rms) {
			try {
				rms[i] = a;
				i++;
			} catch (IndexOutOfBoundsException e) { 
				System.err.println("SocketRmManager() : Ignored one or more rms passed in to constructor.");
				break;
			}
		}
		
		if (i < rms.length) {
			System.err.println("SocketRmManager() : Fewer than three active rms prvided.");
			for (; i < rms.length; i++) {
				rms[i] = active_rms.get(0);
			}
		}
		
		this.results = new HashMap<Integer, Serializable>();
		this.flight_rm = rms[0];
		this.car_rm = rms[1];
		this.room_rm = rms[2];
	}
	
	/**
	 * Called when the server receives a result from one of the RMs.
	 * 
	 * @param id
	 * @param obj
	 */
	public void result(int id, Serializable obj)
	{
		synchronized(this.results) {
			this.results.put(id, obj);
			this.results.notifyAll();
		}
	}
	
	private Serializable get_result(int id)
	{
		// TODO: add timeouts
		
		synchronized(this.results) {
			
			while (!this.results.containsKey(id)) {
				try {
					this.results.wait();
					
				} catch (InterruptedException e) {
					continue;
				}
			}
			Serializable ret =  this.results.get(id);
			this.results.remove(id);
			return ret;
		}
	}
	
	/**
	 * More of a debug feature, a failure, let's say,
	 * booking a flight would be signaled by returning a false boolean value.
	 */
	public void error(Address from, int id, String info)
	{
		System.err.println("SocketRmManager.error() : error recived from " + from.toString() + 
								" for request " + id );
		System.err.println("---");
		System.err.println(info);
		System.err.println("---");
	}
	
	public void received(Message m) 
	{
		
		// TODO : add special case of itinerary

		if (valid_op(m.type)) {
		
			if (m.type.equals("error"))
			{
				error( m.from, m.id, (String) m.data.get(0));
				
			} else if (m.type.equals("result")) {
				result(m.id, m.data.get(0));
		
			} else if (m.type.equals("newCustomer")) {
				newCustomer(m.from, m.id, m.data.toArray());
			} else if (m.type.equals("deleteCustomer")) {
				deleteCustomer(m.from, m.id, m.data.toArray());	
				
			} else {
				
				Address to = resolve_rm(m.type);
			
				if (to == null) {
					System.err.println("SocketRmManager.received() : Could not find rm for request " + m.type);
					return;
				}
			
				Message f = new Message(to, this.self, m.id, m.type, m.data);
				com.send(f);
			
				// blocks here
				Serializable result = get_result(m.id);
			
				f.data.clear();
				f.data.add(result);
				f = new Message(m.from, this.self, m.id, "result", f.data);
				com.send(f);
			}
			
		} else {
			send_error(m.from, m.id, "Requested unsupported operation: " + m.type);
		}
	}
	
	private boolean valid_op(String type)
	{
		for (Method m : super.getClass().getMethods()) {
			if (m.getName().equalsIgnoreCase(type)) {
				return true;
			}
		}
		
		for (Method m : this.getClass().getMethods()) {
			if (m.getName().equalsIgnoreCase(type)) {
				return true;
			}
		}
		
		return false;
	}
	
	private Address resolve_rm(String type)
	{
		Address ret = null;
		if (type.toLowerCase().contains("car")) {
			ret = this.car_rm;
		} else if (type.toLowerCase().contains("flight")) {
			ret = this.flight_rm;
		} else if (type.toLowerCase().contains("room")) {
			ret = this.room_rm;
		}
		
		return ret;
	}
	
	public void newCustomer(Address from, int id, Object[] args)
	{
		boolean sendbool = true;
		
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		for (Object s : args) {
			data.add((Serializable) s);
		}
		
		/* don't let the servers auto-choose the cid because they dont do it so well
		 the resource manager doesn't notify of failure for this, so
		 I optimize and send the response back right away.*/
		if (data.size() == 1) {
			data.add(id);
			ArrayList<Serializable> result = new ArrayList<Serializable>();
			result.add(id);
			com.send(new Message(from, self, id, "result", result));

			// just trying to emulate the original interface
			sendbool = false;
		}
		
		int[] ids = new int[] {get_id(), get_id(), get_id()};
		int i = 0;
		
		for (Address to : new Address[] {this.flight_rm, this.room_rm, this.car_rm}) {
			Message m = new Message(to, this.self, ids[i], "newCustomer", data);
			com.send(m);
			i++;
		}
		
		if (sendbool) {
			
			boolean ret = true;
			for (int uid : ids) {
				Serializable result = get_result(uid);
				
				if (!((Boolean) result).booleanValue()) {
					ret = false;
				}
			}
			ArrayList<Serializable> r = new ArrayList<Serializable>();
			r.add(ret);
			Message m = new Message(from, self, id, "result", r);
			com.send(m);
		}
	}
		
	public void deleteCustomer(Address from, int id, Object[] args) 
	{
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		for (Object s : args) {
			data.add((Serializable) s);
		}
		
		int[] ids = new int[] {get_id(), get_id(), get_id()};
		int i = 0;
		
		for (Address to : new Address[] {this.flight_rm, this.room_rm, this.car_rm}) {
			Message m = new Message(to, this.self, ids[i], "deleteCustomer", data);
			com.send(m);
			i++;
		}
		
		boolean ret = true;
		for (int uid : ids) {
			Serializable result = get_result(uid);
			
			if (!((Boolean) result).booleanValue()) {
				ret = false;
			}
		}
		data.clear();
		data.add(ret);
		Message m = new Message(from, self, id, "result", data);
		com.send(m);
	}
	
	private int get_id()
	{
		this.id += 2; 
		return id;
	}
}
