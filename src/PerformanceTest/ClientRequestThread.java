package PerformanceTest;

import java.rmi.registry.LocateRegistry;
import ResImpl.MiddlewareRMImpl;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Vector;
import ResImpl.*;

import ResInterface.ResourceManager;
//
//
public class ClientRequestThread extends Thread 
{//
	public static final double MILLISECONDS_PER_SECOND = 1000;
	public static final double NANOSECONDS_PER_MILLISECOND = 1000000;
	public static final int REQUEST_LIMIT = 200; //
	public static double REQUEST_TIME_LIMIT = 120000.0;	//120000 = 2 minutes i think

	public static enum TransactionType {
		NEW_CUSTOMER,
		ITINERARY,
		BOOK_FLIGHT,
		QUERY_BILL,
		VOID
	}

	private double totalTimeInMilliseconds = 0.0;

	private TransactionType transactionType;
	private double sleepTime;
	private int sleepVariation;

	public Vector<Double> results = new Vector<Double>();
	public double average = 0.0;

	private ResourceManager rm = null;
	private int id;
	private int thread_id;

	private int customer_id;
	private String location;
	private Vector<Integer> flightNums = new Vector<Integer>();
	private static int threadCount = 0;

	private boolean areDataValuesSet = false;	//for part a)
	private Vector<Object> dataSet = null;	//for part b)

	private long lastRecordedTime;

	private ArrayList<ResourceManager> rmObjList = new ArrayList<ResourceManager>();

