package ResImpl;

import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ResInterface.ResourceManager;

public class ServerRMImpl extends ResourceManagerImpl {
	protected String name;
	
	protected int portNumber;
	protected String hostName;
	
	public static void main(String args[]) {
        // Figure out where server is running
        String server = "localhost";
        String rmName = "reservator_default_rm";
        int port = 1099;
        
         if (args.length == 1) {
             port = 1099;
         } else if (args.length != 3 && args.length != 0) {
             System.err.println ("Wrong usage");
             System.out.println("Usage: java ResImpl.ServerRMImpl [port] [hostname] [rmName]");
             System.exit(1);
         }
		 
         if (args.length != 0) {
        	 port = Integer.parseInt(args[0]);
        	 server = args[1];
        	 rmName = args[2];
        	 System.out.println("Usage: java ResImpl.ServerRMImpl " + args[0] + " " + args[1] + " " + args[2]);
         }
		 try 
		 {
			//Get registry
			//Note: RM must be bound to local registry -- changeback to getRegistry()
			Registry registry = LocateRegistry.getRegistry(server, port);
			 
			System.out.println("Located Registry");
			
			// create a new Server object
			ResourceManager obj = new ServerRMImpl(rmName);
			((ServerRMImpl)obj).setHost(server);			
			((ServerRMImpl)obj).setPort(port);
			
			System.out.println("Created Stub");

			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);
			
			System.out.println("Created Resource Manager ("+server+":"+port+":"+rmName+")");
			
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
