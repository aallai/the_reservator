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

public class SocketRmManager implements ResInterface.Callback, Runnable 
{
	Address self;
	Address flight_rm;
	Address car_rm;
	Address room_rm;
	Address itin_rm;
	Communicator com;
	HashMap<String, Method> actions;
	HashMap<Integer, Serializable> results;
	
	int port;
	
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
		Address[] rms = new Address[4];
		int i = 0;
		
		for (Address a : active_rms) {
			try {
				rms[i] = a;
				i++;
			} catch (IndexOutOfBoundsException e) { 
				System.err.println("TcpRmManager() : Ignored one or more rms passed in to constructor.");
				break;
			}
		}
		
		if (i < rms.length) {
			System.err.println("TcpManager() : Fewer than four active rms prvided.");
			for (; i < rms.length; i++) {
				rms[i] = active_rms.get(0);
			}
		}
		
		this.port = port;
		this.flight_rm = rms[0];
		this.car_rm = rms[1];
		this.room_rm = rms[2];
		this.itin_rm = rms[3];
		this.com = new Communicator(port, this);

		try {
			this.self = new Address(InetAddress.getLocalHost().getHostName(), port);
		} catch (UnknownHostException e) {
			System.err.println("Is that you John Wayne? Is it me?");
		}
			
		actions = init_actions();
	}
	
	private HashMap<String, Method> init_actions() 
	{
		HashMap<String, Method> ret = new HashMap<String, Method>();
		
		Method[] methods = this.getClass().getDeclaredMethods();
		for (Method m : methods) {
			ret.put(m.getName(), m);
		}
		return ret;
	}
	
	public void run() 
	{
		com.init();
	}
	
	/**
	 * Where the actions happen.
	 */
	public void received(Message m)
	{
		if (actions.containsKey(m.type)) {

			Method act = actions.get(m.type);

			try {
				act.invoke(this, m.data.toArray());

			} catch (IllegalArgumentException e) {

				send_error(m.from, m.id, "Wrong parameters for operation: " + m.type);

			} catch (Exception e) {
				e.printStackTrace();
			} 
			
		} else {
			send_error(m.from, m.id, "Requested unsupported operation: " + m.type);
		}
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
	 * Called when the server receives an error message from one of the RMs
	 * Note that this notifies of communication errors and whatnot, a failure
	 * booking a flight would be signaled by returning a false boolean value.
	 */
	public void error(Address from, int id, String info)
	{
		System.err.println("TcpRmManager() : error recived from " + from.toString() + 
								" for request " + id );
		System.err.println("---");
		System.err.println(info);
		System.err.println("---");
	}
	
	private void send_error(Address to, int id, String info)
	{
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add(self);
		data.add(id);
		data.add(info);
		Message error = new Message(to, self, id, "error", data);
		com.send(error);
	}
	
	
	public void addFlight(Address from, int id, int flightNum, int flightSeats, int flightPrice) 
	{
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add(self);
		data.add(id); 
		data.add(flightNum);
		data.add(flightSeats);
		data.add(flightPrice);
		
		Message m = new Message(this.flight_rm, this.self, id, "addFlight", data);
		com.send(m);
		
		Serializable result = get_result(id);
		
		data.clear();
		data.add(result);
		m = new Message(from, this.self, id, "result", data);
	}

	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public int newCustomer(int id) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean newCustomer(int id, int cid) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean deleteCars(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean deleteRooms(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean deleteCustomer(int id, int customer) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public int queryFlight(int id, int flightNumber) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int queryCars(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int queryRooms(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public String queryCustomerInfo(int id, int customer)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int queryCarsPrice(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int queryRoomsPrice(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean reserveFlight(int id, int customer, int flightNumber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean reserveCar(int id, int customer, String location)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean reserveRoom(int id, int customer, String locationd)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean itinerary(int id, int customer, Vector flightNumbers,
			String location, boolean Car, boolean Room) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

}
