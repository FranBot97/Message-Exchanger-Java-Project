import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistrationServiceImpl implements RegistrationService {

	Winsome social;
	
	public RegistrationServiceImpl(Winsome social) {
		this.social = social;
	}
	
	public long register(String username, String password,  List<String> tags)
			throws UsernameAlreadyInUseException, RemoteException, NoSuchAlgorithmException, IllegalArgumentException {
	
		if(username == null || tags == null || password == null)
			throw new IllegalArgumentException("Gli argomenti non possono essere <null>");
		
		if(username.contains(" ") || username.equals("")) 
			throw new IllegalArgumentException("Il nome utente non pu� contenere spazi");
		
		Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(username);
		boolean b = m.find();
		if (b)
		   throw new IllegalArgumentException("Il nome utente pu� contenere solo lettere e numeri");
		
		if(password.trim().isEmpty())
			throw new IllegalArgumentException("La password non pu� essere vuota");
			
		if(social.getUser(username) != null) {
			throw new UsernameAlreadyInUseException("Il nome utente <"+
					username+
					"> risulta gi� in uso, riprovare");
		}else {
			User nuovoUtente = new User(username, tags);
			social.addUtente(nuovoUtente); //synchronized
			social.putUserPassword(nuovoUtente.getUsername(), password);
			return 0;
		} 
	
		
	}
}
