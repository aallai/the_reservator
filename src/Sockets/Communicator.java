package Sockets;

import ResInterface.Callback;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This class handles message passing between the rms, clients and rm manager.
 * @author aallaire
 *
 */

public class Communicator 
{
	
	Callback cb;
	int port;
	ServerSocket in;
	
	public Communicator(int port, Callback cb) 
	{
		this.port = port;
		this.cb = cb;
	}

	public boolean init() 
	{
		try {
			in = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Communicator.init() : Could not open local port " + port);
			return false;
		}
		
		new Thread() 
		{
			public void run() {
				listen();
			}
		}.start();
		
		return true;
	}
	
	/**
	 * A thread executing this method will loop forever until a message arrives, then it will
	 * spawn off another thread to execute the callback.
	 */
	private void listen() 
	{
		while (true) {
			final Socket sock;
			try {
				sock = in.accept();

				new Thread() 
				{
					public void run() {
						receive(sock);
					}
				}.start();
			
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}		
		}
	}
	
	private void receive(Socket sock) 
	{

		try {
			ObjectInputStream reader = new ObjectInputStream(sock.getInputStream());
			final Message m = (Message) reader.readObject();
			reader.close();
			sock.close();
			
			cb.received(m);
		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public boolean send(Message m) 
	{
		Socket sock;
		
		try {
			sock = new Socket(m.to.host, m.to.port);
		} catch (UnknownHostException e) {
			System.err.println("Communicator.send() : Unkown host " + m.to.host);
			return false;
		} catch (IOException e) {
			System.err.println("Communicator.send() : Cannot connect to " + m.to.toString());
			return false;
		}
		
		try {
			ObjectOutputStream writer = new ObjectOutputStream(sock.getOutputStream());
			writer.writeObject(m);
			writer.close();
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
