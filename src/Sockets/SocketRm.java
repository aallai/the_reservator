package Sockets;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Subclass the rmi version so we can reuse the code
 * @author aallaire
 *
 */

public class SocketRm extends  BaseRm
{
	public static void main(String[] args)
	{
		int port = 1337;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.err.println("Usage : SocketRm port");
			System.exit(-1);
		}
		
		SocketRm rm = new SocketRm(port);
		rm.run();
		
		System.out.println("Server ready.");
	}
	
	public SocketRm(int port)
	{
		super(port);
	}
	
	public void received(Message m)
	{
			
		Method act = get_op(m.type);

		if (act != null) {
			try {
				Serializable result = (Serializable) act.invoke(this, m.data.toArray());

				ArrayList<Serializable> data = new ArrayList<Serializable>();
				data.add(result);
				Message r =  new Message(m.from, self, m.client, m.id, "result", data);
				com.send(r);
				
			} catch (IllegalArgumentException e) {
				send_error(m.from, m.client, m.id, "Wrong parameters for operation: " + m.type);
			} catch (InvocationTargetException e) {
				System.err.println("SocketRm.received() : superclass method " + m.type + " threw ->");
				System.err.println();
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			send_error(m.from, m.client, m.id, "Requested unsupported operation: " + m.type);
		}
	}
	

	private Method get_op(String type)
	{
		Method ret = null;
		
		for (Method m : super.getClass().getMethods()) {
			if (m.getName().equalsIgnoreCase(type)) {
				ret = m;
			}
		}
		
		return ret;
	}

	@Override
	public int startTransaction() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean commitTransaction(int tid) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean abortTransaction(int tid) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}
}
