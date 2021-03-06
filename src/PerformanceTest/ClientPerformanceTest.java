package PerformanceTest;

import java.math.BigInteger;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Vector;

import ResImpl.RMReplicationManager;
import ResInterface.ResourceManager;

import LockManager.DeadlockException;
import LockManager.LockManager;
import PerformanceTest.ClientRequestThread;
import PerformanceTest.ClientRequestThread.TransactionType;
//d
public class ClientPerformanceTest {
	public static enum Request_Composition {
		ONLY_TRANSACTION_1,
		ONLY_TRANSACTION_2,
		MIXTURE
	}

	private static boolean TESTING_LOCK_MANAGER = false;
    private static boolean RUNNING_PERMANCE_TEST = true;
    private static boolean COMMAND_LINE = false;
	
    private static ClientPerformanceTest performanceManager;
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
	private ArrayList<ResourceManager> rmObjList = new ArrayList<ResourceManager>();
    
	public static Vector<String> rm_name_list = new Vector<String>();
	
	public static ClientRequestThread.TransactionType stringToTransactionType(String str) {
    	if (str.equalsIgnoreCase("new_customer")) {
    		return ClientRequestThread.TransactionType.NEW_CUSTOMER;
    	} else if (str.equalsIgnoreCase("itinerary")) {
    		return ClientRequestThread.TransactionType.ITINERARY;
    	} else if (str.equalsIgnoreCase("book_flight")) {
    		return ClientRequestThread.TransactionType.BOOK_FLIGHT;
    	} else if (str.equalsIgnoreCase("query_bill")) {
    		return ClientRequestThread.TransactionType.QUERY_BILL;
    	}
    	
    	return ClientRequestThread.TransactionType.VOID;
    }
	
	public static void main(String args[]) {
    	if (args.length == 1) {
			COMMAND_LINE = true;	
    	} else if (args.length > 1) {
    		if (args[1].equalsIgnoreCase("part_a") || args[1].equalsIgnoreCase("part_b")) {
    			RUNNING_PERMANCE_TEST = true;
    		} else if (args[1].equalsIgnoreCase("lock_manager")) {
    			TESTING_LOCK_MANAGER = true;
    		} else {
    			COMMAND_LINE = true;
    		}
    	} else {
			System.out.println ("Usage: java client <CLIENT_MODE> <trxnType1> <trxnType2> <numberOfClients> <requestTimeLimit> [load] [submit_request_variation] [middleware0] ... [middlewareN]" 
					+ "\nCLIENT_MODE = {part_a; part_b; part_c; lock_manager; default=cmdline}"
					+ "\ntrxnType<N> = {new_customer; book_flight; itinerary}"); 			
			System.exit(1);	
    	}
    	
    	if (TESTING_LOCK_MANAGER) {
    		System.out.println("Testing Lock Manager");
    		LockManager lm = new LockManager();
    		
    		try {
				lm.Lock(1, "pizza", LockManager.READ);
				lm.Lock(1, "ham", LockManager.WRITE);
				
				lm.Lock(1, "ham", LockManager.READ);
				lm.Lock(1, "pizza", LockManager.WRITE);
				
				//lm.Lock(2, "ham", LockManager.READ); - deadlock
				lm.UnlockAll(1);
				lm.Lock(3, "ham", LockManager.READ);
				lm.Lock(4, "ham", LockManager.READ);
				//lm.Lock(4, "ham", LockManager.WRITE); - deadlock

			} catch (DeadlockException e) {
				// TODO Auto-generated catch block
				  
			}
    	} else if (RUNNING_PERMANCE_TEST) {
    		//Determine if input is correct
    	    if (args.length >= 7) {
        		//Run performance tests with ClientPerformanceTest class!
        	    String testType = "";
        	    ClientRequestThread.TransactionType transactionType1;
        	    ClientRequestThread.TransactionType transactionType2;
        	    int load = 0;	//set to -1 when running part a)
        	    int submitRequestVariation = 0;
        	    int numberOfClients = 10;
    		    
    		    testType = args[0];
    		    
    		    transactionType1 = stringToTransactionType(args[1]);
    		    transactionType2 = stringToTransactionType(args[2]);
    		    //
    		    numberOfClients = Integer.parseInt(args[3]);
    		    
    		    int requestTimeLimit = Integer.parseInt(args[4]);

    		    load = Integer.parseInt(args[5]);
    		    submitRequestVariation = Integer.parseInt(args[6]);
    		    
    		    //starting at args[7] to length of cmdline arguments, we consider these the names of the individual middlewares
    		    for (int i = 7; i < args.length; i++) {
    		    	if (args[i] != null) {
    		    		if (args[i].length() > 0) {
    		    			rm_name_list.add(args[i]);
    		    		}
    		    	}
    		    }
    		    
        		System.out.println("Running Performance Tests.");
    		    
        		performanceManager = new ClientPerformanceTest(testType, transactionType1, transactionType2, numberOfClients, requestTimeLimit, load, submitRequestVariation);
        		performanceManager.start();
        		
    	    } else {
    			System.out.println ("Usage: java client <CLIENT_MODE> <trxnType1> <trxnType2> <numberOfClients> <requestTimeLimit> [load] [submit_request_variation] [middleware0] ... [middlewareN]" 
    					+ "\nCLIENT_MODE = {part_a; part_b; part_c; lock_manager; default=cmdline}"
    					+ "\ntrxnType<N> = {new_customer; book_flight; itinerary}"); 
    			System.exit(1); 
    	    }
    	}
    }
	
