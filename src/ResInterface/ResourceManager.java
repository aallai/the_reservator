package ResInterface;


import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
/** 
 * Simplified version from CSE 593 Univ. of Washington
 *
 * Distributed  System in Java.
 * 
 * failure reporting is done using two pieces, exceptions and boolean 
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * 
 * If there is a boolean return value and you're not sure how it 
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in 
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface ResourceManager extends Remote 
{
    /* Add seats to a flight.  In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return success.
     */
    public boolean addFlight(int tid, int flightNum, int flightSeats, int flightPrice) 
	throws RemoteException; 
    
    /* Add cars to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public boolean addCars(int tid, String location, int numCars, int price) 
	throws RemoteException; 
   
    /* Add rooms to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public boolean addRooms(int tid, String location, int numRooms, int price) 
	throws RemoteException; 			    

			    
    /* new customer just returns a unique customer identifier */
    public int newCustomer(int tid) 
	throws RemoteException; 
    
    /* new customer with providing id */
    public boolean newCustomer(int tid, int cid)
    throws RemoteException;

    /**
     *   Delete the entire flight.
     *   deleteflight implies whole deletion of the flight.  
     *   all seats, all reservations.  If there is a reservation on the flight, 
     *   then the flight cannot be deleted
     *
     * @return success.
     */   
    public boolean deleteFlight(int tid, int flightNum) 
	throws RemoteException; 
    
    /* Delete all Cars from a location.
     * It may not succeed if there are reservations for this location
     *
     * @return success
     */		    
    public boolean deleteCars(int tid, String location) 
	throws RemoteException; 

    /* Delete all Rooms from a location.
     * It may not succeed if there are reservations for this location.
     *
     * @return success
     */
    public boolean deleteRooms(int tid, String location) 
	throws RemoteException; 
    
    /* deleteCustomer removes the customer and associated reservations */
    public boolean deleteCustomer(int tid,int customer) 
	throws RemoteException; 

    /* queryFlight returns the number of empty seats. */
    public int queryFlight(int tid, int flightNumber) 
	throws RemoteException; 

    /* return the number of cars available at a location */
    public int queryCars(int tid, String location) 
	throws RemoteException; 

    /* return the number of rooms available at a location */
    public int queryRooms(int tid, String location) 
	throws RemoteException; 

    /* return a bill */
    public String queryCustomerInfo(int tid,int customer) 
	throws RemoteException; 
    
    /* queryFlightPrice returns the price of a seat on this flight. */
    public int queryFlightPrice(int tid, int flightNumber) 
	throws RemoteException; 

    /* return the price of a car at a location */
    public int queryCarsPrice(int tid, String location) 
	throws RemoteException; 

    /* return the price of a room at a location */
    public int queryRoomsPrice(int tid, String location) 
	throws RemoteException; 

    /* Reserve a seat on this flight*/
    public boolean reserveFlight(int tid, int customer, int flightNumber) 
	throws RemoteException; 

    /* reserve a car at this location */
    public boolean reserveCar(int tid, int customer, String location) 
	throws RemoteException; 

    /* reserve a room certain at this location */
    public boolean reserveRoom(int tid, int customer, String locationd) 
	throws RemoteException; 


    /* reserve an itinerary */
    public boolean itinerary(int tid,int customer,Vector<Integer> flightNumbers,String location, boolean Car, boolean Room)
	throws RemoteException; 
    
    int startTransaction() throws RemoteException;
    
    boolean commitTransaction(int tid) throws RemoteException;
    
    boolean abortTransaction(int tid) throws RemoteException;
}