	public ClientRequestThread(TransactionType transactionType, Vector<String> rmList, Vector<Object> aDataSet, double sleepTime, int sleepVariation, long lastRecordedTime) {
		this.transactionType = transactionType;
		this.sleepTime = sleepTime;
		this.sleepVariation = sleepVariation;
		this.lastRecordedTime = 0;

		thread_id = ++threadCount;
		//
		try 
		{
			for (int k = 0; k < rmList.size(); k++) {
				// get a reference to the rmiregistry.
				String elements[] = ((String)rmList.get(k)).split(":");
				////
				if (elements.length != 2) {
					System.err.println("[rmihost] must be in the format [server:rm_name]");
				}

				String server = elements[0];
				String rm_name = elements[1];					
				Registry registry = LocateRegistry.getRegistry(server);
				// get the proxy and the remote reference by rmiregistry lookup
				rm = (ResourceManager) registry.lookup(rm_name);
				if(rm!=null)
				{
					System.out.println("Connected to RM #" + k);

					rmObjList.add(rm);
				}
				else
				{
					System.out.println("Unsuccessful Connection to RM #" + k);
				}

				if (aDataSet != null) {
					//we assume this is part b)
					this.lastRecordedTime = lastRecordedTime;

					dataSet = new Vector<Object>();
					for (int i = 0; i < aDataSet.size(); i++) {
						customer_id = ((Integer)aDataSet.get(0)).intValue();
						location = new String( (String)(aDataSet.get(1)) );
						flightNums.add(new Integer((Integer)aDataSet.get(2)));
					}
				}
			}

			//main instance of Resource Manager is the RMReplicaManager
			rm = new RMReplicationManager(rmObjList);
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

			//we use estimated request count to prepare enough data for the test
			//up to a point we assume the estimation is in line with the time, but after that we may be
			//overcompensating...so we adjust to avoid have very long setup times before we actually begin the tests.
			double estimatedRequestCount = (REQUEST_TIME_LIMIT > 20000? REQUEST_TIME_LIMIT/3.0 : REQUEST_TIME_LIMIT);


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
			}//gg

			switch(transactionType) {
			case NEW_CUSTOMER:
				System.out.println("Thread #" + thread_id + " Creating new customers...");
				break;
			case QUERY_BILL:
				System.out.println("Thread #" + thread_id + "Querying bill for customer with customer_id: "+customer_id + "...");
				break;
			case ITINERARY:
				System.out.println("Thread #" + thread_id + "Creating itineraries for customer with customer_id: "+customer_id + "...");

				for (double k = 0; k < estimatedRequestCount; k+=1.0) {
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
				for (double y = 0; y < estimatedRequestCount; y+=1.0) {
					try{
						id = rm.startTransaction();
						rm.addFlight(id, flightNums.get(0).intValue(), 1, 5);
						rm.commitTransaction(id);
					} catch(Exception e) {
						System.out.println("Could not perform setup to add " + y + "th flight");
						e.printStackTrace();
					}
				}

				break;
			default:
				break;
			}
		} else {
			//setup has been done in ClientPerformanceTest class.//
		}

		try {
			long currentTime;
			totalTimeInMilliseconds = 0.0;
			if (lastRecordedTime == 0) lastRecordedTime = System.nanoTime();//
			System.out.println("Beginning Test for Thread #" + thread_id);
			double requestCount = 0.0;
			//while (i++ < REQUEST_LIMIT) {
			while (totalTimeInMilliseconds < REQUEST_TIME_LIMIT) {
				requestCount += 1.0;
				//System.out.println("Thread #" + thread_id + " time elapsed: " + totalTimeInMilliseconds);
				currentTime = System.nanoTime();
				totalTimeInMilliseconds += ((double)(currentTime - lastRecordedTime))/NANOSECONDS_PER_MILLISECOND;
				lastRecordedTime = currentTime;

				switch(transactionType) {
				case NEW_CUSTOMER:
					//System.out.println("New Customer");
					start = System.nanoTime();

					try {
						id = rm.startTransaction();
						rm.newCustomer(id);
						rm.commitTransaction(id);
					} catch(ResImpl.TransactionAbortedException e) {
						System.out.println("Ooops " + e.getMessage());
						e.printStackTrace();
					} catch(ResImpl.InvalidTransactionNumException e) {
						System.out.println("Ooops " +e.getMessage());
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						end = System.nanoTime();
						results.add(new Double((double)(end-start)/NANOSECONDS_PER_MILLISECOND));
					}
					//
					break;
				case QUERY_BILL:
					//System.out.println("Query Bill");

					start = System.nanoTime();
					try {
						id = rm.startTransaction();
						rm.queryCustomerInfo(id,customer_id);
						rm.commitTransaction(id);
					} catch(ResImpl.TransactionAbortedException e) {
						System.out.println("Ooops " + e.getMessage());
						e.printStackTrace();
					} catch(ResImpl.InvalidTransactionNumException e) {
						System.out.println("Ooops " +e.getMessage());
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						end = System.nanoTime();
						results.add(new Double((double)(end-start)/NANOSECONDS_PER_MILLISECOND));
					}

					break;
				case ITINERARY:
					//System.out.println("Book Itinerary");

					start = System.nanoTime();
					try {
						id = rm.startTransaction();
						rm.itinerary(id, customer_id, flightNums, "Toronto", true, true);
						rm.commitTransaction(id);
					} catch(ResImpl.TransactionAbortedException e) {
						System.out.println("Ooops " + e.getMessage());
						e.printStackTrace();
					} catch(ResImpl.InvalidTransactionNumException e) {
						System.out.println("Ooops " +e.getMessage());
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						end = System.nanoTime();
						results.add(new Double((double)(end-start)/NANOSECONDS_PER_MILLISECOND));
					}

					break;
				case BOOK_FLIGHT:
					//	System.out.println("Thread #" + thread_id + "Book Flight - " + requestCount);

					start = System.nanoTime();

					try {
						id = rm.startTransaction();
						rm.reserveFlight(id, customer_id, flightNums.get(0).intValue());
						rm.commitTransaction(id);
					} catch(ResImpl.TransactionAbortedException e) {
						System.out.println("Ooops " + e.getMessage());
						e.printStackTrace();
					} catch(ResImpl.InvalidTransactionNumException e) {
						System.out.println("Ooops " +e.getMessage());
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						end = System.nanoTime();
						results.add(new Double((double)(end-start)/NANOSECONDS_PER_MILLISECOND));
					}

					//		System.out.println("Thread #" + thread_id + "BOOKED Flight");

					break;
				}

				responseTime = (long) (((double)(end - start))/NANOSECONDS_PER_MILLISECOND);

				//For variety we choose a sleep time equally distributed within an interval [sleepTime − x; sleepTime + x]
				// where x is the variation..
				sleepTimeWithVariation = (long) (sleepTime*MILLISECONDS_PER_SECOND + Math.random()*2*sleepVariation - sleepVariation);
				if (responseTime < sleepTimeWithVariation) {
					Thread.sleep(sleepTimeWithVariation-responseTime);	//sleepin to control the load on middleware server
				}
			}
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
			System.out.println("Thread #" + thread_id + " time elapsed: " + totalTimeInMilliseconds);
			System.out.println("Thread #" + thread_id + " sumOfResponseTimes: " + sum + ", numOfResponses: " + results.size());
			average = sum / results.size();

			System.out.println("Thread #" + thread_id + " Average Response Time: " + (average) + " milliseconds");
		} catch(ArithmeticException e) {
			System.out.println("Thread #" + thread_id + "Ooops, looks like we didn't gather any response time data...");
		}
	}
}