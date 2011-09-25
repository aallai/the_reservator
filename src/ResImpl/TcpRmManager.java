package ResImpl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class TcpRmManager implements ResInterface.ResourceManager {
	
	Address flight_rm;
	Address car_rm;
	Address room_rm;
	Address itin_rm;
	
	int port;
	
	public static void main(String[] args) {
		// first argument is port to listen on, the rest is a list of active rm in host:port format

		String usage = "Usage : " + args[0] + " server_port rm_host1:port1 rm_host2:port2 ...";
		int port = 0;
		
		try {
			port = Integer.parseInt(args[0]); 
		} catch (NumberFormatException e) {
			System.err.println(usage);
			System.err.println("one");
			System.exit(-1);
		}
		
		ArrayList<Address> active_rms =  new ArrayList<Address>();
		for (int i = 1; i < args.length; i ++) {
			try {
				active_rms.add(new Address(args[i].split(":")[0], Integer.parseInt(args[i].split(":")[1])));
			} catch (Exception e) {
				System.err.println("two");
				System.err.println(usage);
				System.exit(-1);
			}
		}
		
		TcpRmManager manager = new TcpRmManager(port, active_rms);
	}
	
	public TcpRmManager(int port, ArrayList<Address> active_rms) {
		Address[] rms = new Address[4];
		int i = 0;
		
		for (Address a : active_rms) {
			try {
				rms[i] = a;
				i++;
			} catch (IndexOutOfBoundsException e) { 
				System.err.println("TcpRmManager() : Ignored one or more rms passed in to constructor.");
				break;
			}
		}
		
		if (i < rms.length) {
			System.err.println("TcpManager() : Fewer than four active rms prvided.");
			for (; i < rms.length; i++) {
				rms[i] = active_rms.get(0);
			}
		}
		
		this.port = port;
		this.flight_rm = rms[0];
		this.car_rm = rms[1];
		this.room_rm = rms[2];
		this.itin_rm = rms[3];
	}

	@Override
	public boolean addFlight(int id, int flightNum, int flightSeats,
			int flightPrice) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int newCustomer(int id) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean newCustomer(int id, int cid) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCars(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteRooms(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCustomer(int id, int customer) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int queryFlight(int id, int flightNumber) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryCars(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryRooms(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String queryCustomerInfo(int id, int customer)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryCarsPrice(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryRoomsPrice(int id, String location) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean reserveFlight(int id, int customer, int flightNumber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reserveCar(int id, int customer, String location)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reserveRoom(int id, int customer, String locationd)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean itinerary(int id, int customer, Vector flightNumbers,
			String location, boolean Car, boolean Room) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}
}
