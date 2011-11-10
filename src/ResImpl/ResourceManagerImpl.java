// -------------------------------
// adapated from Kevin T. Manley
// CSE 593
//
package ResImpl;

import ResInterface.*;
import LockManager.LockManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import LockManager.DeadlockException;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

//public class ResourceManagerImpl extends java.rmi.server.UnicastRemoteObject
public class ResourceManagerImpl
	implements ResourceManager {
	
	private Integer transaction_num = 0;
	
	protected RMHashtable m_itemHT = new RMHashtable();
	private LockManager lm = new LockManager();
	private Hashtable<Integer, RMTransaction> t_table = new Hashtable<Integer, RMTransaction>();
    
    public ResourceManagerImpl() throws RemoteException {
    }

    
    public static void main(String args[]) {
        // Figure out where server is running
        String rmName = "";
        
       
         if (args.length != 1) {
             System.err.println ("Wrong usage");
             System.out.println("Usage: java ResImpl.ResourceManagerImpl [rmName]");
             System.exit(1);
         }
		 
         rmName = args[0];

		 try 
		 {
			Registry registry = LocateRegistry.getRegistry();
			 
			System.out.println("Located Registry");
			
			// create a new Server object
			ResourceManager obj = new ResourceManagerImpl();
			
			System.out.println("Created Stub");

			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);
			
			System.out.println("Created Resource Manager ()");
			
			// Bind the remote object's stub in the registry
			//Note: for registry any host but the localhost will draw an exception here.
			registry.rebind(rmName, rm);
			
			System.out.println("Binded Stub to Registry");
			System.err.println("Server ready");
		} catch(AccessException e) {
			System.err.println("Access Remote Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);			
		} catch(RemoteException e) {
			System.err.println("Remote Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} 
		catch (Exception e) 
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);

		}
    }
    
    
	// Reads a data item
	protected RMItem readData(int tid, String key ) throws DeadlockException
	{
		lm.Lock(tid, key, LockManager.READ);
		
		synchronized(m_itemHT){
			return (RMItem) m_itemHT.get(key);
		}
	}

	// Writes a data item
	private void writeData(int tid, String key, RMItem value ) throws DeadlockException
	{
		
		lm.Lock(tid, key, LockManager.WRITE);
		
		System.out.println("Writing " + key);
		
		synchronized(m_itemHT){
			m_itemHT.put(key, value);
		}
	}
	
	// Remove the item out of storage
	protected RMItem removeData(int tid, String key) throws DeadlockException
	{		
		lm.Lock(tid, key, LockManager.WRITE);
		
		synchronized(m_itemHT){
			return (RMItem)m_itemHT.remove(key);
		}
	}
	
	
	// deletes the entire item
	protected boolean deleteItem(int tid, String key) throws DeadlockException
	{	
		Trace.info("RM::deleteItem(" + tid + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData(tid, key );
		// Check if there is such an item in the storage
		if( curObj == null ) {
			Trace.warn("RM::deleteItem(" + tid + ", " + key + ") failed--item doesn't exist" );
			return false;
		} else {
			if(curObj.getReserved()==0){
				removeData(tid, curObj.getKey());
				Trace.info("RM::deleteItem(" + tid + ", " + key + ") item deleted" );
				
				// add undo
				addWrite(tid, curObj.getKey(), curObj);
					
				return true;
			}
			else{
				Trace.info("RM::deleteItem(" + tid + ", " + key + ") item can't be deleted because some customers reserved it" );
				return false;
			}
		} // if
	}
	

	// query the number of available seats/rooms/cars
	protected int queryNum(int tid, String key) throws DeadlockException 
	{
		Trace.info("RM::queryNum(" + tid + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData(tid, key);
		int value = 0;  
		if( curObj != null ) {
			value = curObj.getCount();
		} // else
		Trace.info("RM::queryNum(" + tid + ", " + key + ") returns count=" + value);
		return value;
	}	
	
	// query the price of an item
	protected int queryPrice(int tid, String key) throws DeadlockException
	{
		Trace.info("RM::queryCarsPrice(" + tid + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData(tid, key);
		int value = 0; 
		if( curObj != null ) {
			value = curObj.getPrice();
		} // else
		Trace.info("RM::queryCarsPrice(" + tid + ", " + key + ") returns cost=$" + value );
		return value;		
	}
	
	// reserve an item
	protected boolean reserveItem(int tid, int customerID, String key, String location) throws DeadlockException
	{		
		Trace.info("RM::reserveItem( " + tid + ", customer=" + customerID + ", " +key+ ", "+location+" ) called" );		
		// Read customer object if it exists (and read lock it)
		Customer cust = (Customer) readData(tid, Customer.getKey(customerID) );		
		if( cust == null ) {
			Trace.warn("RM::reserveItem( " + tid + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
			return false;
		} 
		
		// check if the item is available
		ReservableItem item = (ReservableItem)readData(tid, key);
		if(item==null){
			Trace.warn("RM::reserveItem( " + tid + ", " + customerID + ", " + key+", " +location+") failed--item doesn't exist" );
			return false;
		}else if(item.getCount()==0){
			Trace.warn("RM::reserveItem( " + tid + ", " + customerID + ", " + key+", " + location+") failed--No more items" );
			return false;
		}else{			
			Customer old = new Customer(cust.getID());
			old.m_Reservations = (RMHashtable) cust.getReservations().clone();
			
			System.out.println();
			
			cust.reserve( key, location, item.getPrice());		
			writeData(tid, cust.getKey(), cust );
			
			// add undo info
			addWrite(tid, cust.getKey(), old);
				
			
			ReservableItem old_item = item.copy(); 
			
			// decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved()+1);
			
			addWrite(tid, key, old_item);
			
			
			Trace.info("RM::reserveItem( " + tid + ", " + customerID + ", " + key + ", " +location+") succeeded" );
			return true;
		}		
	}
	
	// Create a new flight, or add seats to existing flight
	//  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public void addFlight(int tid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, 
		TransactionAbortedException, InvalidTransactionNumException
	{	
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
		
			Flight curObj = (Flight) readData(tid, Flight.getKey(flightNum) );
		
			if( curObj == null ) {
				// doesn't exist...add it
				Flight newObj = new Flight( flightNum, flightSeats, flightPrice );
				writeData(tid, newObj.getKey(), newObj );
			
				addRemove(tid, newObj.getKey());
					
			
				Trace.info("RM::addFlight (" + tid + ", " + flightNum + ", " + flightSeats + ", " + flightPrice + ") succeeded");
			
			} else {
				Flight old = (Flight) curObj.copy();

				// add seats to existing flight and update the price...
				curObj.setCount( curObj.getCount() + flightSeats );
			
				if ( flightPrice > 0 ) {
					curObj.setPrice( flightPrice );
				} // if
				writeData(tid, curObj.getKey(), curObj );
			
				addWrite(tid, curObj.getKey(), old);
	
			} // else
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, addFlight operation for transaction " + tid);
		}
	}


	
	public void deleteFlight(int tid, int flightNum) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException	
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			if(!deleteItem(tid, Flight.getKey(flightNum))) {
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "deleteFlight() : delete failed.");
			}
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, deleteFlight operation for transaction " + tid);
		}
	}

	// Create a new room location or add rooms to an existing location
	//  NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public void addRooms(int tid, String location, int count, int price) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
		
			Trace.info("RM::addRooms(" + tid + ", " + location + ", " + count + ", $" + price + ") called" );
			Hotel curObj = (Hotel) readData(tid, Hotel.getKey(location) );
			if( curObj == null ) {
				// doesn't exist...add it
				Hotel newObj = new Hotel( location, count, price );
				writeData(tid, newObj.getKey(), newObj );
				
				// add undo
				addRemove(tid, newObj.getKey());
				
				Trace.info("RM::addRooms(" + tid + ") created new room location " + location + ", count=" + count + ", price=$" + price );
			} else {
				
				Hotel old = (Hotel) curObj.copy();
				
				// add count to existing object and update price...
				curObj.setCount( curObj.getCount() + count );
				if( price > 0 ) {
					curObj.setPrice( price );
				} // if
				writeData(tid, curObj.getKey(), curObj );
				
				addWrite(tid, curObj.getKey(), old);
	 			
				Trace.info("RM::addRooms(" + tid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
			}
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, addRooms operation for transaction " + tid);
		}
		 // else
	}

	// Delete rooms from a location
	public void deleteRooms(int tid, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			if (!deleteItem(tid, Hotel.getKey(location))) {
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "deleteRooms() : delete failed.");
			}
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, deleteRooms operation for transaction " + tid);
		}
	}

	// Create a new car location or add cars to an existing location
	//  NOTE: if price <= 0 and the location already exists, it maintains its current price
	public void addCars(int tid, String location, int count, int price) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			Trace.info("RM::addCars(" + tid + ", " + location + ", " + count + ", $" + price + ") called" );
			Car curObj = (Car) readData(tid, Car.getKey(location) );
			if( curObj == null ) {
				// car location doesn't exist...add it
				Car newObj = new Car( location, count, price );
				writeData(tid, newObj.getKey(), newObj );
				
				// undo
				addRemove(tid, newObj.getKey());
				
				Trace.info("RM::addCars(" + tid + ") created new location " + location + ", count=" + count + ", price=$" + price );
			} else {
				Car old = (Car) curObj.copy();
				
				// add count to existing car location and update price...
				curObj.setCount( curObj.getCount() + count );
				if( price > 0 ) {
					curObj.setPrice( price );
				} // if
				writeData(tid, curObj.getKey(), curObj );
				
				// undo
				addWrite(tid, curObj.getKey(), old);
				
				Trace.info("RM::addCars(" + tid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
			} // else
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, addCars operation for transaction " + tid);
		}
	}


	// Delete cars from a location
	public void deleteCars(int tid, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			if (!deleteItem(tid, Car.getKey(location))) {
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "deleteCars() : delete failed.");
			}
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, deleteCars operation for transaction " + tid);
		}
	}



	// Returns the number of empty seats on this flight
	public int queryFlight(int tid, int flightNum) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		int ret = 0;
		try {
			ret = queryNum(tid, Flight.getKey(flightNum));
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, queryFlight operation for transaction " + tid);
		}
		
		return ret;
	}

	// Returns the number of reservations for this flight. 
