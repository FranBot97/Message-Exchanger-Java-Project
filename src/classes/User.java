import java.util.ArrayList;
import java.util.List;

public class User {

	private String username;
	private List<Long> blog;
	private List<String> tags;
	private List<String> following;
	private List<String> followers;
	Wallet wallet;
	
	public User(String username, List<String> tags) {
		this.username = username;
		this.tags = new ArrayList<String>();
			
		for(String tag : tags) {
			if(!this.tags.contains(tag)) this.tags.add(tag);
		}
		this.following = new ArrayList<String>();
		this.followers = new ArrayList<String>();
		this.blog = new ArrayList<Long>();
		this.wallet = new Wallet();
	}
	
	public String getUsername() {
		return username;
	}
	
	public List<String> getTags(){
		return tags;
	}

	public synchronized void addPost(Post p) {
		this.blog.add(p.getId());
	}
	
	public synchronized void addFollower(String newFollower) {
		this.followers.add(newFollower);
	}
	
	public synchronized void removeFollower(String oldFollower) {
		this.followers.remove(oldFollower);
	}
	
	public List<String> getFollowing() {
		return this.following;
	}

	public synchronized void addFollowing(String usernameToFollow) {
	if(this.following.contains(usernameToFollow)) {
		throw new IllegalArgumentException();
	} else {
		this.following.add(usernameToFollow);
		}
		
	}

	public synchronized void removeFollowing(String usernameToUnfollow) {
		if(!this.following.contains(usernameToUnfollow)) {
			throw new IllegalArgumentException();
		} else {
			this.following.remove(usernameToUnfollow);
			}
	}
	
	public List<Long> getBlog(){
		return this.blog;
	}

	public synchronized void removePost(Long idPost) {
		this.blog.remove(idPost);
		
	}

	public List<String> getFollowers() {
		return this.followers;
	}

	public void updateWallet(Transaction t) {
		this.wallet.updateWallet(t);
	}

	public Wallet getWallet() {
		return this.wallet;
		
	}

	
}
