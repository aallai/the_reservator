
import java.rmi.RemoteException;
import java.lang.Thread;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ResImpl.InvalidTransactionNumException;
import ResImpl.TransactionAbortedException;
import ResInterface.*;

public class RMTransactionTest {
	public static void main(String[] args) {
		
		
		try {
		
			Registry registry = LocateRegistry.getRegistry("mimi.cs.mcgill.ca");
			final ResourceManager rm = (ResourceManager) registry.lookup("aallai_rm");
		
	
			
				
			int tid = rm.startTransaction();
			System.out.println("One : " + tid);
			rm.newCustomer(tid, 1337);
			rm.commitTransaction(tid);
				
				
			tid = rm.startTransaction();
			System.out.println("Two : " + tid);
			rm.addCars(tid, "HILTON", 10, 50);
			rm.addFlight(tid, 1, 10, 50);
				
			rm.commitTransaction(tid);
				
			new Thread() 
			{
				public void run() 
				{
					try {
						int tid = rm.startTransaction();
						System.out.println("Three : " + tid);
						rm.reserveCar(tid, 1337, "HILTON");
						rm.reserveFlight(tid, 1337, 1);
						rm.queryCustomerInfo(tid, 1337);
						rm.commitTransaction(tid);
							
					} catch (RemoteException e) {
						System.out.println(e.getMessage());
					} catch (TransactionAbortedException e) {
						System.out.println(e.getMessage());
					} catch (InvalidTransactionNumException e) {
						System.out.println(e.getMessage());
					} 
				}
			}.start();
				
			try {
				tid = rm.startTransaction();
				System.out.println("Four : " + tid);
				rm.reserveFlight(tid, 1337, 1);
				System.out.println(rm.queryCarsPrice(tid, "Hilton"));
				System.out.println(rm.queryFlightPrice(tid, 1337));
				rm.deleteCustomer(tid, 1337);
				rm.commitTransaction(tid);
			} catch (TransactionAbortedException e) {
				System.out.println(e.getMessage());
			}
			
			try {
				tid = rm.startTransaction();
				System.out.println("Five : " + tid);
				Thread.sleep(60000);
				rm.queryCars(tid, "HILTON");
			} catch (InvalidTransactionNumException e) {
				System.out.println(e.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}			
}