//	public int queryFlightReservations(int id, int flightNum)
//		throws RemoteException
//	{
//		Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
//		RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//		if( numReservations == null ) {
//			numReservations = new RMInteger(0);
//		} // if
//		Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
//		return numReservations.getValue();
//	}


	// Returns price of this flight
	public int queryFlightPrice(int tid, int flightNum ) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		int ret = 0;
		try {
			ret = queryPrice(tid, Flight.getKey(flightNum));
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, queryFlightPrice operation for transaction " + tid);
		}
		return ret;
	}


	// Returns the number of rooms available at a location
	public int queryRooms(int tid, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		int ret = 0;
		
		try {
			ret = queryNum(tid, Hotel.getKey(location));
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, queryRooms operation for transaction " + tid);
		}
		
		return ret;
	}


	
	
	// Returns room price at this location
	public int queryRoomsPrice(int tid, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		int ret = 0;
		try {
			ret = queryPrice(tid, Hotel.getKey(location));
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, queryRooms operation for transaction " + tid);
		}
		
		return ret;
	}


	// Returns the number of cars available at a location
	public int queryCars(int tid, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		int ret = 0;
		try {
			ret =  queryNum(tid, Car.getKey(location));
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, queryCars operation for transaction " + tid);
		}
		
		return ret;
	}


	// Returns price of cars at this location
	public int queryCarsPrice(int tid, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		int ret = 0;
		try {
			ret = queryPrice(tid, Car.getKey(location));
		} catch  (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, queryCarsPprice operation for transaction " + tid);
		}
		
		return ret;
	}

	// Returns data structure containing customer reservation info. Returns null if the
	//  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
	//  reservations.
	public RMHashtable getCustomerReservations(int tid, int customerID) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			Trace.info("RM::getCustomerReservations(" + tid + ", " + customerID + ") called" );
			Customer cust = (Customer) readData(tid, Customer.getKey(customerID) );
			if( cust == null ) {

				Trace.warn("RM::getCustomerReservations failed(" + tid + ", " + customerID + ") failed--customer doesn't exist" );
				
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "getCustomerReservations failed for customer " + customerID + ", customer doesn't exist.");
				
			} else {
				return cust.getReservations();
			}
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, getCustomerReservations operation for transaction " + tid);
		}
	}

	// return a bill
	public String queryCustomerInfo(int tid, int customerID) throws RemoteException, 
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			Trace.info("RM::queryCustomerInfo(" + tid + ", " + customerID + ") called" );
			Customer cust = (Customer) readData(tid, Customer.getKey(customerID) );
			if( cust == null ) {
				Trace.warn("RM::queryCustomerInfo(" + tid + ", " + customerID + ") failed--customer doesn't exist" );
				
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "getCustomerReservations failed for customer " + customerID + ", customer doesn't exist.");
				
			} else {
					String s = cust.printBill();
					Trace.info("RM::queryCustomerInfo(" + tid + ", " + customerID + "), bill follows..." );
					System.out.println( s );
					return s;
			} // if
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, getCustomerReservations operation for transaction " + tid);
		}
	}

  // customer functions
  // new customer just returns a unique customer identifier
	// -1 indicates error (invalid tid)
	public int newCustomer(int tid) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			Trace.info("INFO: RM::newCustomer(" + tid + ") called" );
			// Generate a globally unique ID for the new customer
			int cid = Integer.parseInt( String.valueOf(tid) +
									String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
									String.valueOf( Math.round( Math.random() * 100 + 1 )));
	
			Customer cust = new Customer( cid );
			writeData(tid, cust.getKey(), cust );
			
			// add undo
			addRemove(tid, cust.getKey());
			
			Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid );
			return cid;
			
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, newCustomer operation for transaction " + tid);
		}
			
	}

	// I opted to pass in customerID instead. This makes testing easier
	public void newCustomer(int tid, int customerID ) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			Trace.info("INFO: RM::newCustomer(" + tid + ", " + customerID + ") called" );
			Customer cust = (Customer) readData(tid, Customer.getKey(customerID) );
			if( cust == null ) {
				cust = new Customer(customerID);
				writeData(tid, cust.getKey(), cust );
				
				addRemove(tid, cust.getKey());
				
				Trace.info("INFO: RM::newCustomer(" + tid + ", " + customerID + ") created a new customer" );
			
			} else {
				Trace.info("INFO: RM::newCustomer(" + tid + ", " + customerID + ") failed--customer already exists");
				
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "Transaction " + tid + " aborted, newCustomer() : customer already exists.");
				
			} // else
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, newCustomer operation for transaction " + tid);
		}
	}


	// Deletes customer from the database. 
	public void deleteCustomer(int tid, int customerID) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
 		
		try {
			Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") called" );
			Customer cust = (Customer) readData(tid, Customer.getKey(customerID) );
			if( cust == null ) {
				Trace.warn("RM::deleteCustomer(" + tid + ", " + customerID + ") failed--customer doesn't exist" );
				
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "deleteCustomer() : customer doesn't exist.");
				
			} else {			
				// Increase the reserved numbers of all reservable items which the customer reserved. 
				RMHashtable reservationHT = cust.getReservations();
				for(Enumeration e = reservationHT.keys(); e.hasMoreElements();){		
					String reservedkey = (String) (e.nextElement());
					ReservedItem reserveditem = cust.getReservedItem(reservedkey);
					Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times"  );
					ReservableItem item  = (ReservableItem) readData(tid, reserveditem.getKey());
					Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") has reserved " + reserveditem.getKey() + "which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
	
					ReservableItem old = item.copy();
	
					item.setReserved(item.getReserved()-reserveditem.getCount());
					item.setCount(item.getCount()+reserveditem.getCount());
					
					addWrite(tid, reserveditem.getKey(), old);
				}
				
				// remove the customer from the storage
				removeData(tid, cust.getKey());
				
				addWrite(tid, cust.getKey(), cust);
				
				Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") succeeded" );
				
			} // if
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, deleteCustomer operation for transaction " + tid);
		}
	}




	// Frees flight reservation record. Flight reservation records help us make sure we
	//  don't delete a flight if one or more customers are holding reservations
