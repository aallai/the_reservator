package ResImpl;

import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ResInterface.ResourceManager;

public class ServerRMImpl extends ResourceManagerImpl {
	public static void main(String args[]) {
        // Figure out where server is running
        String server = "";
        String rmName = "";
        
        
         if (args.length == 1) {
             server += "localhost" + ":" + args[0];
         } else if (args.length != 0 &&  args.length != 3) {
             System.err.println ("Wrong usage");
             System.out.println("Usage: java ResImpl.ServerRMImpl [port] [hostname] [rmName]");
             System.exit(1);
         }
		 
         rmName = args[2];
         System.out.println("Usage: java ResImpl.ServerRMImpl " + args[0] + " " + args[1] + " " + args[2]);

         
         
		 try 
		 {
			 
			//Get registry
			Registry registry = LocateRegistry.getRegistry();
			 
			System.out.println("Located Registry");
			
			// create a new Server object
			ResourceManager obj = new ServerRMImpl();
			
			System.out.println("Created Stub");

			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);
			
			System.out.println("Created Resource Manager");

			
			// Bind the remote object's stub in the registry
			registry.rebind(rmName, rm);
			
			System.out.println("Binded Stub to Registry");

			
			

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

    
    public ServerRMImpl() throws RemoteException {
    	super();
    }
}
