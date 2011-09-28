package Sockets;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Subclass the rmi version so we can reuse the code
 * @author aallaire
 *
 */

public class SocketRm extends  BaseRm
{
	public SocketRm(int port)
	{
		super(port);
	}
	
	public void received(Message m)
	{
		if (actions.containsKey(m.type)) {
			Method act = actions.get(m.type);
			
			try {
				Serializable result = (Serializable) act.invoke(this, m.data.toArray());

				ArrayList<Serializable> data = new ArrayList<Serializable>();
				data.add(result);
				Message r =  new Message(m.from, self, m.id, "result", data);
				com.send(r);
				
			} catch (IllegalArgumentException e) {
				send_error(m.from, m.id, "Wrong parameters for operation: " + m.type);
			} catch (InvocationTargetException e) {
				System.err.println("SocketRm.received() : superclass method " + m.type + " threw ->");
				System.err.println();
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			send_error(m.from, m.id, "Requested unsupported operation: " + m.type);
		}
	}
}
