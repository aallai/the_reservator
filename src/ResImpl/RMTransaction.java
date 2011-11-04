package ResImpl;

import java.util.ArrayList;

public class RMTransaction {
	int tid;   //transaction id
	ArrayList<RMOperation> undo_set;
	
	RMTransaction(int tid) {
		this.tid = tid;
		this.undo_set = new ArrayList<RMOperation>();
	}
}
