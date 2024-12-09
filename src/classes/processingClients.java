import java.nio.channels.SelectionKey;
import java.util.HashSet;
import java.util.Set;

public class processingClients {

	private Set<SelectionKey> inProcess;
	
	public processingClients() {
		inProcess = new HashSet<SelectionKey>();
	}
	
	public synchronized void addKey(SelectionKey key) {
		inProcess.add(key);
	}
	
	public synchronized void removeKey(SelectionKey key) {
		if(inProcess.contains(key)) inProcess.remove(key);
	}
	
	public boolean contains(SelectionKey key) {
		if(inProcess.contains(key))
			return true;
		else 
			return false;
	}
}
