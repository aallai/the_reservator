package PerformanceTest;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Vector;

import ResInterface.ResourceManager;

import PerformanceTest.ClientRequestThread;
import PerformanceTest.ClientRequestThread.TransactionType;

public class ClientPerformanceTest {
	String server;
	String rm_name;
	ClientRequestThread.TransactionType transactionType1;
	ClientRequestThread.TransactionType transactionType2;
	int load;
	int submitRequestVariation;
	
	public static String PART_A = "part_a";
	public static String PART_B = "part_b";
	
	public int numberOfThreads = 1;
	private String performanceTestType;
	
	private Vector<ClientRequestThread> clientThreadTable; 
	
	public ClientPerformanceTest(String performanceTestType, String server, String rm_name, ClientRequestThread.TransactionType transactionType1, ClientRequestThread.TransactionType transactionType2, int load, 
			int submitRequestVariation) {
		this.server = server;
		this.rm_name = rm_name;
		this.transactionType1 = transactionType1;
		this.transactionType2 = transactionType2;
		this.load = load;
		this.submitRequestVariation = submitRequestVariation;
		this.performanceTestType = performanceTestType;
		if (performanceTestType.equalsIgnoreCase(PART_A)) {
			setupThreads(transactionType1, numberOfThreads);
		} else if (performanceTestType.equalsIgnoreCase(PART_B)) {
			setupThreads(transactionType1, numberOfThreads);
			setupThreads(transactionType2, numberOfThreads);
		}
	}
	
	private void setupThreads(ClientRequestThread.TransactionType transType, int numOfThreads) {
		for (int i = 0; i < numOfThreads; i++) {
			if (performanceTestType.equalsIgnoreCase(PART_A)) {
				ClientRequestThread crt = new ClientRequestThread(transType, server, rm_name, 0, 0);
				clientThreadTable.add(crt);
				crt.run();
			} else if (performanceTestType.equalsIgnoreCase(PART_B)) {
				
			}
		}
	}
}
