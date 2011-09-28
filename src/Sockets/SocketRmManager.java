package Sockets;

import java.rmi.RemoteException;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;

import ResInterface.Callback;

public class SocketRmManager extends BaseRm 
{
	private Address flight_rm;
	private Address car_rm;
	private Address room_rm;
	private Communicator com;
	private HashMap<Integer, Serializable> results;
	
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
			System.err.println("SocketRmManager() : Fewer than four active rms prvided.");
			for (; i < rms.length; i++) {
				rms[i] = active_rms.get(0);
			}
		}
		
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
			return this.results.get(id);
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
		
		if (actions.containsKey(m.type)) {
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
		} else {
			send_error(m.from, m.id, "Requested unsupported operation: " + m.type);
		}
		
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
}
