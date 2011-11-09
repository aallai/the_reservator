package ResImpl;
import java.util.Hashtable;
import ResInterface.ResourceManager;
import java.util.Timer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MiddlewareTransaction {

	static long T_TIMEOUT = 30000;
	
	int tid;
	Timer timer;
	Hashtable<ResourceManager, Integer> rm_table;   // RM -> local tid on RM
	ReentrantReadWriteLock lock;
	
	
	MiddlewareTransaction(int tid) 
	{
		this.lock = new ReentrantReadWriteLock();
		this.tid = tid;
		this.rm_table = new Hashtable<ResourceManager, Integer>();
	}
}
