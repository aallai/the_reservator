package ResImpl;

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

public class TcpRmManager implements ResInterface.ResourceManager, ResInterface.Callback, Runnable 
{
	Address self;
	Address flight_rm;
	Address car_rm;
	Address room_rm;
	Address itin_rm;
	Communicator com;
	HashMap<String, Method> actions;
	HashMap<Integer, Message> requests;
	
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
		
		TcpRmManager manager = new TcpRmManager(port, active_rms);
		manager.run();
	}
	
	public TcpRmManager(int port, ArrayList<Address> active_rms) 
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
			Serializable result ;

			try {
				result = (Serializable) act.invoke(this, m.data.toArray());
				send_result(m.from, m.id, result);

			} catch (IllegalArgumentException e) {

				send_error(m.from, m.id, "Wrong parameters for operation: " + m.type);

			} catch (Exception e) {
				e.printStackTrace();
			} 
			
		} else {
			send_error(m.from, m.id, "Requested unsupported operation: " + m.type);
		}
	}
	
	private void send_result(Address to, int id, Serializable result)
	{
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add(result);
		Message m = new Message(to, self, id, "result", data);
		com.send(m);
	}
	
	private void send_error(Address to, int id, String info)
	{
		ArrayList<Serializable> data = new ArrayList<Serializable>();
		data.add(info);
		Message error = new Message(to, self, id, "error", data);
		com.send(error);
	}
	
	@Override
	public boolean addFlight(int id, int flightNum, int flightSeats,
			int flightPrice) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int newCustomer(int id) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean newCustomer(int id, int cid) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCars(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteRooms(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCustomer(int id, int customer) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int queryFlight(int id, int flightNumber) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryCars(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryRooms(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String queryCustomerInfo(int id, int customer)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryCarsPrice(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryRoomsPrice(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean reserveFlight(int id, int customer, int flightNumber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reserveCar(int id, int customer, String location)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reserveRoom(int id, int customer, String locationd)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean itinerary(int id, int customer, Vector flightNumbers,
			String location, boolean Car, boolean Room) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

}
