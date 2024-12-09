import java.util.ArrayList;
import java.util.List;

public class Post {

	private long id;
	private String titolo;
	private String contenuto;
	private String autore;
	private int rewardIteration;
	private long numero_voti;
	private long like;
	private long dislike;
	private List<String> votanti;
	private List<String> likers;
	private List<String> dislikers;
	private long numero_commenti;
	private List<Comment> commenti;
	private List<String> commentatori;

	//Costruttore che crea un nuovo post con 
	//gli stessi parametri del post passato
	public Post(Post toCopy) {
		
		this.id = toCopy.id;
		this.rewardIteration = toCopy.rewardIteration;
		this.titolo = toCopy.titolo;
		this.contenuto = toCopy.contenuto;
		this.autore = toCopy.autore;
		this.commenti = new ArrayList<Comment>();
		commenti.addAll(toCopy.commenti);
		this.commentatori = new ArrayList<String>();
		commentatori.addAll(toCopy.commentatori);
		this.votanti = new ArrayList<String>();
		votanti.addAll(toCopy.votanti);
		this.likers = new ArrayList<String>();
		likers.addAll(toCopy.likers);
		this.dislikers = new ArrayList<String>();
		dislikers.addAll(toCopy.dislikers);
		this.like = toCopy.like;
		this.dislike = toCopy.dislike;
		this.numero_voti = toCopy.numero_voti;
		this.numero_commenti = toCopy.numero_commenti;
	}
	
	public Post(long id, String titolo, String contenuto, String autore) {
		
		this.id = id;
		this.rewardIteration = 1;
		
		this.titolo = titolo;
		this.contenuto = contenuto;
		this.autore = autore;
		this.commenti = new ArrayList<Comment>();
		this.votanti = new ArrayList<String>();
		this.commentatori = new ArrayList<String>();
		this.likers = new ArrayList<String>();
		this.dislikers = new ArrayList<String>();
		
		this.like = 0;
		this.dislike = 0;
		this.numero_voti = 0;
		this.numero_commenti = 0;
		
		}


	public long getId() {
		return id;
	}


	public String getTitolo() {
		return titolo;
	}


	public String getContenuto() {
		return contenuto;
	}


	public String getAutore() {
		return autore;
	}

	public long getLike() {
		return like;
	}


	public long getDislike() {
		return dislike;
	}

	public long getNumero_commenti() {
		return numero_commenti;
	}


	public List<Comment> getCommenti() {
		return this.commenti;
	}

	//Restituisce 
	//errore  0 -> OK voto registrato
	//errore -4 -> Voto già effettuato
	public synchronized int addVote(int vote, String username) {
		
		if(this.votanti.contains(username))
			return -4;
		else
			this.votanti.add(username);
		
		if(vote > 0) {
			this.like++;
			likers.add(username);
		}
		else {
			this.dislike++;
			dislikers.add(username);
		}
		
		this.numero_voti++;
		
		return 0;
	}


	public List<String> getVotanti() {
		return this.votanti;
	}

	public List<String> getLikers(){
		return this.likers;
	}
	
	public List<String> getDislikers(){
		return this.dislikers;
	}


	public synchronized int addComment(String comment, String autore) {
		
			this.commenti.add(new Comment(comment, autore));
			if(!this.commentatori.contains(autore))
				this.commentatori.add(autore);
			this.numero_commenti++;
		
		return 0;
	}


	public int getRewardIteration() {
		return this.rewardIteration;
	}


	public synchronized void increaseRewardIteration() {
		this.rewardIteration++;
		
	}

	public List<String> getCommenters() {
		return this.commentatori;
	}
	
	public List<String> getVoters() {
		return this.votanti;
	}



	}


