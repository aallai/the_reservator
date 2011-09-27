package ResInterface;

import ResImpl.Message;

public interface Callback {
	/**
	 * This gets executed by threads in the communicator class when a message is received,
	 * possibly concurrently by many of them (a new thread is spawned to execute the callback
	 * as soon as a message is received).
	 * @param msg
	 */
	public void received(Message msg);
}
