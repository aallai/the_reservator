package PerformanceTest;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Vector;

import ResInterface.ResourceManager;

	public class ClientRequestThread extends Thread 
	{         
		public static enum TransactionType {
			NEW_CUSTOMER,
			ITINERARY,
			BOOK_FLIGHT,
			VOID
		}
		
		private TransactionType transactionType;
		private int sleepTime;
		private int sleepVariation;
	
		private Vector<Integer> results;
		
	    private ResourceManager rm = null;
	    private int id;
		
		private final int maxReuqestCount = 1000;
		private static int threadCount = 0;
		
		public ClientRequestThread(TransactionType transactionType, String server, String rm_name, int sleepTime, int sleepVariation) {
			this.transactionType = transactionType;
			this.sleepTime = sleepTime;
			this.sleepVariation = sleepVariation;
			
			id = ++threadCount;
			
			try 
			{
				// get a reference to the rmiregistry
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
				// make call on remote method
			} 
			catch (Exception e) 
			{	
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
				
				System.exit(1);
			}
			
			run();
		}
		
		public void run()                       
	    {        
			int i = 0;
			long start, end;
			
			int sum = 0;
			double averageResponseTime;
			
			while (i++ < maxReuqestCount) {
				switch(transactionType) {
				case NEW_CUSTOMER:
					    System.out.println("Adding a new Customer using id: "+id);
					    try{
					    	start = System.currentTimeMillis();
					    	int customer=rm.newCustomer(id);
					    	end = System.currentTimeMillis();
					    	results.add(new Integer((int)(end-start)));
					    	
					    	System.out.println("(" + id + ") new customer id: "+customer + ", time_elapsed: " + (end-start));
					    }
					    catch(Exception e){
					    	System.out.println("EXCEPTION:");
					    	System.out.println(e.getMessage());
					    	e.printStackTrace();
					    }
					
					break;
				case ITINERARY:
					
					break;
					
				case BOOK_FLIGHT:
					break;
				}
			}
		
			//print all values while taking avergae
			System.out.println("(" + id + ") Response Times...");
			for (int j = 0; j < results.size(); i++) {
				sum += results.get(j).intValue();
				System.out.println("(" + id + ") : " + results.get(j).intValue());
			}
			
			averageResponseTime = sum / results.size();
			
			System.out.println("(" + id + ") Average Response Time: " + averageResponseTime);
	    }
	}