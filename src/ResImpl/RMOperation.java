package ResImpl;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class RMOperation {
	public Method op;
	public ArrayList<Object> args;
	
	RMOperation(Method op, ArrayList<Object> args) {
		this.op = op;
		this.args = args;
	}
}
