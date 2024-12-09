import java.rmi.*;
import java.rmi.server.*;
import java.util.List;
	public class NotifyEventImpl extends RemoteObject 
		implements NotifyEventInterface {
		
		/*warning suppress*/
		private static final long serialVersionUID = 1L;
		List<String> myFollowers;
		
		/* crea un nuovo callback client */
		public NotifyEventImpl(List<String> myFollowers ) throws RemoteException
		{ 	
			super( );
			this.myFollowers = myFollowers;
			 }
		
		/* metodo che può essere richiamato dal servente per notificare un nuovo follower*/
		public void notifyEvent(String Follower) throws RemoteException {
			String action = "";
			if(myFollowers.contains(Follower))
				action = "unfollow";
			else
				action = "follow";
			
		if(action.equals("follow"))	{
			String returnMessage = Follower + " ha iniziato a seguirti!";
			myFollowers.add(Follower);
			System.out.println(returnMessage);
			}
		else {		
			String returnMessage = Follower + " ha smesso di seguirti! :(";
			myFollowers.remove(Follower);
			System.out.println(returnMessage);
			}
		}
	}