//	public boolean freeFlightReservation(int id, int flightNum)
//		throws RemoteException
//	{
//		Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" );
//		RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//		if( numReservations != null ) {
//			numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) );
//		} // if
//		writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
//		Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") succeeded, this flight now has "
//				+ numReservations + " reservations" );
//		return true;
//	}
//	

	
	// Adds car reservation to this customer. 
	public void reserveCar(int tid, int customerID, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			
			if (!reserveItem(tid, customerID, Car.getKey(location), location)) {
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "reserveCar() : reservation failed.");
			}
			
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, reserveCar operation for transaction " + tid);
		}
	}


	// Adds room reservation to this customer. 
	public void reserveRoom(int tid, int customerID, String location) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
		
			if (!reserveItem(tid, customerID, Hotel.getKey(location), location)) {
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "reserveRoom() : reservation failed.");
			}

		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, reserveRoom operation for transaction " + tid);
		}
	}
	// Adds flight reservation to this customer.  
	public void reserveFlight(int tid, int customerID, int flightNum) throws RemoteException,
		TransactionAbortedException, InvalidTransactionNumException
	{
		if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
		
		try {
			if(!reserveItem(tid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum))) {
				abortTransaction(tid);
				throw new TransactionAbortedException(tid, "reserveFlight() : reservation failed.");
			}
		} catch (DeadlockException e) {
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "Deadlock detected, reserveFlight operation for transaction " + tid);
		}
	}
	
	/* reserve an itinerary */
    public void itinerary(int id,int customer,Vector<Integer> flightNumbers,String location,boolean Car,boolean Room)
	throws RemoteException, TransactionAbortedException, InvalidTransactionNumException
	{
    	return;
    }
    
    /*
     * Returns a transaction number
     */
    public int startTransaction() throws RemoteException
    {
    	int tid;
    	
    	synchronized(transaction_num) {
    		tid = transaction_num++;
    	}
    	
    	this.t_table.put(tid, new RMTransaction(tid));
    	
    	return tid;
    }
    
    public boolean commitTransaction(int tid) throws RemoteException, InvalidTransactionNumException
    {
    	if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
    	
    	Trace.info("commitTransaction() : " + tid);
    	
    	// all operations have already been performed, simply delete transaction
    	this.t_table.remove(tid);
    	this.lm.UnlockAll(tid);
 
    	return true;
    }
    
    //
    public boolean abortTransaction(int tid) throws RemoteException, InvalidTransactionNumException
    {
    	if (!check_tid(tid)) {
			throw new InvalidTransactionNumException(tid);
		}
    	
    	Trace.info("\nabortTransaction() : " + tid);
    	
    	RMTransaction t = this.t_table.get(tid);
    	
    	System.out.println("before >>>");
    	this.m_itemHT.dump();
    	
    	boolean ret = true;
    	
    	// undo operations performed by transaction
    	RMOperation operation;
    	while(!t.undo_stack.empty()) {
    		
    		operation = t.undo_stack.pop();
    		
    		try {
    			operation.op.invoke(this, operation.args.toArray());
    		} catch (InvocationTargetException e) {
    			e.getTargetException().printStackTrace();
    			ret = false;
    		} catch (IllegalAccessException e) {
    			e.printStackTrace();
    			ret = false;
    		}
    	}
    	
    	System.out.println();
    	System.out.println("After >>>");
    	this.m_itemHT.dump();
    	
    	this.t_table.remove(tid);
    	lm.UnlockAll(tid);
    	
    	return ret;
    }
    
    // These must not fail!
    private void addRemove(int tid, String key) 
    {
    	Class rm = this.getClass();
		
    	if (!check_tid(tid)) {
    		System.err.println("undo operation added on nonexistent transaction, add tid check!!!");
    		return;
    	}
		
    		
		RMTransaction t = this.t_table.get(tid);
			
		// add undo info
		Method m = null;
		try {
			m = rm.getDeclaredMethod("removeData", int.class, String.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			System.out.println("Fix reflection!!!");
		}
						
		ArrayList args = new ArrayList();
		args.add(tid);
		args.add(key);	
						
		t.undo_stack.push(new RMOperation(m, args));
    	
		
		return;
    }

    private void addWrite(int tid, String key, RMItem obj)
    {
    	Class rm = this.getClass();
    		
    	if (!check_tid(tid)) {
    		System.err.println("undo operation added on nonexistent transaction, add tid check!!!");
    		return;
    	}
		
    	RMTransaction t = this.t_table.get(tid);
    	
		// add undo
		Method m = null;
		try {
			m = rm.getDeclaredMethod("writeData", int.class, String.class, RMItem.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			System.out.println("Fix reflection!!!");
		}
		
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(tid);
		args.add(key);
		args.add(obj);
		
		t.undo_stack.push(new RMOperation(m, args));
    }
    
    // checks if tid is a valid transaction
    private boolean check_tid(int tid) {
    	RMTransaction t = this.t_table.get(tid);
		
		if (t == null) {
			Trace.error("Non existent transaction " + tid);
			return false;
		}
		return true;
    }
}
