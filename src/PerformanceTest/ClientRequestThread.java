package PerformanceTest;

import java.rmi.registry.LocateRegistry;
import ResImpl.MiddlewareRMImpl;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Vector;
import ResImpl.*;

import ResInterface.ResourceManager;

public class ClientRequestThread extends Thread 
{         
	private static final int NANOSECONDS_PER_MILLISECOND = 1000000;
	public static final int REQUEST_LIMIT = 200; //

	public static enum TransactionType {
		NEW_CUSTOMER,
		ITINERARY,
		BOOK_FLIGHT,
		QUERY_BILL,
		VOID
	}

	private TransactionType transactionType;
	private int sleepTime;
	private int sleepVariation;

	private Vector<Integer> results = new Vector<Integer>();

	private ResourceManager rm = null;
	private int id;
	private int thread_id;
	
	private int customer_id;
	private String location;
	private Vector<Integer> flightNums = new Vector<Integer>();
	private static int threadCount = 0;
	
	private boolean areDataValuesSet = false;	//for part a)
	private Vector<Object> dataSet = null;	//for part b)

	public ClientRequestThread(TransactionType transactionType, String server, String rm_name, Vector<Object> aDataSet, int sleepTime, int sleepVariation) {
		this.transactionType = transactionType;
		this.sleepTime = sleepTime;
		this.sleepVariation = sleepVariation;

		thread_id = ++threadCount;

		try 
		{
			// get a reference to the rmiregistry.
			Registry registry = LocateRegistry.getRegistry(server);
			// get the proxy and the remote reference by rmiregistry lookup
			rm = (ResourceManager) registry.lookup(rm_name);
			if(rm!=null)
			{
				System.out.println("Successful");
				System.out.println("Connected to RM");
			}
			else
			{
				System.out.println("Unsuccessful");
			}

			if (aDataSet != null) {
				//we assume this is part b)
				
				dataSet = new Vector<Object>();
				for (int i = 0; i < aDataSet.size(); i++) {
					customer_id = ((Integer)aDataSet.get(0)).intValue();
					location = new String( (String)(aDataSet.get(1)) );
					flightNums.add(new Integer((Integer)aDataSet.get(2)));
				}
			}
		} 
		catch (Exception e) 
		{	
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();

			System.exit(1);
		}
	}
	
	//khalique: may need to change this to accept vector of flight numbers
	//			may also need to add booleans for hasCars and hasHotels...
	public void setDataValues(int customer_id, int flightNum, String location) {
		this.customer_id = customer_id;
		this.flightNums.add(new Integer(flightNum));
		this.location = location;
		
		areDataValuesSet = true;
	}

	public void run()                       
	{        
		int i = 0;

		long start = 0, end = 0;
		long responseTime;
		long sleepTimeWithVariation;

		double sum = 0;
		double averageResponseTime;
		
		//if dataset is empty this means that we must do setup here
		if (dataSet == null) {
			//do setup
			
			int theFlightNumber;
			String theLocation;

			
			//Creating dummy customer to perform transactions with
			if (!areDataValuesSet) {
				theLocation = "Toronto";
				flightNums.add(new Integer(666));

				try {

					id = rm.startTransaction();
					customer_id=rm.newCustomer(id);
					rm.commitTransaction(id);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else {
				theLocation = this.location;
			}
			
			switch(transactionType) {
			case NEW_CUSTOMER:
				System.out.println("Thread #" + thread_id + " Creating new customers...");
				break;
			case QUERY_BILL:
				System.out.println("Thread #" + thread_id + "Querying bill for customer with customer_id: "+customer_id + "...");
				break;
			case ITINERARY:
				System.out.println("Thread #" + thread_id + "Creating itineraries for customer with customer_id: "+customer_id + "...");
				
				for (int k = 0; k < REQUEST_LIMIT; k++) {
					try{
						id = rm.startTransaction();
						rm.addFlight(id, flightNums.get(0).intValue(), 1, 1);
						rm.commitTransaction(id);

						id = rm.startTransaction();
						rm.addCars(id, theLocation, 1, 1);
						rm.commitTransaction(id);

						id = rm.startTransaction();
						rm.addRooms(id, theLocation, 1, 1);
						rm.commitTransaction(id);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}

				break;
			case BOOK_FLIGHT:
				System.out.println("Thread #" + thread_id + "Booking flights using customer_id: "+customer_id + "...");

				//create flight and seats to reserve
				try{
					id = rm.startTransaction();
					rm.addFlight(id, flightNums.get(0).intValue(), REQUEST_LIMIT+1, 5);
					rm.commitTransaction(id);
				} catch(Exception e) {
					e.printStackTrace();
				}

				break;
			default:
				break;
			}
		} else {
			//setup has been done in ClientPerformanceTest class
		}

		try {
			while (i++ < REQUEST_LIMIT) {
				switch(transactionType) {
				case NEW_CUSTOMER:
					//System.out.println("New Customer");
					
					start = System.nanoTime();
					id = rm.startTransaction();
					rm.newCustomer(id);
					rm.commitTransaction(id);
					end = System.nanoTime();
					
					
					results.add(new Integer((int)(end-start)));
					break;
				case QUERY_BILL:
					//System.out.println("Query Bill");

					start = System.nanoTime();
					id = rm.startTransaction();
					rm.queryCustomerInfo(id,customer_id);
					rm.commitTransaction(id);
					end = System.nanoTime();
					
					
					results.add(new Integer((int)(end-start)));
					break;
				case ITINERARY:
					//System.out.println("Book Itinerary");

					start = System.nanoTime();
					id = rm.startTransaction();
					rm.itinerary(id, customer_id, flightNums, "Toronto", true, true);
					rm.commitTransaction(id);
					end = System.nanoTime();
					
					results.add(new Integer((int)(end-start)));
					break;
				case BOOK_FLIGHT:
					System.out.println("Thread #" + thread_id + "Book Flight");

					start = System.nanoTime();
					id = rm.startTransaction();
					rm.reserveFlight(id, customer_id, 666);
					rm.commitTransaction(id);
					end = System.nanoTime();
					
					System.out.println("Thread #" + thread_id + "BOOKED Flight");

					
					results.add(new Integer((int)(end-start)));
					break;
				}

				responseTime = end - start;

				//For variety we choose a sleep time equally distributed within an interval [sleepTime − x; sleepTime + x]
				// where x is the variation.
				sleepTimeWithVariation = (long) (sleepTime + Math.random()*2*sleepVariation - sleepVariation);
				if (responseTime < sleepTimeWithVariation) {
					Thread.sleep(sleepTimeWithVariation-responseTime);	//sleepin to control the load on middleware server
				}
			}
		} catch(ResImpl.TransactionAbortedException e) {
			System.out.println("Ooops " + e.getMessage());
			e.printStackTrace();
		} catch(ResImpl.InvalidTransactionNumException e) {
			System.out.println("Ooops " +e.getMessage());
			e.printStackTrace();

		} catch(Exception e){
			System.out.println("Ooops " + e.getMessage());
			e.printStackTrace();
		}	

		//print all values while taking avergae
		//	System.out.println("Response Times...");
		for (int j = 0; j < results.size(); j++) {
			sum += results.get(j).intValue();
		//		System.out.println("respTime = " + results.get(j).intValue());
		}

		try {
			averageResponseTime = sum / results.size();

			System.out.println("Thread #" + thread_id + " Average Response Time: " + (averageResponseTime)/NANOSECONDS_PER_MILLISECOND);
		} catch(ArithmeticException e) {
			System.out.println("Thread #" + thread_id + "Ooops, looks like we didn't gather any response time data...");
		}
	}
}