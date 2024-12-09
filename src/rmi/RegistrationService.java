import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
	
public interface RegistrationService extends Remote {

		public long register(String username, String password,  List<String> tags)
				throws  UsernameAlreadyInUseException, RemoteException, NoSuchAlgorithmException, IllegalArgumentException;
		
	}