	public ClientPerformanceTest(String performanceTestType, ClientRequestThread.TransactionType transactionType1, ClientRequestThread.TransactionType transactionType2, int numberOfClients, double requestTimeLimit, int load, 
			int submitRequestVariation) {
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
				ResourceManager rm;

				for (int k = 0; k < rm_name_list.size(); k++) {
					// get a reference to the rmiregistry.
					System.out.println((String)rm_name_list.get(k));
					String elements[] = ((String)rm_name_list.get(k)).split(":");
				     ////
				     if (elements.length != 2) {
				     System.err.println("[rmihost] must be in the format [server:rm_name]");
				     }
				    
				     server = elements[0];
				     rm_name = elements[1];					
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
				}

				
				numberOfThreads = numberOfClients;

				//we use estimated request count to prepare enough data for the test
				//up to a point we assume the estimation is in line with the time, but after that we may be
				//overcompensating...so we adjust to avoid have very long setup times before we actually begin the tests.
				double estimatedRequestCount = (ClientRequestThread.REQUEST_TIME_LIMIT > 20000? ClientRequestThread.REQUEST_TIME_LIMIT/13.3 : ClientRequestThread.REQUEST_TIME_LIMIT/3.5);				
				//
				System.out.println("Creating datasets to handle " + estimatedRequestCount + " request each");
				
				rm =  new RMReplicationManager(rmObjList);
				
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

			ClientRequestThread crt = new ClientRequestThread(transType, rm_name_list, threadDataSet, 0.0, 0, startTime);
			clientThreadTable.add(crt);
			crt.run();
		} else if (performanceTestType.equalsIgnoreCase(PART_CMD_LINE)) {
			System.out.println("Creating Thread #1 - CMD_LINE_PART");
			long startTime = System.nanoTime();

			ClientRequestThread crt = new ClientRequestThread(transType, rm_name_list, threadDataSet, requestInterval, submitRequestVariation, startTime);
			clientThreadTable.add(crt);
			crt.run();
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
				
				
				// Khalique TODO: We need to pass in t
				ClientRequestThread crt = new ClientRequestThread(trxnType, rm_name_list, threadDataSet, requestInterval, submitRequestVariation, startTime);
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
