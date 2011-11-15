package ResImpl;

import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ResInterface.ResourceManager;
//
public class ServerRMImpl extends ResourceManagerImpl {
	protected String name;
	
	protected int portNumber;
	protected String hostName;
	
	public static void main(String args[]) {
        // Figure out where server is running
        String rmName = "";
        
       
         if (args.length != 1) {
             System.err.println ("Wrong usage");
             System.out.println("Usage: java ResImpl.ServerRMImpl [rmName]");
             System.exit(1);
         }
		 
         rmName = args[0];

		 try 
		 {
			Registry registry = LocateRegistry.getRegistry();
			 
			System.out.println("Located Registry");
			
			// create a new Server object
			ResourceManager obj = new ServerRMImpl(rmName);
			
			System.out.println("Created Stub");

			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);
			
			System.out.println("Created Resource Manager ()");
			
			// Bind the remote object's stub in the registry
			//Note: for registry any host but the localhost will draw an exception here.
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
    }

    
    public ServerRMImpl(String rmName) throws RemoteException {
    	super();
    	name = rmName;
    }
    
	public void setPort(int port) {
		portNumber = port;
	}
	
	public void setHost(String host) {
		hostName = host;
	}
}
