package Sockets;

import java.io.Serializable;

public class Result {
	Address client;
	int id;
	Serializable value;
	
	public Result(Address client, int id, Serializable value)
	{
		this.client = client;
		this.id = id;
		this.value = value;
	}
	
	public boolean matches(Address addr, int id)
	{
		return this.client.equals(addr) && this.id == id;
	}
}
