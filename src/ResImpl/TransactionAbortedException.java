package ResImpl;

public class TransactionAbortedException extends Exception {
	public int tid;
	String reason;
	
	TransactionAbortedException(int tid, String reason)
	{
		super("Transaction " + tid + " aborted. Reason: " + reason);
		this.tid = tid;
		this.reason = reason;
	}
}
