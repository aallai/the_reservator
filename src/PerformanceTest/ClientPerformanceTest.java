package PerformanceTest;

import java.math.BigInteger;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.SecureRandom;
import java.util.Vector;

import ResInterface.ResourceManager;

import PerformanceTest.ClientRequestThread;
import PerformanceTest.ClientRequestThread.TransactionType;
//d
public class ClientPerformanceTest {
	public static enum Request_Composition {
		ONLY_TRANSACTION_1,
		ONLY_TRANSACTION_2,
		MIXTURE
	}

	String server;
	String rm_name;
	ClientRequestThread.TransactionType transactionType1;
	ClientRequestThread.TransactionType transactionType2;
	int load;
	int submitRequestVariation;
	int numberOfClients;

	public static String PART_A = "part_a";
	public static String PART_B = "part_b";
	public static String PART_CMD_LINE = "part_cmd_line";
	
	public static String INTERACTIVE_RUN = "interactive";

	public static final int DATA_SET_SIZE = 7;
	private static final double DATA_SET_SPREAD = 1.0;	//F.or part b) : the closer this value is to 1.0, the less data threads will share. [0,1]

	public int numberOfThreads = 1;
	private String performanceTestType;

	public static Request_Composition requestComposition = Request_Composition.ONLY_TRANSACTION_1;
	
	private Vector<ClientRequestThread> clientThreadTable = new Vector<ClientRequestThread>(); 
	private Vector<Vector<Object>> dataSets = new Vector<Vector<Object>>();

	private SecureRandom randomGen = new SecureRandom();
	
	public ClientPerformanceTest(String performanceTestType, String server, String rm_name, ClientRequestThread.TransactionType transactionType1, ClientRequestThread.TransactionType transactionType2, int numberOfClients, double requestTimeLimit, int load, 
			int submitRequestVariation) {
		this.server = server;
		this.rm_name = rm_name;
		this.transactionType1 = transactionType1;
		this.transactionType2 = transactionType2;
		this.numberOfClients = numberOfClients;
		this.load = load;
		this.submitRequestVariation = submitRequestVariation;
		this.performanceTestType = performanceTestType;
		ClientRequestThread.REQUEST_TIME_LIMIT = requestTimeLimit;
		
		/*
		 * Because part b) involves concurrency to avoid any chance of disturbing our results
		 * we allocate all of the data we'll need here before setting up and running the threads.
		 */
		if (performanceTestType.equalsIgnoreCase(PART_B)) {
			int id;
			int counter = 0;
			this.numberOfClients = numberOfClients;
			
			System.out.println("Creating Threads - PART_B");
			
			try 
			{
				Registry registry = LocateRegistry.getRegistry(server);
				ResourceManager rm = (ResourceManager) registry.lookup(rm_name);
				if(rm == null) {
					throw new Exception();
				}

				numberOfThreads = numberOfClients;

				//we use estimated request count to prepare enough data for the test
				//up to a point we assume the estimation is in line with the time, but after that we may be
				//overcompensating...so we adjust to avoid have very long setup times before we actually begin the tests.
				double estimatedRequestCount = (ClientRequestThread.REQUEST_TIME_LIMIT > 20000? ClientRequestThread.REQUEST_TIME_LIMIT/10.0 : ClientRequestThread.REQUEST_TIME_LIMIT/1.5);				
				//
				System.out.println("Creating datasets to handle " + estimatedRequestCount + " request each");
				
				//each iteration represents one unique data set
				while (counter <= numberOfClients*DATA_SET_SPREAD) {
					Vector<Object> dataSetContainer = new Vector<Object>();
					
					//-- The following. data is added to the dataSetContainer - to be given to ClientRequestMethod for initialization.
					int customer_id = 0;
					String location =  new BigInteger(130, randomGen).toString(32).toUpperCase();
					Vector<Integer> flightNums = new Vector<Integer>();
					
					try {
						id = rm.startTransaction();
						customer_id=rm.newCustomer(id);
						rm.commitTransaction(id);
					} catch (Exception e) {
						e.printStackTrace();
					}

					System.out.println("Creating Location: " + location);

					ClientRequestThread.TransactionType trxnType;					
					if (requestComposition == Request_Composition.ONLY_TRANSACTION_1) {
						trxnType = transactionType1;
					} else if (requestComposition == Request_Composition.ONLY_TRANSACTION_2) {
						trxnType = transactionType2;
					} else {
						trxnType = ((counter+1) % 2 == 0? transactionType1 : transactionType2);
					}
					
					switch(trxnType) {
					case NEW_CUSTOMER:
						break;
					case QUERY_BILL:
						break;
					case ITINERARY:
						System.out.println("Creating itineraries for customer with customer_id: "+customer_id + "...");

						flightNums.add(new Integer(counter));

						for (double k = 0; k < estimatedRequestCount+1; k+=1.0) {
							try{
								id = rm.startTransaction();
								rm.addFlight(id, counter, 1, 1);
								rm.commitTransaction(id);

								id = rm.startTransaction();
								rm.addCars(id, location, 1, 1);
								rm.commitTransaction(id);

								id = rm.startTransaction();
								rm.addRooms(id, location, 1, 1);
								rm.commitTransaction(id);
							} catch(Exception e) {
								e.printStackTrace();
							}
						}

						break;
					case BOOK_FLIGHT:
						System.out.println("Creating new flights");

						flightNums.add(new Integer(counter));
						//create flight and seats to reserve
							for (double y = 0; y < (int)(estimatedRequestCount/((1+(1-DATA_SET_SPREAD)))) + 1; y+=1.0) {
								try{
									id = rm.startTransaction();
									rm.addFlight(id, counter, 1, 5);
									rm.commitTransaction(id);
								} catch(Exception e) {
									e.printStackTrace();
								}
							}
						break;
					default:
						break;
					}
					
					dataSetContainer.add(new Integer(customer_id));
					dataSetContainer.add(location);
					if (flightNums.size() != 0) {
						dataSetContainer.add((Integer)flightNums.get(0));
					} else {
						dataSetContainer.add(new Integer(666));	
					}

					dataSets.add(dataSetContainer);
					
					++counter;
				} 
			} catch (Exception e) 
			{	
				System.err.println("ClientPerformanceTest couldn't connect to middleware! : " + e.toString());
				e.printStackTrace();

				System.exit(1);
			}
		}
	}

