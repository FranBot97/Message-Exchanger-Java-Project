import java.rmi.*; 
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FollowerServiceImpl extends RemoteObject implements FollowerServiceInterface
{
	/*warning suppress*/
	private static final long serialVersionUID = 1L;
	
	/* lista dei client registrati */
	private ConcurrentHashMap<String, List<NotifyEventInterface>> clients;

	/* crea un nuovo service */
	public FollowerServiceImpl()throws RemoteException{
		super( );
		clients = new ConcurrentHashMap<String, List<NotifyEventInterface>>( );
	};

	/* registra un nuovo client per il callback*/
	public synchronized void registerForCallback (String username, NotifyEventInterface ClientInterface)
			throws RemoteException
	{	if (!clients.containsKey(username)) {
		clients.put(username, new ArrayList<NotifyEventInterface>());
		}
		clients.get(username).add(ClientInterface);
		//System.out.println("New client registered." );
		
	};
	
	/* annulla registrazione per il callback */
	public synchronized void unregisterForCallback (String username, NotifyEventInterface Client)
			throws RemoteException
	{	
		if (clients.get(username).remove(Client)){
				//System.out.println("Client unregistered");
		}
		else {
				//System.out.println("Unable to unregister client.");
			}
	}
	
	/* Notifica nuovo follower o unfollower
	 * Aggiorna l'elenco locale dei followers*/
	public void update(String username, String newFollower) throws RemoteException
	{
		doCallbacks(username, newFollower);
	};
	
	private synchronized void doCallbacks(String username, String Follower)
			throws RemoteException
	{ 
		//System.out.println("Starting callbacks.");
		for (Map.Entry<String, List<NotifyEventInterface> > entry : clients.entrySet()){
		    String user = entry.getKey();
		    List<NotifyEventInterface> activeClients = entry.getValue();
		    if(user.equals(username)) {
		    	for(NotifyEventInterface c : activeClients) {
		    		c.notifyEvent(Follower);
		    	}
		    }
		}
		
	//System.out.println("Callbacks complete.");
	}
	}