import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Winsome {

	private List<User> utenti;
	private List<Post> posts;
	//Post aggiornati all'ultimo calcolo della ricompensa
	private List<Post> lastRewarding;
	private ConcurrentHashMap<String, String> encryptedPassword;
	private long progressivePostID;
	
	public Winsome() {
		this.utenti = new ArrayList<User>();
		this.posts = new ArrayList<Post>();
		this.encryptedPassword = new ConcurrentHashMap<String, String>();
		this.progressivePostID = 0;
		this.lastRewarding = new ArrayList<Post>();
	}

		
	public User getUser(String username) {
		for(User u : utenti) {
			if(u.getUsername().equals(username))
				return u;
		}
		return null;
	}
	
	public synchronized void addUtente(User nuovoUtente) {
		utenti.add(nuovoUtente);
		
	}

	public synchronized void putUserPassword(String nuovoUtente, String bytesToHex) {
		encryptedPassword.put(nuovoUtente, bytesToHex);
	}
	
	public int checkLogin(String u, String password) {
		String storedPassword = this.encryptedPassword.get(u);
		if(storedPassword.equals(password))
			return 0;
		else
			return -1;
	}

	public List<User> getAllUser(){
		return utenti;
	}

	public synchronized void addPost(Post p) {
		this.posts.add(p);
	}
	
	public synchronized long putPost(String username, String titolo, String contenuto) {
		
		Post p = new Post( progressivePostID++, 
						   titolo, 
						   contenuto, 
						  username);
		try {	posts.add(p);
				User u = getUser(username);
				u.addPost(p);	
		}catch(Exception ex) {
			return -1;
			}
		return p.getId();
	}

	//Restituisce lista utenti seguiti da "username"
	public List<String> getFollowing(String username) {
		return this.getUser(username).getFollowing();
	}

	public Post getPost(long idPost) {
		for(Post p : posts) {
			if(p.getId() == idPost)
				return p;
		} 
			return null;
	}

	public synchronized void removePost(long idPost) {
		Post toRemove = this.getPost(idPost);
		this.posts.remove(toRemove);
		this.getUser(toRemove.getAutore()).removePost(idPost);
	}

	public List<Post> getAllPosts() {
		return this.posts;
	}

	public ConcurrentHashMap<String, String> getPasswords() {
		return this.encryptedPassword;
		
	}

	public long getProgressiveID() {
		return this.progressivePostID;
	}

	public void setPasswords(ConcurrentHashMap<String, String> passwords) {
		this.encryptedPassword = passwords;
		
	}

	public void setProgressiveID(long idPost) {
		this.progressivePostID = idPost;
		
	}

	public List<Post> getLastRewarding() {
		return lastRewarding;
	}

	public synchronized void addOldPost(Post postToAdd) {
		this.lastRewarding.add(postToAdd);
	}
	
	public synchronized long putOldPost(Post p) {
		try {	lastRewarding.add(p);
				
		}catch(Exception ex) {
		return -1;
		}
		return p.getId();
	}
}