package ResImpl;

import java.util.Stack;

public class RMTransaction {
	int tid;   //transaction id
	Stack<RMOperation> undo_stack;
	
	RMTransaction(int tid) {
		this.tid = tid;
		this.undo_stack = new Stack<RMOperation>();
	}
}
