import java.rmi.*;
public interface FollowerServiceInterface extends Remote
{
	/* registrazione per la callback */
	public void registerForCallback (String username, NotifyEventInterface ClientInterface)
			throws RemoteException;
	
	/* cancella registrazione per la callback */
	public void unregisterForCallback (String username, NotifyEventInterface ClientInterface)
			throws RemoteException; 
}