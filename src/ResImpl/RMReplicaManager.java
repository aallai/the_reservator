package ResImpl;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Vector;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;

import ResInterface.*;

public class RMReplicaManager implements ResourceManager {
	
	// this hashtable holds replicated middlewares and a mapping from
	// client tids to middleware tids
	private LinkedHashMap<ResourceManager, Hashtable<Integer, Integer>> rm_table; 
	private int next = 0;   // used for round robin load balancing 
	private int tid = 0;
	
	public RMReplicaManager(ArrayList<ResourceManager> rms)
	{
		this.rm_table = new LinkedHashMap<ResourceManager, Hashtable<Integer, Integer>>();
		
		for (ResourceManager rm : rms) {
			this.rm_table.put(rm, new Hashtable<Integer, Integer>());
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
		return next++ % this.rm_table.size();
	}

	private int get_tid()
	{
		return tid++;
	}
	
	public int replicas_available()
	{
		return this.rm_table.size();
	}
	
	private void shutdown()
	{
		System.err.println("All replicas failed. Shutting down.");
		System.exit(-1);
	}
	
	@Override
	public void addFlight(int tid, int flightNum, int flightSeats, int flightPrice) throws TransactionAbortedException, InvalidTransactionNumException 
	{
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void addCars(int tid, String location, int numCars, int price) throws TransactionAbortedException, InvalidTransactionNumException 
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void addRooms(int tid, String location, int numRooms, int price) throws TransactionAbortedException, InvalidTransactionNumException 
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public int newCustomer(int tid) throws TransactionAbortedException, InvalidTransactionNumException 
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
			throw new TransactionAbortedException(tid, e.getMessage());
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
	public void newCustomer(int tid, int cid) throws TransactionAbortedException, InvalidTransactionNumException
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteFlight(int tid, int flightNum) throws TransactionAbortedException, InvalidTransactionNumException 
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteCars(int tid, String location) throws TransactionAbortedException, InvalidTransactionNumException 
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteRooms(int tid, String location) throws TransactionAbortedException, InvalidTransactionNumException 
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public void deleteCustomer(int tid, int customer) throws TransactionAbortedException, InvalidTransactionNumException 
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
			throw new TransactionAbortedException(tid, e.getMessage());
		} catch (InvalidTransactionNumException e) {                     // transactions may time out at middlewares, if one does, abort all
			abortTransaction(tid);
			throw new InvalidTransactionNumException(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
	}

	@Override
	public int queryFlight(int tid, int flightNumber) throws TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);
		
		// reset timers at each middleware
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
			throw new TransactionAbortedException(tid, e.getMessage());
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
	public int queryCars(int tid, String location) throws TransactionAbortedException, InvalidTransactionNumException 
	{
		check_tid(tid);
		
		// reset timers at each middleware
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
			throw new TransactionAbortedException(tid, e.getMessage());
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
	public int queryRooms(int tid, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionNumException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String queryCustomerInfo(int tid, int customer)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionNumException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int queryFlightPrice(int tid, int flightNumber)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionNumException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryCarsPrice(int tid, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionNumException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryRoomsPrice(int tid, String location)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionNumException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void reserveFlight(int tid, int customer, int flightNumber)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionNumException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reserveCar(int tid, int customer, String location)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionNumException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reserveRoom(int tid, int customer, String locationd)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionNumException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void itinerary(int tid, int customer, Vector<Integer> flightNumbers,
			String location, boolean Car, boolean Room) throws RemoteException,
			TransactionAbortedException, InvalidTransactionNumException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int startTransaction() 
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
	public boolean commitTransaction(int tid) throws InvalidTransactionNumException 
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
		
		for (Iterator<ResourceManager> i = this.rm_table.keySet().iterator(); i.hasNext(); ) {
			ResourceManager rm = i.next();
			this.rm_table.get(rm).remove(tid);
		}
		
		if (replicas_available() == 0) {
			shutdown();
		}
		
		return true;
	}

	@Override
	public boolean abortTransaction(int tid) throws InvalidTransactionNumException 
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
	
	public void reset_timer(int tid) throws InvalidTransactionNumException
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
	}
}
