import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class estimateRewardRunnable implements Runnable {
	
	Winsome winsome;
	long interval;
	int authorPercentage;
	int othersPercentage;
	int MCASTPORT;
	InetAddress group;
	
	public estimateRewardRunnable(Winsome winsome, long interval, int authorPercentage,
			int MCASTPORT, InetAddress group) {
		this.winsome = winsome;
		this.interval = interval;
		this.authorPercentage = authorPercentage;
		this.othersPercentage = 100 - authorPercentage;
		this.MCASTPORT = MCASTPORT;
		this.group = group;
	}

	/**
	 * Calcola le ricompense per tutti i post e invia la notifica
	 * di avvenuto calcolo a tutti gli utenti
	 * tramite il canale multicast
	 * */
	public void run() {
		try {	
			while(true) {
				Thread.sleep(interval);
				List<Post> newPosts = winsome.getAllPosts();
				List<Post> oldPosts = winsome.getLastRewarding();
				
				//Per ogni post già valutato controllo se è anche dentro newPosts
				//per vedere se non è stato cancellato
				for(Post old : oldPosts) {
					long id = old.getId();
					try {
					Post newPost = containsId(newPosts, id).get();
					if(newPost == null)
						continue;	
					double postReward = estimateReward(old, newPost);
					//Se non ci sono stati cambiamenti ignoro il post
					if(postReward == 0)
						continue;
					double authorReward = (postReward/100.0)*(double)authorPercentage;
					double othersReward = postReward - authorReward;
					//Salvo i collaboratori per aggiornare i loro portafogli
					String author = newPost.getAutore();
					List<String> collaborators = new ArrayList<String>();
					collaborators.addAll(newPost.getCommenters());
					collaborators.addAll(newPost.getLikers());
					//Rimuovo chi ha messo dislike
					collaborators.removeAll(newPost.getDislikers());
					//Rimuovo duplicati passando a Set
					Set<String> set = new HashSet<>(collaborators);
					collaborators.clear();
					collaborators.addAll(set);
					double eachReward = othersReward/(double)collaborators.size();
					//Arrotondo a 1 cifra decimale
					eachReward = (double)Math.round(eachReward * 10) / 10;
					authorReward = (double)Math.round(authorReward * 10) / 10;
					
					//Aggiorno i portafogli
					updateWallet(author, authorReward, collaborators, eachReward);
									
					}catch(NullPointerException ex) {
						//Se ottengo questa eccezione significa
						//che il post è stato cancellato
						//mentre tentavo di calcolarne la ricompensa
						//Quindi lo ignoro e vado avanti
						continue;
					}catch(NoSuchElementException ex) {
						//Analogo caso precedente
						continue;
					}
				}
				//Invio notifica multicast		
			     try (DatagramSocket socket = new DatagramSocket()) {
			    	 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			         String message = df.format(
			              new Date(System.currentTimeMillis()));
			         message += " - Nuovo aggiornamento ricompense!\n Controlla il tuo portafoglio";
			         byte[] content = message.getBytes();
			         DatagramPacket packet = 
			        		 new DatagramPacket(content, content.length, group, this.MCASTPORT);
			         // Invio il pacchetto.
			         socket.send(packet);	                   
			        }
			        catch (Exception e) {
			            System.err.println("Errore nella comunicazione multicast: " 
			            		+ e.getMessage());
			        }
				
				//Svuoto e aggiorno la lista dei post con quelli nuovi
				oldPosts.clear();
				for(Post newP : newPosts) {
					//Faccio una copia del post e la aggiungo
					Post p = new Post(newP);
					winsome.putOldPost(p);
				}	
			}
		}catch(InterruptedException ex) {
			return;
		}

	}
	
	/** Aggiorna il portafoglio dell'autore e dei collaboratori 
	 *   @param authorStr L'username dell'autore
	 *   @param authorReward La ricompensa dell'autore
	 *   @param collaborators La lista di username dei collaboratori
	 *   @param eachReward La ricompensa per ogni collaboratore
	 * */
	private void updateWallet(String authorStr, double authorReward,
			List<String> collaborators, double eachReward) {
		
		//Aggiorno portafoglio dell'autore
		User author = winsome.getUser(authorStr);
	if(authorReward != 0)
		author.updateWallet(new Transaction(authorReward));
		//Aggiorno il portafoglio di ogni collaboratore
	if(eachReward != 0) {
		for(String u : collaborators) {
			User c = winsome.getUser(u);
			c.updateWallet(new Transaction(eachReward));
		}	
	}
	}

	/** Calcola la ricompensa per il post confrontando le due versioni
	 * 
	 * @param old La vecchia versione del post
	 * @param newPost Il post aggiornato
	 * 
	 * */
	private double estimateReward(Post old, Post newPost) throws NullPointerException {
				
		//Calcolo la differenza di voti
		long newLikes = newPost.getLike() - old.getLike();
		long newDislikes = newPost.getDislike() - old.getDislike();

		List<Integer> newCommentsCounter = new ArrayList<Integer>();
		List<String> new_people_commenter = new ArrayList<String>();
		//Per ogni commento nel post aggiornato
		for( Comment c : newPost.getCommenti()) {
			//System.out.println(c.toString());
			boolean isNewComment = checkIfNew(old.getCommenti(), c);
			//Controllo se il commento prima non c'era e ora sì
			if(isNewComment) {
				//Se non ho ancora calcolato i commenti totali
				//relativi all'autore del commento
				//mi conto quanti commenti totali ha inserito.
				//Aggiungo il numero ottenuto ad una lista
				String author = c.getAutore();
				if(!new_people_commenter.contains(author)){
					newCommentsCounter.add(countComments(author, newPost.getCommenti()));
					new_people_commenter.add(author);
				}
			}
		}
		double reward =
				rewardFormule(newLikes, newDislikes,newCommentsCounter, newPost.getRewardIteration());		
		newPost.increaseRewardIteration();
		//Approssimo a una cifra decimale
		double roundOff = (double)Math.round(reward * 10) / 10;
		return roundOff;
	}

    /**Controlla se il commento passato come parametro è un
     * nuovo commento rispetto all'iterazione precedente
	 * 
	 * @param oldComments Lista dei commenti all'iterazione precedente
	 * @param newComment Lista dei commenti aggiornata
	 * @return true se il commento è nuovo, false altrimenti
	 */
	private boolean checkIfNew(List<Comment> oldComments, Comment newComment) {
		for(Comment c : oldComments) {
			if(c.equals(newComment))
				return false;
		}
		return true;
	}

	/**Applica la formula del guadagno e restituisce il risultato*
	 **/
	private double rewardFormule(long newLikes, long newDislikes, List<Integer> newCommentsCounter,
			int postIteration) {

		//Calcolo relativo ai voti
		int likeBalance = 0;
		for(int i = 0; i < newLikes; i++)
			likeBalance++;
		for(int i = 0; i < newDislikes; i++)
			likeBalance--;
		if(likeBalance < 0)
			likeBalance = 0;
		
		//Calcolo relativo ai commenti
		double sum = 0;
		for(Integer personComments : newCommentsCounter) {
			sum+= 2/(1+Math.exp(-(personComments-1)));
			//debug print
			//System.out.println("Sum " + personComments + " " + sum);
		}
		//debug print
		//System.out.println("Like balance: " + likeBalance + " | Sum:" + sum + "Post it" + postIteration);
		double reward = (Math.log((double)likeBalance + 1) + Math.log((double)sum + 1))/((double)postIteration);
		return reward;
	}

	/**Conta i commenti totali fatti dall'utente**/
	private int countComments(String autore, List<Comment> commenti) {
		int counter = 0;
		for(Comment c : commenti) {
			if(c.getAutore().equals(autore))
				counter++;
		}
		return counter;
	}

	/**Restituisce il post con quell'id nella lista**/
	public Optional<Post> containsId(final List<Post> list, final long id) throws NullPointerException{
	    return list.stream().filter(o -> o.getId() == id).findFirst();
	}
	
}
