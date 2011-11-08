package Sockets;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.io.Serializable;

public class SocketRmManager extends BaseRm 
{
	private Address flight_rm;
	private Address car_rm;
	private Address room_rm;
	private ArrayList<Result> results;
	private int id = 0;
	private 
	final int RETRY_MAX = 5;
	
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
		
		this.results = new ArrayList<Result>();
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
	public void result(int id, Address client, Serializable obj)
	{
		synchronized(this.results) {
			this.results.add(new Result(client, id, obj));
			this.results.notifyAll();
		}
	}
	
	private Serializable get_result(Address client, int id)
	{
		// TODO: add timeouts
		
		synchronized(this.results) {
			
			int retries = 0;
			Result r;
			
			while ((r = _get_result_(client, id)) == null && retries <= this.RETRY_MAX) {
				try {
					this.results.wait(1000L);
					
				} catch (InterruptedException e) {
				}
				retries++;
			}
			
			if (r != null) {
				Serializable ret =  r.value;
				return ret;
			} else {
				return null;
			}
		}
	}
	
	private Result _get_result_(Address client, int id)
	{
		Result ret = null;
		for (Result r : this.results) {
			if (r.matches(client, id)) {
				ret = r;
			}
		}
		this.results.remove(ret);
		return ret;
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
				if (m.client.equals(this.self)) {
					error( m.from, m.id, (String) m.data.get(0));
				} else {
					send_error(m.client, m.client, m.id, (String) m.data.get(0));
				}
			} else if (m.type.equals("result")) {
				result(m.id, m.client, m.data.get(0));
			} else if (m.type.equals("newCustomer")) {
				newCustomer(m.from, m.id, m.data.toArray());
			} else if (m.type.equals("deleteCustomer")) {
				deleteCustomer(m.from, m.id, m.data.toArray());	
			} else if (m.type.equals("queryCustomerInfo"))	{
				queryCustomerInfo(m.from, m.id, m.data.toArray());
			} else if (m.type.equals("itinerary")) {
				try {
					itinerary(m.from, m.id, (Integer) m.data.get(1), 
							(Vector<Integer>) m.data.get(2), (String) m.data.get(3), 
								(Boolean) m.data.get(4), (Boolean) m.data.get(5)); 
				} catch (ClassCastException e) {
					e.printStackTrace();
					send_error(m.from, m.client, m.id, "Wrong parameters for operation: " + m.type);
				}
			} else {
				
				Address to = resolve_rm(m.type);
			
				if (to == null) {
					System.err.println("SocketRmManager.received() : Could not find rm for request " + m.type);
					return;
				}
			
				Message f = new Message(to, this.self, m.client, m.id, m.type, m.data);
				com.send(f);
			
				// blocks here
				Serializable result = get_result(m.client, m.id);
				
				// assume error is coming back
				if (result == null) {
					return;
				}
			
				f.data.clear();
				f.data.add(result);
				f = new Message(m.from, this.self, m.client, m.id, "result", f.data);
				com.send(f);
			}
			
		} else {
			send_error(m.from, m.client, m.id, "Requested unsupported operation: " + m.type);
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

		/* don't let the servers auto-choose the cid because they dont do it so well
		 the resource manager doesn't notify of failure for this, so
		 I optimize and send the response back right away.*/

		ArrayList<Serializable> data = new ArrayList<Serializable>();
		for (Object o : args) {
			data.add((Serializable) o);
		}
		
		if (data.size() == 1) {
			data.add(id);
			
			ArrayList<Serializable> result = new ArrayList<Serializable>();
			result.add(id);
			com.send(new Message(from, self, from, id, "result", result));

			// just trying to emulate the original interface
			sendbool = false;
		}
		
		Serializable[] results = send_all_rms(data.toArray(), "newCustomer");
		
		// error occurred should propagate back
		if (results == null) {
			return;
		}
		
		if (sendbool) {
			
			boolean ret = true;
			
			for (Serializable result : results) {
				if (!((Boolean) result).booleanValue()) {
					ret = false;
				}
			}
			
			ArrayList<Serializable> r = new ArrayList<Serializable>();
			r.add(ret);
			Message m = new Message(from, self, from, id, "result", r);
			com.send(m);
		}
	}
		
	public void deleteCustomer(Address from, int id, Object[] args) 
	{
	
		Serializable[] results = send_all_rms(args, "deleteCustomer");
		
		if (results == null) {
			return;
		}
		
		boolean ret = true;
		
		for (Serializable res : results) {
			if (!((Boolean) res).booleanValue()) {
				ret = false;
			}
		}
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add(ret);
		Message m = new Message(from, self, from, id, "result", data);
		com.send(m);
	}
	
	
	public void queryCustomerInfo(Address from, int id, Object[] args)
	{
		Serializable[] results = send_all_rms(args, "queryCustomerInfo");
		
		// assume error gets propagated back
		if (results == null) {
			return;
		}
		
		String ret = "Bill for customer " + args[1].toString();
		for (Serializable str : results) {
			
			// call may have failed
			if (((String) str).equals("")) {
				
			}
			
			String[] tmp = ((String) str).split("\n");
			for (int i = 1; i < tmp.length; i++) {
				ret += "\n" + tmp[i];
			}
		}
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add(ret);
		Message m = new Message(from, this.self, from, id, "result", data);
		com.send(m);
	}
	
	
	public void itinerary(Address from, int id,int customer,Vector<Integer> flightNumbers,String location, boolean Car, boolean Room)
    {
		
		int cur_id = id;
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		Message m;
		Serializable result;
		
		// check to see if all flights exist
		for (int i : flightNumbers) {
			data.clear();
			data.add(id);
			data.add(i);
			
			m = new Message(this.flight_rm, this.self, from, cur_id, "queryFlight", data);
			com.send(m);
			
			result = get_result(from, cur_id);
			
			if ( ((Integer) result) == 0) {
    			data.clear();
    			data.add(false);
    			m = new Message(from, this.self, from, id, "result", data);
    			com.send(m);
    			return;
    		}
			
			cur_id++;
		}
		
    	for (int i : flightNumbers) {
    		data.clear();
    		data.add(id);
    		data.add(customer);
    		data.add(i);
    		m = new Message(this.flight_rm, this.self, from, cur_id, "reserveFlight", data);
    		com.send(m);
    		
    		result = get_result(from, cur_id);

    		// error occurred should propagate back
    		if (result == null) {
    			return;
    		}
    		
    		if (!((Boolean) result).booleanValue()) {
    			data.clear();
    			data.add(false);
    			m = new Message(from, this.self, from, id, "result", data);
    			com.send(m);
    			return;
    		}
    		cur_id++;
    	}
    		
    	if (Car) {
    		data.clear();
    		data.add(id);
    		data.add(customer);
    		data.add(location);
    			
    		m = new Message(car_rm, this.self, from, cur_id, "reserveCar", data);
    		com.send(m);
    			
    		result = get_result(from, cur_id);

    		// error occurred should propagate back
    		if (result == null) {
    			return;
    		}
    			
    		if (!((Boolean) result).booleanValue()) {
        		data.clear();
        		data.add(false);
        		m = new Message(from, this.self, from, id, "result", data);
        		com.send(m);
        		return;
        	}
    		cur_id++;
    	}
    		
    	if (Room) {
    		data.clear();
    		data.add(id);
    		data.add(customer);
    		data.add(location);
    		
    		m = new Message(room_rm, this.self, from, cur_id, "reserveRoom", data);
    		com.send(m);
    		
    		result = get_result(from, cur_id);
    		
    		// error occurred should propagate back
    		if (result == null) {
    			return;
    		}
    		
    		if (!((Boolean) result).booleanValue()) {
       			data.clear();
       			data.add(false);
       			m = new Message(from, this.self, from, id, "result", data);
       			com.send(m);
       			return;
       		}	
   		}
    
    		
    	data.clear();
    	data.add(true);
    	m = new Message(from, this.self, from, id, "result", data);
    	com.send(m);
    }
    
	
	
	private Serializable[] send_all_rms(Object[] args, String type) 
	{
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		for (Object s : args) {
			data.add((Serializable) s);
		}
		
		int[] ids = new int[] {get_id(), get_id(), get_id()};
		int i = 0;
		
		for (Address to : new Address[] {this.flight_rm, this.room_rm, this.car_rm}) {
			Message m = new Message(to, this.self, this.self, ids[i], type, data);
			com.send(m);
			i++;
		}
		
		i = 0;
		Serializable[] ret = new Serializable[3];
		for (int uid : ids) {
			Serializable result = get_result(this.self, uid);
			if (result == null) {
				return null;
			}
			ret[i] = result;
			i++;
		}
		
		return ret;
	}
	
	private int get_id()
	{ 
		return id++;
	}

	@Override
	public boolean itinerary(int tid, int customer,
			Vector<Integer> flightNumbers, String location, boolean Car,
			boolean Room) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int startTransaction() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean commitTransaction(int tid) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean abortTransaction(int tid) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}
}
