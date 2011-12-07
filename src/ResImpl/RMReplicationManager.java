package ResImpl;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Vector;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;

import ResInterface.*;

public class RMReplicationManager implements ResourceManager {

	// this hashtable holds replicated middlewares and a mapping from
	// client tids to middleware tids
	private LinkedHashMap<ResourceManager, Hashtable<Integer, Integer>> rm_table; 

	private int next = 0;   // used for round robin load balancing 
	private int tid = 0;

	private boolean isStillAlive = true;
	private String address;

	static ResourceManager rm = null;

	public static void main(String[] args) {
		ArrayList<ResourceManager> rm_list = new ArrayList<ResourceManager>();

		String rmName = args[0];

		try {
			Registry registry = LocateRegistry.getRegistry();

			for (String s : args) {
				if (s.equals(rmName)) continue;

				String elements[] = s.split(":");

				if (elements.length != 2) {
					System.err.println("[rmihost] must be in the format [server:rm_name]");
				}

				String server = elements[0];
				String rm_name = elements[1];

				try 
				{
					// get a reference to the rmiregistry
					Registry registryi = LocateRegistry.getRegistry(server);
					// get the proxy and the remote reference by rmiregistry lookup
					rm = (ResourceManager) registryi.lookup(rm_name);

					if(rm!=null) {
						rm_list.add(rm);
						System.out.println("RMReplicationManager Connected to RM " + rm_name);
					} else {
						System.out.println("RMReplicationManaer Unsuccessful");
						System.out.println("RMReplicationManager Not connected to RM");
					}
				} catch(AccessException e) {
					System.err.println("Access Remote Server exception: " + e.toString());
					e.printStackTrace();
				} catch(RemoteException e) {
					System.err.println("RMReplicationManager Remote Server exception: " + e.toString());
					e.printStackTrace();
				} catch (Exception e) 
				{
					System.err.println("RMReplicationManager Server exception: " + e.toString());
					e.printStackTrace();
				}
			}

			ResourceManager obj = new RMReplicationManager(rm_list);

			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			registry.rebind(rmName, rm);

			System.err.println("RMReplicationManager Server ready");


			Thread t = new Thread(new HeartbeatThread(rm));
			t.start();
			try{
				while (true) {
					Thread.sleep(20000);
				} 
			}catch(InterruptedException ei){
				System.err.println(ei);
			}

			System.exit(0);
		} catch(AccessException e) {
			System.err.println("Access Remote Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);			
		} catch(RemoteException e) {
			System.err.println("RMReplicationManager Remote Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} 
		catch (Exception e) 
		{
			System.err.println("RMReplicationManager Server exception: " + e.toString());
			e.printStackTrace();
			//System.exit(1);
		}

		System.exit(0);
		System.out.println("Leaving main of RMReplicationManager");		
	} 

	public RMReplicationManager(ArrayList<ResourceManager> rms)
	{
		this.rm_table = new LinkedHashMap<ResourceManager, Hashtable<Integer, Integer>>();

		for (ResourceManager rm : rms) {
			this.rm_table.put(rm, new Hashtable<Integer, Integer>());
		}
	}

	@Override
	public int beat() throws RemoteException{
		if (isStillAlive) {
			return 1;
		} else {
			return -1;
		}
	}

	private void check_tid(int tid) throws InvalidTransactionNumException
	{
		for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
			if (this.rm_table.get(i.next()).get(tid) == null) {
				throw new InvalidTransactionNumException(tid);
			}
		}
	}

	private int lookup_tid(ResourceManager rm, int tid)
	{
		return this.rm_table.get(rm).get(tid);
	}

	private int round_robin()
	{
		return next++ % this.rm_table.keySet().size();
	}

	private int get_tid()
	{
		return tid++;
	}

	public int replicas_available() throws RemoteException
	{
		return this.rm_table.size();
	}

	private void shutdown()
	{
		System.err.println("All replicas failed. Shutting down.");
		System.exit(-1);
	}

	@Override
	public void addFlight(int tid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		System.out.println("Oooops");

		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.addFlight(lookup_tid(rm, tid), flightNum, flightSeats, flightPrice);
				} catch (RemoteException e) {
					i.remove();
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			e.tid = tid;
			throw e;
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void addCars(int tid, String location, int numCars, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.addCars(lookup_tid(rm, tid), location, numCars, price);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void addRooms(int tid, String location, int numRooms, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.addRooms(lookup_tid(rm, tid), location, numRooms, price);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public int newCustomer(int tid) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{	
		check_tid(tid);

		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt( String.valueOf( Math.round( Math.random() * 9 + 1 )) +
				String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
				String.valueOf( Math.round( Math.random() * 1000 + 1 )));

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.newCustomer(lookup_tid(rm, tid), cid);
				} catch (RemoteException e) {

					i.remove();
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return cid;
	}

	@Override
	public void newCustomer(int tid, int cid) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.newCustomer(lookup_tid(rm, tid), cid);
				} catch (RemoteException e) {
					i.remove();
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteFlight(int tid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.deleteFlight(lookup_tid(rm, tid), flightNum);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteCars(int tid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.deleteCars(lookup_tid(rm, tid), location);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteRooms(int tid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.deleteRooms(lookup_tid(rm, tid), location);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteCustomer(int tid, int customer) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.deleteCustomer(lookup_tid(rm, tid), customer);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public int queryFlight(int tid, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		// reset timers at each middleware
		reset_timer(tid);

		int flight = 0;

		try {
			while (replicas_available() > 0) {

				ResourceManager rm = null;

				try {
					int n = round_robin();
					rm = (ResourceManager) this.rm_table.keySet().toArray()[n];
					flight = rm.queryFlight(lookup_tid(rm, tid), flightNumber);
					break;
				} catch (RemoteException e) {
					this.rm_table.remove(rm);
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return flight;
	}

	@Override
	public int queryCars(int tid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		// reset timers at each middleware
		reset_timer(tid);

		int cars = 0;

		try {
			while (replicas_available() > 0) {

				ResourceManager rm = null;

				try {
					int n = round_robin();
					rm = (ResourceManager) this.rm_table.keySet().toArray()[n];
					cars = rm.queryCars(lookup_tid(rm, tid), location);
					break;
				} catch (RemoteException e) {
					this.rm_table.remove(rm);
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return cars;	
	}

	@Override
	public int queryRooms(int tid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		reset_timer(tid);

		int rooms = 0;

		try {
			while (replicas_available() > 0) {

				ResourceManager rm = null;

				try {
					int n = round_robin();
					rm = (ResourceManager) this.rm_table.keySet().toArray()[n];
					rooms = rm.queryRooms(lookup_tid(rm, tid), location);
					break;
				} catch (RemoteException e) {
					this.rm_table.remove(rm);
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return rooms;
	}

	@Override
	public String queryCustomerInfo(int tid, int customer) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		// reset timers at each middleware
		reset_timer(tid);

		String bill = null;

		try {
			while (replicas_available() > 0) {

				ResourceManager rm = null;

				try {
					int n = round_robin();
					rm = (ResourceManager) this.rm_table.keySet().toArray()[n];
					bill = rm.queryCustomerInfo(lookup_tid(rm, tid), customer);
					break;
				} catch (RemoteException e) {
					this.rm_table.remove(rm);
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return bill;
	}

	@Override
	public int queryFlightPrice(int tid, int flightNumber) throws RemoteException,  TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		// reset timers at each middleware
		reset_timer(tid);

		int price = 0;

		try {
			while (replicas_available() > 0) {

				ResourceManager rm = null;

				try {
					int n = round_robin();
					rm = (ResourceManager) this.rm_table.keySet().toArray()[n];
					price = rm.queryFlightPrice(lookup_tid(rm, tid), flightNumber);
					break;
				} catch (RemoteException e) {
					this.rm_table.remove(rm);
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return price;
	}

	@Override
	public int queryCarsPrice(int tid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		// reset timers at each middleware
		reset_timer(tid);

		int price = 0;

		try {
			while (replicas_available() > 0) {

				ResourceManager rm = null;

				try {
					int n = round_robin();
					rm = (ResourceManager) this.rm_table.keySet().toArray()[n];
					price = rm.queryCarsPrice(lookup_tid(rm, tid), location);
					break;
				} catch (RemoteException e) {
					this.rm_table.remove(rm);
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return price;
	}

	@Override
	public int queryRoomsPrice(int tid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		// reset timers at each middleware
		reset_timer(tid);

		int price = 0;

		try {
			while (replicas_available() > 0) {

				ResourceManager rm = null;

				try {
					int n = round_robin();
					rm = (ResourceManager) this.rm_table.keySet().toArray()[n];
					price = rm.queryRoomsPrice(lookup_tid(rm, tid), location);
					break;
				} catch (RemoteException e) {
					this.rm_table.remove(rm);
				}
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return price;
	}

	@Override
	public void reserveFlight(int tid, int customer, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.reserveFlight(lookup_tid(rm, tid), customer, flightNumber);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

	}

	@Override
	public void reserveCar(int tid, int customer, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.reserveCar(lookup_tid(rm, tid), customer, location);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void reserveRoom(int tid, int customer, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.reserveRoom(lookup_tid(rm, tid), customer, location);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void itinerary(int tid, int customer, Vector<Integer> flightNumbers, String location, boolean Car, boolean Room) 
			throws RemoteException, TransactionAbortedException, InvalidTransactionNumException 
			{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.itinerary(lookup_tid(rm, tid), customer, flightNumbers, location, Car, Room);
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (TransactionAbortedException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, e.reason);
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
			}

	@Override
	public int startTransaction() throws RemoteException 
	{
		int tid = get_tid();

		for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
			try {
				ResourceManager rm = i.next();
				this.rm_table.get(rm).put(tid, rm.startTransaction());
			} catch (RemoteException e) {
				i.remove();
			}
		}

		return tid;
	}

	@Override
	public boolean commitTransaction(int tid) throws RemoteException, InvalidTransactionNumException 
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.commitTransaction(lookup_tid(rm, tid));
				} catch (RemoteException e) {
					i.remove();
				}
			} 
		} catch (InvalidTransactionNumException e) {  // timer expired somewhere
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
			ResourceManager rm = i.next();
			this.rm_table.get(rm).remove(tid);
		}

		return true;
	}

	@Override
	public boolean abortTransaction(int tid) throws RemoteException, InvalidTransactionNumException 
	{
		check_tid(tid);

		for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
			try {
				ResourceManager rm = i.next();
				rm.abortTransaction(this.rm_table.get(rm).get(tid));
			} catch (InvalidTransactionNumException e) {

			} catch (RemoteException e) {
				i.remove();
			}
		}

		for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
			ResourceManager rm = i.next();
			this.rm_table.get(rm).remove(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}

		return true;
	}

	public void reset_timer(int tid) throws RemoteException, InvalidTransactionNumException
	{
		check_tid(tid);

		try {
			for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
				try {
					ResourceManager rm = i.next();
					rm.reset_timer(lookup_tid(rm, tid));
				} catch (RemoteException e) {
					i.remove();
				} 
			}
		} catch (InvalidTransactionNumException e) {  // at least one timer expired, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}

		if (replicas_available() == 0) {
			shutdown();
		}
	}

	public String getAddress() throws RemoteException {
		return address;
	}

	public void setAddress(String addr) throws RemoteException {
		address = addr;

		System.out.println("ReplicationManager (" + address + ")");
	}

}

class HeartbeatThread implements Runnable{
	private int nothingCount = 0;

	private ResourceManager rm;

	public HeartbeatThread(ResourceManager rm){
		this.rm = rm;
	}

	public void run(){ 
		//create memoroy map for heartbeat file

		// Create a read-write memory-mapped file
		File file = new File("heartbeat.txt");
		FileChannel rwChannel;
		ByteBuffer wrBuf;

		BufferedWriter bw;	    

		boolean firstRun = true;
		int nothingCount = 0;
		try 
		{
			rwChannel = new RandomAccessFile(file, "rw").getChannel();
			System.out.println("Size: " + (int)rwChannel.size());
			wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, 10);

			while(true){
				try {
					rm.beat();
				} catch (RemoteException e) {
					System.out.println("Unable to reach ReplicaManager...");
					System.exit(1);
				}

				if (firstRun) {
					firstRun = false;

					Runtime.getRuntime().exec("java Daemonator &");
				}
				
				byte readByte = wrBuf.get(0);
				wrBuf.clear();

				if (readByte == (byte)0x0) {
					wrBuf.put(0, (byte)0x1);
					nothingCount = 0;
				} else {
					nothingCount++;
				}

				wrBuf.clear();

				try {
					bw = new BufferedWriter(new FileWriter("resultsReplicationManager.txt", true));
					bw.write("mmap: " + readByte);
					bw.newLine();

					if (nothingCount == 20){
						nothingCount = 0;
						
						Runtime.getRuntime().exec("java Daemonator &");
						bw.write("Restart Daemonator");
						bw.newLine();
					}

					bw.flush();
					bw.close();
				} catch(Exception e) {
					System.out.println(e);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
	}
}