	private void setupThreads(ClientRequestThread.TransactionType transType, int numOfThreads) {
		double requestInterval = 0.0;
		try {
			requestInterval = numberOfClients/load;	//this is in seconds
		} catch (ArithmeticException e) {
			requestInterval = 0;
		}
		Vector<Object> threadDataSet = null;
		if (performanceTestType.equalsIgnoreCase(PART_A)) {
			System.out.println("Creating Thread #1 - PART_A");
			long startTime = System.nanoTime();

			ClientRequestThread crt = new ClientRequestThread(transType, server, rm_name, threadDataSet, 0.0, 0, startTime);
			clientThreadTable.add(crt);
			crt.run();
		} else if (performanceTestType.equalsIgnoreCase(PART_CMD_LINE)) {
			System.out.println("Creating Thread #1 - CMD_LINE_PART");
			long startTime = System.nanoTime();

			ClientRequestThread crt = new ClientRequestThread(transType, server, rm_name, threadDataSet, requestInterval, submitRequestVariation, startTime);
			clientThreadTable.add(crt);
			crt.run();	//////
		} else if (performanceTestType.equalsIgnoreCase(PART_B)) {
			ClientRequestThread.TransactionType trxnType;					
			long startTime = System.nanoTime();

			for (int i = 1; i <= numOfThreads; i++) {
				System.out.println("Creating Thread #" + i + " - PART_B");
				
				threadDataSet = dataSets.get((int)(i % numberOfClients*DATA_SET_SPREAD));
				if (requestComposition == Request_Composition.ONLY_TRANSACTION_1) {
					trxnType = transactionType1;
				} else if (requestComposition == Request_Composition.ONLY_TRANSACTION_2) {
					trxnType = transactionType2;
				} else {
					trxnType = (i % 2 == 0? transactionType1 : transactionType2);
				}
				
				ClientRequestThread crt = new ClientRequestThread(trxnType, server, rm_name, threadDataSet, requestInterval, submitRequestVariation, startTime);
				clientThreadTable.add(crt);
				crt.start();
			}
			
			//we must join all spawn threads and calculate a total average
			try {
				for (int k = 0; k < clientThreadTable.size(); k++) {
					((ClientRequestThread)clientThreadTable.get(k)).join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			double sum = 0.0;
			double average = 0.0;
			for (int k = 0; k < clientThreadTable.size(); k++) {
				sum += ((ClientRequestThread)clientThreadTable.get(k)).average;
			}
			average = sum/clientThreadTable.size();
			
			System.out.println("\n\nOverall average response time in milliseconds = " + average);
		}
	}

	public void start() {
		if (performanceTestType.equalsIgnoreCase(PART_A)) {
			setupThreads(transactionType1, numberOfThreads);
		} else if (performanceTestType.equalsIgnoreCase(PART_B)) {
			setupThreads(null, numberOfThreads);
		}
	}
}
