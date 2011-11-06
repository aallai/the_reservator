// -------------------------------
// adapated from Kevin T. Manley
// CSE 593
//
package ResImpl;

import ResInterface.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
	Hashtable<Integer, RMTransaction> t_table = new Hashtable<Integer, RMTransaction>();
    
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
	private RMItem readData(String key )
	{
		synchronized(m_itemHT){
			return (RMItem) m_itemHT.get(key);
		}
	}

	// Writes a data item
	private void writeData(String key, RMItem value )
	{
		System.out.println("Writing " + key);
		
		synchronized(m_itemHT){
			m_itemHT.put(key, value);
		}
	}
	
	// Remove the item out of storage
	private RMItem removeData(String key){
		
		System.out.println("Removing " + key);
		
		synchronized(m_itemHT){
			return (RMItem)m_itemHT.remove(key);
		}
	}
	
	
	// deletes the entire item
	protected boolean deleteItem(int tid, String key)
	{
		if (!check_tid(tid)) {
			return false;
		}
		
		Trace.info("RM::deleteItem(" + tid + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData( key );
		// Check if there is such an item in the storage
		if( curObj == null ) {
			Trace.warn("RM::deleteItem(" + tid + ", " + key + ") failed--item doesn't exist" );
			return false;
		} else {
			if(curObj.getReserved()==0){
				removeData(curObj.getKey());
				Trace.info("RM::deleteItem(" + tid + ", " + key + ") item deleted" );
				
				// add undo
				if (!addWrite(tid, curObj.getKey(), curObj)) {
					return false;
				}
				
				return true;
			}
			else{
				Trace.info("RM::deleteItem(" + tid + ", " + key + ") item can't be deleted because some customers reserved it" );
				return false;
			}
		} // if
	}
	

	// query the number of available seats/rooms/cars
	protected int queryNum(int id, String key) {
		Trace.info("RM::queryNum(" + id + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData( key);
		int value = 0;  
		if( curObj != null ) {
			value = curObj.getCount();
		} // else
		Trace.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
		return value;
	}	
	
	// query the price of an item
	protected int queryPrice(int id, String key){
		Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData(key);
		int value = 0; 
		if( curObj != null ) {
			value = curObj.getPrice();
		} // else
		Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$" + value );
		return value;		
	}
	
	// reserve an item
	protected boolean reserveItem(int tid, int customerID, String key, String location){

		if (!check_tid(tid)) {
			return false;
		}
		
		Trace.info("RM::reserveItem( " + tid + ", customer=" + customerID + ", " +key+ ", "+location+" ) called" );		
		// Read customer object if it exists (and read lock it)
		Customer cust = (Customer) readData(Customer.getKey(customerID) );		
		if( cust == null ) {
			Trace.warn("RM::reserveCar( " + tid + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
			return false;
		} 
		
		// check if the item is available
		ReservableItem item = (ReservableItem)readData(key);
		if(item==null){
			Trace.warn("RM::reserveItem( " + tid + ", " + customerID + ", " + key+", " +location+") failed--item doesn't exist" );
			return false;
		}else if(item.getCount()==0){
			Trace.warn("RM::reserveItem( " + tid + ", " + customerID + ", " + key+", " + location+") failed--No more items" );
			return false;
		}else{			
			Customer old = new Customer(cust.getID());
			old.m_Reservations = cust.getReservations();
			
			cust.reserve( key, location, item.getPrice());		
			writeData(cust.getKey(), cust );
			
			// add undo info
			if (!addWrite(tid, cust.getKey(), old)) {
				return false;
			}
			
			ReservableItem old_item = item.copy(); 
			
			// decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved()+1);
			
			if (!addWrite(tid, key, old_item)) {
				return false;
			}
			
			Trace.info("RM::reserveItem( " + tid + ", " + customerID + ", " + key + ", " +location+") succeeded" );
			return true;
		}		
	}
	
	// Create a new flight, or add seats to existing flight
	//  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int tid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{	
		if (!check_tid(tid)) {
			return false;
		}
		
		Flight curObj = (Flight) readData(Flight.getKey(flightNum) );
		
		if( curObj == null ) {
			// doesn't exist...add it
			Flight newObj = new Flight( flightNum, flightSeats, flightPrice );
			writeData(newObj.getKey(), newObj );
			
			if (!addRemove(tid, newObj.getKey())) {
				return false;
			}
			
			Trace.info("RM::addFlight (" + tid + ", " + flightNum + ", " + flightSeats + ", " + flightPrice + ") succeeded");
			
		} else {
			Flight old = (Flight) curObj.copy();

			// add seats to existing flight and update the price...
			curObj.setCount( curObj.getCount() + flightSeats );
			
			if ( flightPrice > 0 ) {
				curObj.setPrice( flightPrice );
			} // if
			writeData( curObj.getKey(), curObj );
			
			if (!addWrite(tid, curObj.getKey(), old)) {
				return false;
			}
			
		} // else
		return(true);
	}


	
	public boolean deleteFlight(int tid, int flightNum) throws RemoteException
	{	
		return deleteItem(tid, Flight.getKey(flightNum));
	}

	// Create a new room location or add rooms to an existing location
	//  NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int tid, String location, int count, int price) throws RemoteException
	{
		if (!check_tid(tid)) {
			return false;
		}
		
		Trace.info("RM::addRooms(" + tid + ", " + location + ", " + count + ", $" + price + ") called" );
		Hotel curObj = (Hotel) readData(Hotel.getKey(location) );
		if( curObj == null ) {
			// doesn't exist...add it
			Hotel newObj = new Hotel( location, count, price );
			writeData( newObj.getKey(), newObj );
			
			// add undo
			if (!addRemove(tid, newObj.getKey())) {
				return false;
			}
			
			Trace.info("RM::addRooms(" + tid + ") created new room location " + location + ", count=" + count + ", price=$" + price );
		} else {
			
			Hotel old = (Hotel) curObj.copy();
			
			// add count to existing object and update price...
			curObj.setCount( curObj.getCount() + count );
			if( price > 0 ) {
				curObj.setPrice( price );
			} // if
			writeData( curObj.getKey(), curObj );
			
			if (!addWrite(tid, curObj.getKey(), old)) {
				return false;
			}
 			
			Trace.info("RM::addRooms(" + tid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
		} // else
		return(true);
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location)
		throws RemoteException
	{
		return deleteItem(id, Hotel.getKey(location));
		
	}

	// Create a new car location or add cars to an existing location
	//  NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int tid, String location, int count, int price) throws RemoteException
	{
		if (!check_tid(tid)) {
			return false;
		}
		
		Trace.info("RM::addCars(" + tid + ", " + location + ", " + count + ", $" + price + ") called" );
		Car curObj = (Car) readData( Car.getKey(location) );
		if( curObj == null ) {
			// car location doesn't exist...add it
			Car newObj = new Car( location, count, price );
			writeData( newObj.getKey(), newObj );
			
			// undo
			if (!addRemove(tid, newObj.getKey())) {
				return false;
			}
			
			Trace.info("RM::addCars(" + tid + ") created new location " + location + ", count=" + count + ", price=$" + price );
		} else {
			Car old = (Car) curObj.copy();
			
			// add count to existing car location and update price...
			curObj.setCount( curObj.getCount() + count );
			if( price > 0 ) {
				curObj.setPrice( price );
			} // if
			writeData( curObj.getKey(), curObj );
			
			// undo
			if (!addWrite(tid, curObj.getKey(), old)) {
				return false;
			}
			
			Trace.info("RM::addCars(" + tid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
		} // else
		return(true);
	}


	// Delete cars from a location
	public boolean deleteCars(int id, String location) throws RemoteException
	{
		return deleteItem(id, Car.getKey(location));
	}



	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum) throws RemoteException
	{
		return queryNum(id, Flight.getKey(flightNum));
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
	public int queryFlightPrice(int id, int flightNum ) throws RemoteException
	{
		return queryPrice(id, Flight.getKey(flightNum));
	}


	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location) throws RemoteException
	{
		return queryNum(id, Hotel.getKey(location));
	}


	
	
	// Returns room price at this location
	public int queryRoomsPrice(int id, String location) throws RemoteException
	{
		return queryPrice(id, Hotel.getKey(location));
	}


	// Returns the number of cars available at a location
	public int queryCars(int id, String location) throws RemoteException
	{
		return queryNum(id, Car.getKey(location));
	}


	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) throws RemoteException
	{
		return queryPrice(id, Car.getKey(location));
	}

	// Returns data structure containing customer reservation info. Returns null if the
	//  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
	//  reservations.
	public RMHashtable getCustomerReservations(int id, int customerID) throws RemoteException
	{
		Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
		Customer cust = (Customer) readData( Customer.getKey(customerID) );
		if( cust == null ) {
			Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
			return null;
		} else {
			return cust.getReservations();
		} // if
	}

	// return a bill
	public String queryCustomerInfo(int id, int customerID)
		throws RemoteException
	{
		Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
		Customer cust = (Customer) readData( Customer.getKey(customerID) );
		if( cust == null ) {
			Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
			return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
		} else {
				String s = cust.printBill();
				Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );
				System.out.println( s );
				return s;
		} // if
	}

  // customer functions
  // new customer just returns a unique customer identifier
	// -1 indicates error (invalid tid)
	public int newCustomer(int tid) throws RemoteException
	{
		if (!check_tid(tid)) {
			return -1;
		}
		
		Trace.info("INFO: RM::newCustomer(" + tid + ") called" );
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt( String.valueOf(tid) +
								String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
								String.valueOf( Math.round( Math.random() * 100 + 1 )));

		Customer cust = new Customer( cid );
		writeData( cust.getKey(), cust );
		
		// add undo
		if (!addRemove(tid, cust.getKey())) {
			return -1;
		}
		
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid );
		return cid;
	}

	// I opted to pass in customerID instead. This makes testing easier
	public boolean newCustomer(int tid, int customerID ) throws RemoteException
	{
		if (!check_tid(tid)) {
			return false;
		}
		
		Trace.info("INFO: RM::newCustomer(" + tid + ", " + customerID + ") called" );
		Customer cust = (Customer) readData( Customer.getKey(customerID) );
		if( cust == null ) {
			cust = new Customer(customerID);
			writeData( cust.getKey(), cust );
			
			if (!addRemove(tid, cust.getKey())) {
				return false;
			}
			
			Trace.info("INFO: RM::newCustomer(" + tid + ", " + customerID + ") created a new customer" );
			return true;
		} else {
			Trace.info("INFO: RM::newCustomer(" + tid + ", " + customerID + ") failed--customer already exists");
			return false;
		} // else
	}


	// Deletes customer from the database. 
	public boolean deleteCustomer(int tid, int customerID) throws RemoteException
	{
		if (!check_tid(tid)) {
			return false;
		}
 		
		Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") called" );
		Customer cust = (Customer) readData( Customer.getKey(customerID) );
		if( cust == null ) {
			Trace.warn("RM::deleteCustomer(" + tid + ", " + customerID + ") failed--customer doesn't exist" );
			return false;
		} else {			
			// Increase the reserved numbers of all reservable items which the customer reserved. 
			RMHashtable reservationHT = cust.getReservations();
			for(Enumeration e = reservationHT.keys(); e.hasMoreElements();){		
				String reservedkey = (String) (e.nextElement());
				ReservedItem reserveditem = cust.getReservedItem(reservedkey);
				Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times"  );
				ReservableItem item  = (ReservableItem) readData(reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") has reserved " + reserveditem.getKey() + "which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );

				ReservableItem old = item.copy();

				item.setReserved(item.getReserved()-reserveditem.getCount());
				item.setCount(item.getCount()+reserveditem.getCount());
				
				if (!addWrite(tid, reserveditem.getKey(), old)) {
					return false;
				}
			}
			
			// remove the customer from the storage
			removeData(cust.getKey());
			
			if (!addWrite(tid, cust.getKey(), cust)) {
				return false;
			}
			
			Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") succeeded" );
			return true;
		} // if
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
	public boolean reserveCar(int id, int customerID, String location) throws RemoteException
	{
		return reserveItem(id, customerID, Car.getKey(location), location);
	}


	// Adds room reservation to this customer. 
	public boolean reserveRoom(int id, int customerID, String location) throws RemoteException
	{
		return reserveItem(id, customerID, Hotel.getKey(location), location);
	}
	// Adds flight reservation to this customer.  
	public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException
	{
		return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}
	
	/* reserve an itinerary */
    public boolean itinerary(int id,int customer,Vector<Integer> flightNumbers,String location,boolean Car,boolean Room)
	throws RemoteException {
    	return false;
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
    
    public boolean commitTransaction(int tid) throws RemoteException
    {
    	if (!check_tid(tid)) {
    		return false;
    	}
    	
    	// all operations have already been performed, simply delete transaction
    	this.t_table.remove(tid);
    	
    	return true;
    }
    
    //
    public boolean abortTransaction(int tid) throws RemoteException
    {
    	if (!check_tid(tid)) {
    		return false;
    	}
    	
    	RMTransaction t = this.t_table.get(tid);
    	
    	System.out.println();
    	System.out.println("before >>>");
    	this.m_itemHT.dump();
    	
    	boolean ret = true;
    	// undo operations performed by transaction
    	for (RMOperation operation : t.undo_set) {
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
    	
    	System.out.println("After >>>");
    	this.m_itemHT.dump();
    	
    	return true;
    }
    
    private boolean addRemove(int tid, String key) 
    {
    	Class rm = this.getClass();
		
    	if (!check_tid(tid)) {
    		return false;
    	}
		
		RMTransaction t = this.t_table.get(tid);
		
		// add undo info
		Method m;
		try {
			m = rm.getDeclaredMethod("removeData", String.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return false;
		}
					
		ArrayList args = new ArrayList();
		args.add(key);	
					
		t.undo_set.add(new RMOperation(m, args));	
		
		return true;
    }

    private boolean addWrite(int tid, String key, RMItem obj)
    {
    	Class rm = this.getClass();
    		
    	if (!check_tid(tid)) {
    		return false;
    	}
		
    	RMTransaction t = this.t_table.get(tid);
    	
		// add undo
		Method m;
		try {
			m = rm.getDeclaredMethod("writeData", String.class, RMItem.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return false;
		}
		
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(key);
		args.add(obj);
		
		t.undo_set.add(new RMOperation(m, args));
		
		return true;
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
