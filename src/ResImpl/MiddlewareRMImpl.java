package ResImpl;
import ResInterface.*;

import java.util.*;
import java.rmi.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class MiddlewareRMImpl extends ResourceManagerImpl {
	public MiddlewareRMImpl() throws RemoteException {
		super();
	}
	
	public static void main(String args[]) {
        // Figure out where server is running
		int port = -1;
        String server = "";
        String rmName = "";
        ArrayList<String> rmArray = new ArrayList<String>();
        
         if (args.length == 1) {
             server = "localhost" + ":" + args[0];
         } else if (args.length != 0 &&  args.length != 6) {
             System.err.println ("Wrong usage");
             System.out.println("Usage: java ResImpl.ResourceManagerImpl [port] [hostname] [rmname] [...] [...] [...]\n" + 
            		 				"\tWhere [...] represents the active resource managers for middlewear to connect to" + 
            		 				" for cars, hotels, and flights");
             System.exit(1);
         } else {
        	 System.exit(1);
         }
		 
         port = Integer.parseInt(args[0]);
         server = args[1];
         rmName = args[2];
         for (int i = 3; i < args.length; i++) {
        	 rmArray.add(args[i]);
         }
         
		 try 
		 {
			Registry registry = LocateRegistry.getRegistry();
			 
			// create a new Server object
			ResourceManagerImpl obj = new ResourceManagerImpl();
			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			registry.rebind(rmName, rm);

			//Get resource managers
			// get a reference to the rmiregistry
			for (int i = 0; i < rmArray.size(); i++) {
				// get the proxy and the remote reference by rmiregistry lookup
				rm = (ResourceManager) registry.lookup(rmArray.get(i));
				if(rm!=null)
				{
					System.out.println("Connected to RM: " + rmArray.get(i));
				}
				else
				{
					System.out.println("Unsuccessful.  Could not connect to RM: " + rmArray.get(i));
				}
			}
			
			System.out.println("Successfully Connect to all RMs");
			System.err.println("Server ready");
		} catch(AccessException e) {
			System.err.println("Access Remote Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);			
		} catch(RemoteException e) {
			System.err.println("Remote Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} 
		catch (Exception e) 
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);

		}

         // Create and install a security manager
 //        if (System.getSecurityManager() == null) {
 //          System.setSecurityManager(new RMISecurityManager());
 //        }
 //        try {
 //               ResourceManagerImpl obj = new ResourceManagerImpl();
 //               Naming.rebind("rmi://" + server + "/RM", obj);
 //               System.out.println("RM bound");
 //        } 
 //        catch (Exception e) {
 //               System.out.println("RM not bound:" + e);
 //        }
		 }
}
