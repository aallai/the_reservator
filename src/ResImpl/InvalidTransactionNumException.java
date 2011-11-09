package ResImpl;

public class InvalidTransactionNumException extends Exception {
	public int tid;
	
	InvalidTransactionNumException(int tid) 
	{
		super(tid +" is not a valid transaction number");
		this.tid = tid;
	}
}
