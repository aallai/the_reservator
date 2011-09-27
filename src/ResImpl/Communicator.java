package ResImpl;

import ResInterface.Callback;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Communicator {
	
	Callback cb;
	int port;
	ServerSocket in;
	
	public Communicator(int port, Callback cb) {
		this.port = port;
		this.cb = cb;
	}

	public boolean init() {
		try {
			in = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Communicator() : Could not open local port " + port);
			return false;
		}
		
		new Thread() {
			public void run() {
				listen();
			}
		}.start();
		
		return true;
	}
	
	public void listen() {
		while (true) {
			Socket sock;
			try {
				sock = in.accept();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			
			try {
				ObjectInputStream reader = new ObjectInputStream(sock.getInputStream());
				final Message m = (Message) reader.readObject();
				reader.close();
				sock.close();
				
				new Thread() {
					public void run() {
						cb.received(m);
					}
				}.start();
				
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	public boolean send(Message m) {
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
