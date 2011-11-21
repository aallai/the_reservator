package ResInterface;


import java.rmi.Remote;
import ResImpl.InvalidTransactionNumException;
import ResImpl.TransactionAbortedException;
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
    public void addFlight(int tid, int flightNum, int flightSeats, int flightPrice) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException; 
    
    /* Add cars to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public void addCars(int tid, String location, int numCars, int price) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException; 
   
    /* Add rooms to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public void addRooms(int tid, String location, int numRooms, int price) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;			    

			    
    /* new customer just returns a unique customer identifier */
    public int newCustomer(int tid) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    
    /* new customer with providing id */
    public void newCustomer(int tid, int cid)
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /**
     *   Delete the entire flight.
     *   deleteflight implies whole deletion of the flight.  
     *   all seats, all reservations.  If there is a reservation on the flight, 
     *   then the flight cannot be deleted
     *
     * @return success.
     */   
    public void deleteFlight(int tid, int flightNum) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    
    /* Delete all Cars from a location.
     * It may not succeed if there are reservations for this location
     *
     * @return success
     */		    
    public void deleteCars(int tid, String location) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* Delete all Rooms from a location.
     * It may not succeed if there are reservations for this location.
     *
     * @return success
     */
    public void deleteRooms(int tid, String location) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* deleteCustomer removes the customer and associated reservations */
    public void deleteCustomer(int tid,int customer) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException; 

    /* queryFlight returns the number of empty seats. */
    public int queryFlight(int tid, int flightNumber) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* return the number of cars available at a location */
    public int queryCars(int tid, String location) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* return the number of rooms available at a location */
    public int queryRooms(int tid, String location) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* return a bill */
    public String queryCustomerInfo(int tid,int customer) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;    
    /* queryFlightPrice returns the price of a seat on this flight. */
    public int queryFlightPrice(int tid, int flightNumber) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* return the price of a car at a location */
    public int queryCarsPrice(int tid, String location) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* return the price of a room at a location */
    public int queryRoomsPrice(int tid, String location) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* Reserve a seat on this flight*/
    public void reserveFlight(int tid, int customer, int flightNumber) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* reserve a car at this location */
    public void reserveCar(int tid, int customer, String location) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;
    /* reserve a room certain at this location */
    public void reserveRoom(int tid, int customer, String locationd) 
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;

    /* reserve an itinerary */
    public void itinerary(int tid,int customer,Vector<Integer> flightNumbers,String location, boolean Car, boolean Room)
    		throws RemoteException, TransactionAbortedException, InvalidTransactionNumException;    

    int startTransaction() throws RemoteException;
    
    boolean commitTransaction(int tid) throws RemoteException, InvalidTransactionNumException;
    
    boolean abortTransaction(int tid) throws RemoteException, InvalidTransactionNumException;
    
    public void reset_timer(int tid) throws RemoteException, InvalidTransactionNumException;
}
