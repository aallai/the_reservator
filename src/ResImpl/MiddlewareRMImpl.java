package ResImpl;
import ResInterface.*;

import java.util.*;
import java.rmi.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class MiddlewareRMImpl extends ResourceManagerImpl {
	protected ResourceManager flightsRM;
	protected ResourceManager carsRM;
	protected ResourceManager hotelsRM;

	protected int portNumber;
	protected String hostName;
	
	public MiddlewareRMImpl(ResourceManager flights, ResourceManager cars, ResourceManager hotels) throws RemoteException {
		super();

		flightsRM = flights;
		carsRM = cars;
		hotelsRM = hotels;
	}
	
	public void setPort(int port) {
		portNumber = port;
	}
	
	public void setHost(String host) {
		hostName = host;
	}

	public static void main(String args[]) {
		// Figure out where server is running
		int port = -1;
		String server = "";
		String rmName = "";
		ArrayList<ResourceManager> serverRMImplArray = new ArrayList<ResourceManager>();

		if (args.length == 1) {
			server = "localhost";
		} else if (args.length < 6) {
			System.err.println ("Wrong usage");
			System.out.println("Usage: java ResImpl.ResourceManagerImpl [port] [hostname] [rmname] [...] [...] [...]\n" + 
					"\tWhere [...] represents the active resource managers for middleware to connect to" + 
					" for cars, hotels, and flights");
			System.exit(1);
		}

		port = Integer.parseInt(args[0]);
		server = args[1];
		rmName = args[2];
		
		//error checking in server:port:rmname arguments
		for (int i = 3; i < args.length; i++) {
			if(args[i].split(":").length != 3) {
				System.err.println("For each rm = [...], rm must be in the format of [rmhost:port:rmname].");
				return;
			}
		}

		try 
		{
			Registry registry = LocateRegistry.getRegistry(server, port);


			//Get resource managers from rmiregistry
			for (int i = 3; i < args.length; i++) {				
			    String elements[] = args[i].split(":");
			    
			    String serveri = elements[0];
			    int porti = Integer.parseInt(elements[1]);
			    String rmnamei = elements[2];
			    
				Registry registryi = LocateRegistry.getRegistry(serveri, porti);

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

			((MiddlewareRMImpl)obj).setPort(port);
			((MiddlewareRMImpl)obj).setHost(server);

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

	// deletes the entire item
	protected boolean deleteItem(int id, String key)
	{
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::deleteItem(" + id + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData( id, key );
		// Check if there is such an item in the storage
		if( curObj == null ) {
			Trace.warn("MiddlewareRm("+hostName+":"+portNumber+")::deleteItem(" + id + ", " + key + ") failed--item doesn't exist" );
			return false;
		} else {
			if(curObj.getReserved()==0){
				removeData(id, curObj.getKey());
				Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::deleteItem(" + id + ", " + key + ") item deleted" );
				return true;
			}
			else{
				Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::deleteItem(" + id + ", " + key + ") item can't be deleted because some customers reserved it" );
				return false;
			}
		}
	}

	// query the number of available seats/rooms/cars
	protected int queryNum(int id, String key) {
		return -1;
	}	

	// query the price of an item
	protected int queryPrice(int id, String key){
		return -1;		
	}

	// reserve an item
	protected boolean reserveItem(int id, int customerID, String key, String location){
		return false;
	}

	// Create a new flight, or add seats to existing flight
	//  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::addFlight(" + id + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called" );

		boolean result = false;
		synchronized(flightsRM) {
			result = flightsRM.addFlight(id, flightNum, flightSeats, flightPrice);
		}

		return result;
	}

	public boolean deleteFlight(int id, int flightNum)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::deleteFlight(" + id + ", " + flightNum + ") called" );

		boolean result = false;
		synchronized(flightsRM) {
			result = flightsRM.deleteFlight(id, flightNum);
		}

		return result;
	}

	// Create a new room location or add rooms to an existing location
	//  NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int id, String location, int count, int price)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );

		boolean success;
		synchronized(hotelsRM) {
			success = hotelsRM.addRooms(id, location, count, price);
		}

		return success;
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::deleteRooms(" + id + ", " + location + ") called" );

		boolean success;
		synchronized(hotelsRM) {
			success = hotelsRM.deleteRooms(id, location);
		}

		return success;
	}

	// Create a new car location or add cars to an existing location
	//  NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int id, String location, int count, int price)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called" );

		boolean success;
		synchronized(carsRM) {
			success = carsRM.addCars(id, location, count, price);
		}

		return success;
	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::deleteCars(" + id + ", " + location + ") called" );

		boolean success;
		synchronized(carsRM) {
			success = carsRM.deleteCars(id, location);
		}

		return success;
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::queryFlight(" + id + ", " + flightNum + ") called" );

		int numberOfEmptySeats;
		synchronized(flightsRM) {
			numberOfEmptySeats = flightsRM.queryFlight(id, flightNum);
		}

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
	public int queryFlightPrice(int id, int flightNum )
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::queryFlightPrice(" + id + ", " + flightNum + ") called" );

		int flightPrice;
		synchronized(flightsRM) {
			flightPrice = flightsRM.queryFlight(id, flightNum);
		}

		return flightPrice;		
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::queryRooms(" + id + ", " + location + ") called" );

		int numOfRoomsAvailable;		
		synchronized(hotelsRM) {
			numOfRoomsAvailable = hotelsRM.queryRooms(id, location);
		}

		return numOfRoomsAvailable;			
	}

	// Returns room price at this location
	public int queryRoomsPrice(int id, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::queryRoomsPrice(" + id + ", " + location + ") called" );

		int price;		
		synchronized(hotelsRM) {
			price = hotelsRM.queryRoomsPrice(id, location);
		}

		return price;		
	}

	// Returns the number of cars available at a location
	public int queryCars(int id, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::queryCars(" + id + ", " + location + ") called" );

		int numOfCarsAvailable;		
		synchronized(carsRM) {
			numOfCarsAvailable = carsRM.queryCars(id, location);
		}

		return numOfCarsAvailable;	
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::queryCarsPrice(" + id + ", " + location + ") called" );

		int price;		
		synchronized(carsRM) {
			price = carsRM.queryCarsPrice(id, location);
		}

		return price;
	}

	// Returns data structure containing customer reservation info. Returns null if the
	//  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
	//  reservations.
	public RMHashtable getCustomerReservations(int id, int customerID)
			throws RemoteException {
		return null;
	}

	// return a bill
	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::queryCustomerInfo(" + id + ", " + customerID + ") called" );

		String theBill = "";
		String bills[] = new String[3];

		synchronized(flightsRM) {
			bills[0] = flightsRM.queryCustomerInfo(id, customerID);	
		}
		synchronized(hotelsRM) {
			bills[1] = hotelsRM.queryCustomerInfo(id, customerID);
		}
		synchronized(carsRM) {
			bills[2] = carsRM.queryCustomerInfo(id, customerID);			
		}

		for (int i = 0; i < 3; i++) {
			// call may have failed
			if (((String) bills[i]).equals("")) {
				Trace.warn("MiddlewareRm("+hostName+":"+portNumber+")::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
				return "";
			}

			int _j = (i != 0? 1 : 0);	//We want to include the customers information from one of the bills in the return bill
			String[] tmp = ((String) bills[i]).split("\n");
			for (int j = _j; j < tmp.length; j++) {
				theBill += "\n" + tmp[j];
			}
		}

		return theBill;
	}

	// customer functions
	// new customer just returns a unique customer identifier
	public int newCustomer(int id)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::newCustomer(" + id + ") called" );
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt( String.valueOf(id) +
				String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
				String.valueOf( Math.round( Math.random() * 100 + 1 )));

		synchronized(flightsRM) {
			flightsRM.newCustomer(id, cid);
		}
		synchronized(hotelsRM) {
			hotelsRM.newCustomer(id, cid);
		}
		synchronized(carsRM) {
			carsRM.newCustomer(id, cid);
		}

		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::newCustomer(" + cid + ") returns ID=" + cid );
		return cid;
	}

	public boolean newCustomer(int id, int customerID )
			throws RemoteException {
		//Nothing. Only applies to ServerRMImpl
		return false;
	}


	// Deletes customer from the database. 
	public boolean deleteCustomer(int id, int customerID)
			throws RemoteException {
		Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );

		boolean result1 = false, result2 = false, result3 = false;
		synchronized(flightsRM) {
			result1 = flightsRM.deleteCustomer(id, customerID);
		}
		synchronized(hotelsRM) {
			result2 = hotelsRM.deleteCustomer(id, customerID);
		}
		synchronized(carsRM) {
			result3 = carsRM.deleteCustomer(id, customerID);
		}

		if (result1 && result2 && result3) {
			return true;
		} else {
			return false;
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
	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::reserveCar(" + id + ", " + customerID + ", " + location + ") called" );

		boolean success;
		synchronized(carsRM) {
			success = carsRM.reserveCar(id, customerID, location);
		}
		return success;
	}

	// Adds room reservation to this customer. 
	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::reserveRoom(" + id + ", " + customerID + ", " + location + ") called" );

		boolean success;
		synchronized(hotelsRM) {
			success = hotelsRM.reserveCar(id, customerID, location);
		}
		return success;
	}

	// Adds flight reservation to this customer.  
	public boolean reserveFlight(int id, int customerID, int flightNum)
			throws RemoteException {
		Trace.info("MiddlewareRm("+hostName+":"+portNumber+")::reserveFlight(" + id + ", " + customerID + ", " + flightNum + ") called" );

		boolean success;
		synchronized(flightsRM) {
			success = flightsRM.reserveFlight(id, customerID, flightNum);
		}
		return success;
	}

	/* reserve an itinerary */
	public boolean itinerary(int id,int customer,Vector<Integer> flightNumbers,String location,boolean Car,boolean Room)
			throws RemoteException {    	
		String traceStr = "MiddlewareRm("+hostName+":"+portNumber+")::itinerary(" + id + ", " + customer + ", < ";

		for(int i : flightNumbers) {
			traceStr += "("+ Integer.toString(i) + "), ";
		}
		traceStr += ">, " + location + ", " + Car + ", " + Room + ") called";
		Trace.info(traceStr);

		for(int i : flightNumbers) {
			synchronized(flightsRM) {
				int seats = flightsRM.queryFlight(id, i);
				if(seats == 0) {
					return false;
				}
			}
		}

		for (int i : flightNumbers) {
			synchronized(flightsRM) {
				boolean success = flightsRM.reserveFlight(id, customer, i);
				if(!success) {
					return false;
				}
			}
		}

		if(Car) {
			synchronized(carsRM) {
				boolean success = carsRM.reserveCar(id, customer, location);
				if(!success) {
					return false;
				}
			}
		}

		if(Room) {
			synchronized(hotelsRM) {
				boolean success = hotelsRM.reserveRoom(id, customer, location);
				if(!success) {
					return false;
				}
			}	
		}

		return true;
	}
}


