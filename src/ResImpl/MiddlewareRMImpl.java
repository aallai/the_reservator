package ResImpl;
import ResInterface.*;
import java.util.*;
import java.rmi.*;
import java.util.TimerTask;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.lang.Math;


public class MiddlewareRMImpl implements ResourceManager {
	protected ResourceManager flightsRM;
	protected ResourceManager carsRM;
	protected ResourceManager hotelsRM;
	protected Hashtable<Integer, MiddlewareTransaction> t_table;
	Integer t_count = 0;
	
	public MiddlewareRMImpl(ResourceManager flights, ResourceManager cars, ResourceManager hotels) throws RemoteException {
		flightsRM = flights;
		carsRM = cars;
		hotelsRM = hotels;
		
		t_table = new Hashtable<Integer, MiddlewareTransaction>();	
	}

	public static void main(String args[]) {
		// Figure out where server is running
		String server = "";
		String rmName = "";
		ArrayList<ResourceManager> serverRMImplArray = new ArrayList<ResourceManager>();

		if (args.length < 4) {
			System.err.println ("Wrong usage");
			System.out.println("Usage: java ResImpl.ResourceManagerImpl [rmname] [...] [...] [...]\n" + 
					"\tWhere [...] represents the active resource managers for middleware to connect to" + 
					" for cars, hotels, and flights");
			System.exit(1);
		}
	
		rmName = args[0];
		
		//error checking in server:rmname arguments
		for (int i = 1; i < args.length; i++) {
			if(args[i].split(":").length != 2) {
				System.err.println("For each rm = [...], rm must be in the format of [rmhost:rmname].");
				return;
			}
		}

		try 
		{
			Registry registry = LocateRegistry.getRegistry(server);

			//Get resource managers from rmiregistry
			for (int i = 1; i < args.length; i++) {				
			    String elements[] = args[i].split(":");
			    
			    String serveri = elements[0];
			    String rmnamei = elements[1];
			    
				Registry registryi = LocateRegistry.getRegistry(serveri);

				// get the proxy and the remote reference by rmiregistry lookup, we assume rms are on port 1099
				ResourceManager rm = (ResourceManager) registryi.lookup(rmnamei);
				if(rm!=null)
				{					
					serverRMImplArray.add(rm);
					System.out.println("Connected to RM: " + args[i]);
				}
				else
				{
					System.err.println("Unsuccessful.  Could not connect to RM: " + args[i]);
					System.exit(1);
				}
			}

			// create a new Server object
			ResourceManager obj = new MiddlewareRMImpl(serverRMImplArray.get(0), serverRMImplArray.get(1), serverRMImplArray.get(2));

			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			registry.rebind(rmName, rm);

			System.out.println("Successfully Connect to all RMs");
			System.err.println("Server ready");
		} catch(AccessException e) {
			System.err.println("Access Remote Server exception: " + e.toString());
			e.printStackTrace();
			//System.exit(1);			
		} catch(RemoteException e) {
			System.err.println("Remote Server exception: " + e.toString());
			e.printStackTrace();
			//System.exit(1);
		} 
		catch (Exception e) 
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
			//System.exit(1);
		}
	}

	

	// Create a new flight, or add seats to existing flight
	//  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public void addFlight(int tid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		add_rm(t, this.flightsRM);
		int rmtid = t.rm_table.get(this.flightsRM);

		Trace.info("MiddlewareRm()::addFlight(" + tid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called" );
		
		try {
			flightsRM.addFlight(rmtid, flightNum, flightSeats, flightPrice);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "AddFlight failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	public void deleteFlight(int tid, int flightNum) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);

		Trace.info("MiddlewareRm()::deleteFlight(" + tid + ", " + flightNum + ") called" );
		
		add_rm(t, this.flightsRM);
		int rmtid = t.rm_table.get(this.flightsRM);
		
		try {
			flightsRM.deleteFlight(rmtid, flightNum);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "deleteFlight failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Create a new room location or add rooms to an existing location
	//  NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public void addRooms(int tid, String location, int count, int price) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::addRooms(" + tid + ", " + location + ", " + count + ", $" + price + ") called" );

		add_rm(t, this.hotelsRM);
		int rmtid = t.rm_table.get(this.hotelsRM);
		
		try {
			hotelsRM.addRooms(rmtid, location, count, price);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "addRooms failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Delete rooms from a location
	public void deleteRooms(int tid, String location) throws RemoteException, 
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::deleteRooms(" + tid + ", " + location + ") called" );

		add_rm(t, this.hotelsRM);
		int rmtid = t.rm_table.get(this.hotelsRM);
		
		try {
			hotelsRM.deleteRooms(rmtid, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "deleteRooms failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Create a new car location or add cars to an existing location
	//  NOTE: if price <= 0 and the location already exists, it maintains its current price
	public void addCars(int tid, String location, int count, int price) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::addCars(" + tid + ", " + location + ", " + count + ", $" + price + ") called" );

		add_rm(t, this.carsRM);
		int rmtid = t.rm_table.get(this.carsRM);
		
		try {
			carsRM.addCars(rmtid, location, count, price);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "addCars failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Delete cars from a location
	public void deleteCars(int tid, String location) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException 
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::deleteCars(" + tid + ", " + location + ") called" );

		add_rm(t, this.carsRM);
		int rmtid = t.rm_table.get(this.carsRM);
		
		try {
			carsRM.deleteCars(rmtid, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "deleteCars failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int tid, int flightNum) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::queryFlight(" + tid + ", " + flightNum + ") called" );

		add_rm(t, this.carsRM);
		int rmtid = t.rm_table.get(this.flightsRM);
		
		int numberOfEmptySeats;
		
		try {
			numberOfEmptySeats = flightsRM.queryFlight(rmtid, flightNum);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "queryFlight failed.");
		}

		read_unlock(t);
		set_timer(t);
		
		return numberOfEmptySeats;
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
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::queryFlightPrice(" + tid + ", " + flightNum + ") called" );

		add_rm(t, this.flightsRM);
		int rmtid = t.rm_table.get(this.flightsRM);
		
		int flightPrice;
		try {
			flightPrice = flightsRM.queryFlight(rmtid, flightNum);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "queryFlightPrice failed.");
		}

		read_unlock(t);
		set_timer(t);
		
		return flightPrice;
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int tid, String location) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::queryRooms(" + tid + ", " + location + ") called" );

		add_rm(t, this.hotelsRM);
		int rmtid = t.rm_table.get(this.hotelsRM);
		
		int numOfRoomsAvailable;		
		try {
			numOfRoomsAvailable = hotelsRM.queryRooms(rmtid, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "queryRooms failed.");
		}
		
		read_unlock(t);
		set_timer(t);

		return numOfRoomsAvailable;			
	}

	// Returns room price at this location
	public int queryRoomsPrice(int tid, String location) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::queryRoomsPrice(" + tid + ", " + location + ") called" );

		add_rm(t, this.hotelsRM);
		int rmtid = t.rm_table.get(this.hotelsRM);
		
		int price;		
		try {
			price = hotelsRM.queryRoomsPrice(rmtid, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "queryRooms failed.");
		}

		read_unlock(t);
		set_timer(t);
		
		return price;		
	}

	// Returns the number of cars available at a location
	public int queryCars(int tid, String location) throws RemoteException, 
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::queryCars(" + tid + ", " + location + ") called" );

		add_rm(t, this.carsRM);
		int rmtid = t.rm_table.get(this.carsRM);
		
		int numOfCarsAvailable;		
		try {
			numOfCarsAvailable = carsRM.queryCars(rmtid, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "queryCars failed.");
		}
		
		read_unlock(t);
		set_timer(t);

		return numOfCarsAvailable;	
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int tid, String location) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		Trace.info("MiddlewareRm()::queryCarsPrice(" + tid + ", " + location + ") called" );

		MiddlewareTransaction t = read_lock(tid);
		
		add_rm(t, this.carsRM);
		int rmtid = t.rm_table.get(this.carsRM);
		
		int price;		
		try {
			price = carsRM.queryCarsPrice(rmtid, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "queryCarsPrice failed.");
		}

		read_unlock(t);
		set_timer(t);
		
		return price;
	}

	// return a bill
	public String queryCustomerInfo(int tid, int customerID) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::queryCustomerInfo(" + tid + ", " + customerID + ") called" );

		add_rm(t, this.carsRM);
		add_rm(t, this.flightsRM);
		add_rm(t, this.hotelsRM);
		
		int ftid = t.rm_table.get(this.flightsRM);
		int ctid = t.rm_table.get(this.carsRM);
		int htid = t.rm_table.get(this.hotelsRM);
		
		String theBill = "Bill for customer " + customerID;
		String bills[] = new String[3];

		try {
			
			bills[0] = flightsRM.queryCustomerInfo(ftid, customerID);	
			bills[1] = hotelsRM.queryCustomerInfo(htid, customerID);
			bills[2] = carsRM.queryCustomerInfo(ctid, customerID);			
			
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "queryCustomerInfo failed.");
		}

		
		
		for (int i = 0; i < 3; i++) {
			String[] tmp = bills[i].split("\n");
			for (int j = 1; j < tmp.length; j++) {
				theBill += "\n" + tmp[j];
			}
		}

		read_unlock(t);
		set_timer(t);
		
		return theBill;
	}

	// customer functions
	// new customer just returns a unique customer identifier
	public int newCustomer(int tid) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::newCustomer() called" );
		
		
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt( String.valueOf( Math.round( Math.random() * 1000 + 1 )) +
				String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
				String.valueOf( Math.round( Math.random() * 1000 + 1 )));

		add_rm(t, this.carsRM);
		add_rm(t, this.flightsRM);
		add_rm(t, this.hotelsRM);
		
		int ftid = t.rm_table.get(this.flightsRM);
		int ctid = t.rm_table.get(this.carsRM);
		int htid = t.rm_table.get(this.hotelsRM);
		
		try {
			flightsRM.newCustomer(ftid, cid);
			hotelsRM.newCustomer(htid, cid);
			carsRM.newCustomer(ctid, cid);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "newCustomer failed.");
		}

		Trace.info("MiddlewareRm()::newCustomer(" + cid + ") returns ID=" + cid );
		
		read_unlock(t);
		set_timer(t);
		
		return cid;
	}

	public void newCustomer(int tid, int cid ) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::newCustomer(" + cid + ") called" );

		add_rm(t, this.carsRM);
		add_rm(t, this.flightsRM);
		add_rm(t, this.hotelsRM);
		
		int ftid = t.rm_table.get(this.flightsRM);
		int ctid = t.rm_table.get(this.carsRM);
		int htid = t.rm_table.get(this.hotelsRM);
		
		try {
			flightsRM.newCustomer(ftid, cid);
			hotelsRM.newCustomer(htid, cid);
			carsRM.newCustomer(ctid, cid);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "newCustomer failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}


	// Deletes customer from the database. 
	public void deleteCustomer(int tid, int customerID) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("RM::deleteCustomer(" + tid + ", " + customerID + ") called" );

		add_rm(t, this.carsRM);
		add_rm(t, this.flightsRM);
		add_rm(t, this.hotelsRM);
		
		int ftid = t.rm_table.get(this.flightsRM);
		int ctid = t.rm_table.get(this.carsRM);
		int htid = t.rm_table.get(this.hotelsRM);
		
		try {
			flightsRM.deleteCustomer(ftid, customerID);
			hotelsRM.deleteCustomer(htid, customerID);
			carsRM.deleteCustomer(ctid, customerID);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "deleteCustomer failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Adds car reservation to this customer. 
	public void reserveCar(int tid, int customerID, String location) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		Trace.info("MiddlewareRm()::reserveCar(" + tid + ", " + customerID + ", " + location + ") called" );

		add_rm(t, this.carsRM);
		int rmtid = t.rm_table.get(this.carsRM);
		
		try {
			carsRM.reserveCar(rmtid, customerID, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "reserveCar failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Adds room reservation to this customer. 
	public void reserveRoom(int tid, int customerID, String location) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		Trace.info("MiddlewareRm()::reserveRoom(" + tid + ", " + customerID + ", " + location + ") called" );

		MiddlewareTransaction t = read_lock(tid);
		
		add_rm(t, this.hotelsRM);
		int rmtid = t.rm_table.get(this.hotelsRM);
		
		try {
			hotelsRM.reserveCar(rmtid, customerID, location);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "reserveRoom failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	// Adds flight reservation to this customer.  
	public void reserveFlight(int tid, int customerID, int flightNum) throws RemoteException,
		InvalidTransactionNumException, TransactionAbortedException
	{
		MiddlewareTransaction t = read_lock(tid);
		
		add_rm(t, this.flightsRM);
		int rmtid = t.rm_table.get(this.flightsRM);
		
		Trace.info("MiddlewareRm()::reserveFlight(" + tid + ", " + customerID + ", " + flightNum + ") called" );

		try {
			flightsRM.reserveFlight(rmtid, customerID, flightNum);
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "reserveFlight failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	/* reserve an itinerary */ 

	public void itinerary(int tid,int customer,Vector<Integer> flightNumbers,String location,boolean car,boolean room)
		throws RemoteException, InvalidTransactionNumException, TransactionAbortedException
	{    
		MiddlewareTransaction t = read_lock(tid);
		
		String traceStr = "MiddlewareRm()::itinerary(" + tid + ", " + customer + ", < ";

		for(int i : flightNumbers) {
			traceStr += "("+ Integer.toString(i) + "), ";
		}
		
		traceStr += ">, " + location + ", " + car + ", " + room + ") called";
		Trace.info(traceStr);

		add_rm(t, this.flightsRM);
		int ftid = t.rm_table.get(this.flightsRM);
		
		try {
			for (int i : flightNumbers) {
					flightsRM.reserveFlight(ftid, customer, i);		
			}
			
			if (car) {
				add_rm(t, this.carsRM);
				int ctid = t.rm_table.get(this.carsRM);
				carsRM.reserveCar(ctid, customer, location);	
			}
	
			if (room) {
				add_rm(t, this.hotelsRM);
				int htid = t.rm_table.get(this.hotelsRM);
				hotelsRM.reserveRoom(htid, customer, location);	
			}
			
		} catch (TransactionAbortedException e) {
			read_unlock(t);
			abortTransaction(tid);
			throw new TransactionAbortedException(tid, "itinerary failed.");
		}
		
		read_unlock(t);
		set_timer(t);
	}

	@Override
	public int startTransaction() throws RemoteException 
	{
		final int tid;
		synchronized(this.t_count) {
			tid = this.t_count++;
		}
		
		final MiddlewareTransaction t = new MiddlewareTransaction(tid);
		
		// start timer to clean up transactions from crashed clients
		set_timer(t);

		this.t_table.put(tid, t);
		
		return tid;
	}

	@Override
	public boolean commitTransaction(int tid) throws RemoteException, InvalidTransactionNumException 
	{		
			MiddlewareTransaction t = write_lock(tid);
			
			for (ResourceManager rm : t.rm_table.keySet()) {
				rm.commitTransaction(t.rm_table.get(rm));
			}
			
			write_unlock(t);
			
			return true;
		
	}

	@Override
	public boolean abortTransaction(int tid) throws RemoteException, InvalidTransactionNumException 
	{
		MiddlewareTransaction t = write_lock(tid);
			
		for (ResourceManager rm : t.rm_table.keySet()) {
				
			try {
				rm.abortTransaction(t.rm_table.get(rm));
			} catch (InvalidTransactionNumException e) {
				// a transaction may have aborted already, squelch
			}
		}
			
		write_unlock(t);
		
		return true;
	}
	
	private void set_timer(MiddlewareTransaction t)
	{
		final int final_tid = t.tid;
		final Timer timer = new Timer(true); 
		
		TimerTask timeout = new TimerTask()    
		{
			public void run()
			{
				try {
					timer.cancel();
					abortTransaction(final_tid);
				} catch (InvalidTransactionNumException e) {
					e.printStackTrace();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		};
		
		timer.schedule(timeout, MiddlewareTransaction.T_TIMEOUT, MiddlewareTransaction.T_TIMEOUT);
		t.timer = timer;
	}
	
	private MiddlewareTransaction read_lock(int tid) throws InvalidTransactionNumException
	{
		synchronized(this.t_table) {
			MiddlewareTransaction t = this.t_table.get(tid);
			if (t == null) throw new InvalidTransactionNumException(tid);
			t.lock.readLock().lock();
			t.timer.cancel();
			return t;
		}
	}
	
	private void read_unlock(MiddlewareTransaction t)
	{
		t.lock.readLock().unlock();
	}
	
	private MiddlewareTransaction write_lock(int tid) throws InvalidTransactionNumException
	{
		synchronized(this.t_table) {
			MiddlewareTransaction t = this.t_table.remove(tid);
			if (t == null) throw new InvalidTransactionNumException(tid);
			t.lock.writeLock().lock();
			t.timer.cancel();
			return t;
		}
	}
	
	private void write_unlock(MiddlewareTransaction t)
	{
		t.lock.writeLock().unlock();
	}
	
	private void add_rm(MiddlewareTransaction t, ResourceManager rm)
	{
		if(t.rm_table.get(rm) == null) {
			try {
				t.rm_table.put(rm, rm.startTransaction());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}


