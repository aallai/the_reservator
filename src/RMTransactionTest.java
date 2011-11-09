
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
		
	
			try {
				
				int tid = rm.startTransaction();
				rm.newCustomer(tid, 1337);
				rm.commitTransaction(tid);
				
				
				tid = rm.startTransaction();
		
				rm.addCars(tid, "HILTON", 10, 50);
				rm.addFlight(tid, 1, 10, 50);
				
				rm.commitTransaction(tid);
				
				new Thread() 
				{
					public void run() 
					{
						try {
							int tid = rm.startTransaction();
							rm.reserveCar(tid, 1337, "HILTON");
							rm.reserveFlight(tid, 1337, 1);
							rm.commitTransaction(tid);
							
						} catch (RemoteException e) {
							e.printStackTrace();
						} catch (TransactionAbortedException e) {
							e.printStackTrace();
						} catch (InvalidTransactionNumException e) {
							e.printStackTrace();
						} 
					}
				}.start();
				
				tid = rm.startTransaction();
				System.out.println(rm.queryCustomerInfo(tid, 1337));
				System.out.println(rm.queryCarsPrice(tid, "Hilton"));
				System.out.println(rm.queryCustomerInfo(tid, 1337));
				rm.deleteCustomer(tid, 1337);
				
				rm.commitTransaction(tid);
			
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (TransactionAbortedException e) {
				e.printStackTrace();
			} catch (InvalidTransactionNumException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}			
}
