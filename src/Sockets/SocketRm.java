package Sockets;

import java.lang.reflect.Method;
import java.util.HashMap;

import ResInterface.*;
import ResImpl.ResourceManagerImpl;

/**
 * Subclass the rmi version so we can reuse the code
 * @author aallaire
 *
 */

public class SocketRm extends  BaseRm implements Callback
{
	private Address self;
	private Communicator com;
	private HashMap<String, Method> actions;
	
	public SocketRm()
	{
		
	}
	
	private HashMap<String, Method> init_actions() 
	{
		HashMap<String, Method> ret = new HashMap<String, Method>();
		
		Method[] methods = this.getClass().getDeclaredMethods();
		for (Method m : methods) {
			ret.put(m.getName(), m);
		}
		return ret;
	}
	
	public void received(Message m)
	{
		
	}
}
