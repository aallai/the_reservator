package ResImpl;

import ResInterface.Callback;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.*;

public class Communicator {
	
	Callback cb;
	int port;
	ServerSocket in;
	
	public Communicator(int port, Callback cb) {
		this.port = port;
		this.cb = cb;
		
		try {
			in = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Communicator() : Could not open local port " + port);
			System.err.println("Shutting down");
			System.exit(-1);
		}
		
		
	}
}
