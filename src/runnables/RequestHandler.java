import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.gson.Gson;

public class RequestHandler implements Runnable {
	//Dimensioni per buffer NIO
	public static int BUFFER_SIZE = 200;
	public static int SMALL_BUFFER = 4;
	public static int MEDIUM_BUFFER = 516;
	public static int LARGE_BUFFER = 1024;
	
	Winsome social;
	//array contenente la richiesta del client
	byte[] data;
	//Canale del client con cui comunicare
	SocketChannel client;
	SelectionKey key;
	//Username del client gestito
	String myUsername;
	//Oggetto condiviso dei client attualmente serviti
	processingClients inProgress;
	//callback notifica follower
	FollowerServiceImpl callback;
	
	public RequestHandler(Winsome social, byte[] data, SelectionKey key,
			processingClients inProgress,FollowerServiceImpl callback) {
		this.social = social;
		this.data = data;
		this.client =(SocketChannel)key.channel();
		this.key = key;
		this.inProgress = inProgress;
		this.callback = callback;
	}
	
	/**
	 * Funzione che invia sul canale del client tutto
	 * il contenuto dentro il buffer
	 * 
	 * @param buffer Il buffer contenente i byte da spedire
	 * @param client Canale del client
	 * **/
	public void send(ByteBuffer buffer, SocketChannel client) 
			throws IOException {
		buffer.flip();
		do {
			client.write(buffer);
		}while(buffer.hasRemaining());
	}

	/**
	 * Task che gestisce tutta la richiesta del client,
	 * analizza la stringa ricevuta, gestisce la richiesta
	 * e invia la risposta al client.
	 * **/
	public void run() {
		//System.out.println(Thread.currentThread().getName());
			
		String receivedData = new String(data);
		//Poichè ho ricevuto una stringa con argomenti
		//separati da virgole eseguo lo split
		//per analizzarla
		String[] tokens = receivedData.toString().split(",");
	
		if(tokens[0].equals("login")) {
			String username = tokens[1];
			String password = tokens[2];
			login(username, password);
		}
		else if(tokens[0].equals("listUsers")) {
			this.myUsername = tokens[1];
			if (listUsers() == -1)
				System.out.println("listUsers fallita");
		}
		else if(tokens[0].equals("listFollowing")) {
			this.myUsername = tokens[1];
			listFollowing();
		}
		else if(tokens[0].equals("follow")) {
			this.myUsername = tokens[1];
			followUser(tokens[2]);
		}
		else if(tokens[0].equals("unfollow")) {
			this.myUsername = tokens[1];
			unfollowUser(tokens[2]);
		}
		else if(tokens[0].equals("viewBlog")) {
			this.myUsername = tokens[1];
			viewBlog();
		}		
		else if(tokens[0].equals("post")) {
			this.myUsername = tokens[1];
			//Dato che un post può contenere ","
			//non posso fare lo split come per gli altri casi.
			//Creo un buffer per gestire meglio i dati ricevuti
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			buffer.put(data);
			buffer.flip();
			//Vado subito alla posizione che mi interessa
			int newPosition = "post,".length() + myUsername.length() + 1;
			buffer.position(newPosition);
			//I prossimi byte sono un intero che
			//rappresenta la lunghezza del titolo
			int titleSize = buffer.getInt();
			byte[] titoloByte = new byte[titleSize];
			buffer.get(titoloByte);
			//I restanti byte sono il contenuto del post
			byte[] contenutoByte = new byte[buffer.limit() - buffer.position()];
			buffer.get(contenutoByte);
			String titolo = new String(titoloByte);
			String contenuto = new String(contenutoByte);
			createPost(titolo, contenuto);
		}
		else if(tokens[0].equals("showFeed")) {
			this.myUsername = tokens[1];
			showFeed();
		}
		else if(tokens[0].equals("showPost")) {
			this.myUsername = tokens[1];
			showPost(tokens[2]);
		}
		else if(tokens[0].equals("deletePost")) {
			this.myUsername = tokens[1];
			deletePost(tokens[2]);
		}
		else if(tokens[0].equals("rewin")){
			this.myUsername = tokens[1];
			rewinPost(tokens[2]);
		}
		else if(tokens[0].equals("rate")) {
			this.myUsername = tokens[1];
			ratePost(tokens[2], tokens[3]);
		}
		else if(tokens[0].equals("comment")){
			this.myUsername = tokens[1];
			String idPost = tokens[2];
			//Dato che un commento può contenere "," 
			//non posso fare un semplice split come negli altri casi
			//Eseguo una gestione tramite buffer
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			buffer.put(data);
			buffer.flip();
			//Vado subito alla posizione che mi interessa
			int newPosition = "comment,".length() + myUsername.length() + 1 + idPost.length() + 1; 
			buffer.position(newPosition);
			//I prossimi byte sono un intero che
			//rappresenta la lunghezza del commento
			int commentSize = buffer.getInt();
			byte[] commentByte = new byte[commentSize];
			buffer.get(commentByte);
			String commento = new String(commentByte);
			addComment(idPost, commento);
			
		}
		else if(tokens[0].equals("wallet")) {
			this.myUsername = tokens[1];
			getWallet();
		}
		else if(tokens[0].equals("walletBtc")) {
			this.myUsername = tokens[1];
			getWalletBtc();
		}
	//Rimuovo il client da quelli serviti prima di terminare
	inProgress.removeKey(key);
	return;
	}
	
	private int getWalletBtc() {
		try {
			User me = social.getUser(myUsername);
			double total = me.getWallet().getAmount();
			double totalBtc = 0;
			/**Calcolo valore in btc tramite RANDOM.ORG**/
			URL url = new URL("https://www.random.org/decimal-fractions/?num=1&dec=1&col=1&format=plain&rnd=new");
			InputStream uin = url.openStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(uin));
			String line;
			while ((line = in.readLine()) != null) {
				totalBtc = Double.parseDouble(line)*total;;
			}
			totalBtc = (double)Math.round(totalBtc * 10) / 10;
			//Invio al client
			ByteBuffer sendBuffer = ByteBuffer.allocate(8);
			sendBuffer.clear();
			sendBuffer.putDouble(totalBtc);
			send(sendBuffer, client);
	
		}catch(IOException ex) {
			return -1;
		}
		return 0;
	}

	private int getWallet() {
		try {	User me = social.getUser(myUsername);
				Wallet wallet = me.getWallet();
				double saldo = wallet.getAmount();
				ByteBuffer sendBuffer = ByteBuffer.allocate(BUFFER_SIZE);
				//Aggiungo il saldo al buffer
				sendBuffer.clear();
				sendBuffer.putDouble(saldo);
							
				List<Transaction> list = wallet.getTransactions();
				//Aggiungo il numero di transazioni
				int size = list.size();
				sendBuffer.putInt(size);
				//Invio i due dati al client
				send(sendBuffer, client);
				//Invio una alla volta le transazioni
				for(Transaction t : list) {
					sendBuffer.clear();
					sendBuffer.putDouble(t.getValue());
					sendBuffer.put(t.getTimestamp().getBytes());
					send(sendBuffer, client);		
					}
				}catch(IOException e) {
	return -1;
}
		return 0;
	}
	
	

	private int addComment(String idPostStr, String commento) {
		//errore  0 -> OK 
		//errore -1 -> il post non esiste
		//errore -2 -> il post esiste ma è un proprio post
		//errore -3 -> il post esiste ma non è nel feed
		long idPost = Long.parseLong(idPostStr);
		Post p = social.getPost(idPost);
		User me = social.getUser(myUsername);
		int error = -1;
		
		//Se il post esiste
		if(p != null) {		
			//Controllo se l'autore del post è tra gli utenti che seguo
			String autore = p.getAutore();
			if (me.getFollowing().contains(autore))
				error = p.addComment(commento, myUsername);
			else {
				//Controllo se il post è dell'utente che commenta
				if(myUsername.equals(autore))
					error = -2;
				else //altrimenti non è nel feed
					error = -3;
			}				
		}	
		ByteBuffer sendBuffer = ByteBuffer.allocate(4);
		sendBuffer.clear();
		sendBuffer.putInt(error);
		try {
			send(sendBuffer, client);
		}catch(IOException ex) {
			return -1;
		}	
		return 0;
	}

	private int ratePost(String idPostStr, String votoStr) {
		
		//errore  0 -> OK il post esiste ed è nel feed
		//errore -1 -> il post non esiste
		//errore -2 -> il post esiste ma è un proprio post
		//errore -3 -> il post esiste ma non è nel feed
		//errore -4 -> Voto già effettuato

		long idPost = Long.parseLong(idPostStr);
		int voto = Integer.parseInt(votoStr);
		Post p = social.getPost(idPost);
		User me = social.getUser(myUsername);
		int error = -1;
		
		//Se il post esiste
		if(p != null) {		
			//Controllo se l'autore del post è tra gli utenti che seguo
			String autore = p.getAutore();
			if (me.getFollowing().contains(autore))
				error = p.addVote(voto, myUsername);
			else {
				//Controllo se il post è dell'utente che ha richiesto il voto
				if(myUsername.equals(autore))
					error = -2;
				else //altrimenti non è nel feed
					error = -3;
			}		
		}
		ByteBuffer sendBuffer = ByteBuffer.allocate(4);
		sendBuffer.clear();
		sendBuffer.putInt(error);
		try {
			send(sendBuffer, client);
		}catch(IOException ex) {
			return -1;
		}	
		return 0;
	}

	public int login(String username, String password) {
		
		String responseMessage = "";
		ByteBuffer response = ByteBuffer.allocate(BUFFER_SIZE);
		User u = social.getUser(username);
		
		//Se l'utente non è stato trovato
		if(u == null) {
			responseMessage+="Utente <"+username+"> inesistente. Riprovare";
			int messageSize = responseMessage.length();
			response.clear();
			response.putInt(messageSize);
			response.put(responseMessage.getBytes());
			try {
				send(response, client);
			} catch (IOException e) {
				return -1;
			}
			return -1;
		}
		 //Controllo se la password memorizzata corrisponde 
		 //a quella inserita
		if (social.checkLogin(u.getUsername(), password) == 0) {
			responseMessage+= "Login effettuato";
			int messageSize = responseMessage.length();
			response.clear();
			response.putInt(messageSize);
			response.put(responseMessage.getBytes());
			try {
				send(response, client);
			} catch (IOException e) {
			return -1;
			}
			//Aspetto che il client mi chieda la lista dei followers
			ByteBuffer smallBuffer = ByteBuffer.allocate(SMALL_BUFFER);
			smallBuffer.clear();
			try {
				while(client.read(smallBuffer) <= 0) { }
			} catch (IOException e) {
				return -1;
			}
			smallBuffer.flip().getInt();
			List<String> followers = u.getFollowers();
			Gson gson = new Gson();
			String followersStr = gson.toJson(followers);
			int size = followersStr.length();
			ByteBuffer sendBuffer = ByteBuffer.allocate(size + 4);
			sendBuffer.clear();
			sendBuffer.putInt(size);
			sendBuffer.put(followersStr.getBytes());
			try {
				send(sendBuffer, client);
			} catch (IOException e) {
				return -1;
			}
			
			return 0;
		}else{
			responseMessage+="Password errata";
			response.clear();
			int messageSize = responseMessage.length();
			response.putInt(messageSize);
			response.put(responseMessage.getBytes());
			try {
				send(response, client);
			} catch (IOException e) {
				return -1;
			}
		}
		return -1;
	}
	
public int listUsers() {
		
		
	/**INIZIO calcolo utenti con tag in comune**/
	List<String> myTags = social.getUser(myUsername).getTags();
	HashMap<String,List<String>> object = new HashMap<String, List<String>>();
	int numeroTags = myTags.size();
				
	for(User u : social.getAllUser()) {
		if(u.getUsername().equals(myUsername)) continue;
		List<String> commonTags = new ArrayList<String>();
		for(int i = 0; i < numeroTags; i++) {
			if(u.getTags().contains(myTags.get(i))) {
				commonTags.add(myTags.get(i));
			} 
		} if(!commonTags.isEmpty()) {
			//System.out.println(u.getUsername() + commonTags);
			object.put(u.getUsername(), commonTags);
		}
	}
	/**FINE calcolo utenti con tag in comune**/
	String str = "";
	for(String utente : object.keySet()) {
		str+=":" + utente;
		for(String tag : object.get(utente)) {
			str+="," + tag;
		}
	};
	int size = str.length();
	ByteBuffer sendBuffer = ByteBuffer.allocate(4 + size);
	sendBuffer.clear();
	sendBuffer.putInt(size); 
	sendBuffer.put(str.getBytes());
	try {
		send(sendBuffer, client);
	} catch (IOException e) {
		//Errore nella comunicazione col client
		return -1;
	}
	return 0;
	
		
	}

	private int listFollowing() {
		
		//Ottengo utenti seguiti
		List<String> following = social.getFollowing(myUsername);		
		String str = "Non stai seguendo nessuno";
		if(!following.isEmpty()) {
			str = "";
			for(String utente : following) 
				str+= utente + ",";
		}
		ByteBuffer response = ByteBuffer.allocate(str.length() + 4);
		response.clear();
		response.putInt(str.length());
		response.put(str.getBytes());
		try {
			send(response, client);
		} catch (IOException e) {
			return -1;
		}
		
		return 0;
		
	}
	
	private int followUser(String usernameToFollow) {
		
		String responseMessage = "";
		User toFollow = social.getUser(usernameToFollow);
		User me = social.getUser(myUsername);
		ByteBuffer response = ByteBuffer.allocate(MEDIUM_BUFFER);
		
		if(toFollow == null) {
			responseMessage+="Utente <"+usernameToFollow+"> inesistente. Riprovare";
			response.clear();
			response.putInt(responseMessage.length());
			response.put(responseMessage.getBytes());
			try {
				send(response, client);
			} catch (IOException e) {
				return -1;
			}
			return -1;
		}
		
		try {
			responseMessage = "Adesso segui <"+usernameToFollow+"> ";
			me.addFollowing(usernameToFollow);
			//Mando notifica tramite callback
			callback.update(usernameToFollow, myUsername);
			toFollow.addFollower(myUsername);
			
		}catch(IllegalArgumentException ex) {
			responseMessage="Stai già seguendo <"+usernameToFollow+">.";
		}catch( RemoteException ex){
			//L'utente a cui inviare la notifica non è connesso
			//ignore
		}finally {
			response.clear();
			response.putInt(responseMessage.length());
			response.put(responseMessage.getBytes());
			try {
				send(response, client);
			} catch (IOException e) {
				return -1;
			}
		}
		return 0;
	}
	
	private int unfollowUser(String usernameToUnfollow) {
		
		String responseMessage = "";
		User toUnfollow = social.getUser(usernameToUnfollow);
		User me = social.getUser(myUsername);
		ByteBuffer response = ByteBuffer.allocate(BUFFER_SIZE);
		
		if(toUnfollow == null) {
			responseMessage+="Utente <"+usernameToUnfollow+"> inesistente. Riprovare";
			response.clear();
			response.putInt(responseMessage.length());
			response.put(responseMessage.getBytes());
			try {
				send(response, client);
			} catch (IOException e) {
				return -1;
			}
			return -1;
		}
		
		try {
			me.removeFollowing(usernameToUnfollow);
			responseMessage = "Non segui più <"+usernameToUnfollow+"> ";
			//Aggiorno la lista dei followers
			callback.update(usernameToUnfollow, myUsername);
			toUnfollow.removeFollower(myUsername);
		}catch(IllegalArgumentException ex) {
			responseMessage="Operazione non eseguita perché"
					+ " <"+usernameToUnfollow+"> non è nella tua lista following";
		} catch (RemoteException e) {
			//L'utente a cui inviare la notifica non è connesso
			//ignore
		}finally {
			response.clear();
			response.putInt(responseMessage.length());
			response.put(responseMessage.getBytes());
			try {
				send(response, client);
			} catch (IOException e) {
				return -1;
			}
		}
		return 0;
		
	}
	
	public int viewBlog() {
		
		User me = social.getUser(myUsername);
		List<Long> myBlogID = me.getBlog();
		List<Post> myBlog = new ArrayList<Post>();
		for(Long l : myBlogID) {
			Post toFind = social.getPost(l);
			if(toFind == null)
				continue;
			myBlog.add(social.getPost(l));
		}
		Gson gson = new Gson();
		String toSend = gson.toJson(myBlog);
		
		ByteBuffer sendBuffer = ByteBuffer.allocate(toSend.length() + 4);
		sendBuffer.clear();
		sendBuffer.putInt(toSend.length());
		sendBuffer.put(toSend.getBytes());
		try {
			send(sendBuffer, client);
		} catch (IOException e) {
			return -1;
		}
		return 0;
		
	}

	private int createPost(String titolo, String contenuto) {
		
		String responseMessage = "";
		ByteBuffer response = ByteBuffer.allocate(BUFFER_SIZE);
		long postID; 
		
		if( (postID = social.putPost(myUsername, titolo, contenuto)) == -1 ){
			responseMessage = "Errore nella creazione del post";
		} else {
			//Aggiunge il post anche nella lista per calcolare le ricompense
			social.putOldPost(new Post(social.getPost(postID)));
			responseMessage = "Post creato correttamente, ID del tuo post " + postID; 
		}
		response.clear();
		response.putInt(responseMessage.length());
		response.put(responseMessage.getBytes());
		try {
			send(response, client);
			} catch (IOException e) {
			return -1;
		}
		return 0;
	}
	
	private int rewinPost(String string) {
		//errore  0 -> OK il post esiste ed è nel feed
		//errore -1 -> il post non esiste
		//errore -2 -> il post esiste ma è un proprio post
		//errore -3 -> il post esiste ma non è nel feed
		//errore -4 -> Rewin già effettuato

		long idPost = Long.parseLong(string);
		Post p = social.getPost(idPost);
		int error = -1;
		
		if(p != null) {
			User me = social.getUser(myUsername);
			String autore = (p.getAutore());
			if(!autore.equals(myUsername) && me.getBlog().contains(idPost)) {
				error = -4;
			}else {
				
				if(autore.equals(myUsername)) {
					error = -2; 
				} else {
					List<String> following = social.getFollowing(myUsername);
					//Controllo se l'autore del post di cui voglio fare
					//il rewin è negli utenti che seguo
					if(following.contains(autore)) {
						//Aggiungi il post a me stesso
						me.addPost(p);
						error = 0;
					}else {
						error = -3;
					}	
				}
			}
		}
		ByteBuffer sendBuffer = ByteBuffer.allocate(4);
		sendBuffer.clear();
		sendBuffer.putInt(error);
		try {
			send(sendBuffer, client);
		}catch(IOException ex) {
			ex.printStackTrace();
			return -1;
		}	
		return 0;
	}


	private void deletePost(String string) {
		
		User me = social.getUser(myUsername);
		long idPost = Long.parseLong(string);
		Post p = social.getPost(idPost);
		int error = -1;
		if(p != null) {
			String autore = (p.getAutore());
			//Se sono l'autore del post lo elimino
			//sia dal mio blog che dal social
			if(autore.equals(myUsername)) {
				error = 0;
				social.removePost(idPost);
			//Se il post è un rewin lo elimino solo dal mio blog
			}else if(me.getBlog().contains(idPost)) {
				me.removePost(idPost);
				error = 0;
			}
		} 
		ByteBuffer sendBuffer = ByteBuffer.allocate(4);
		sendBuffer.clear();
		sendBuffer.putInt(error);
		try {
			send(sendBuffer, client);
		}catch(IOException ex) {
			ex.printStackTrace();
			return;
		}	
	}

	private void showPost(String string) {
		//Cerco il post
		long idPost = Long.parseLong(string);
		Post p = social.getPost(idPost);
		String post = "";
		int error = -1;
		if(p != null) {
			String autore = (p.getAutore());
			List<String> following = social.getFollowing(myUsername);
			if(following != null ) {
				//Controllo se il post è nel feed o è scritto da chi
				//ha fatto la richiesta
				if(following.contains(autore) || autore.equals(myUsername)) {
					//trasformo in json per inviarlo
					Gson gson = new Gson();
					post = gson.toJson(p);
					error = 0;
				}
			}else {
				if(autore.equals(myUsername)){
					Gson gson = new Gson();
					post = gson.toJson(p);
					error = 0;
				}
			}
		} 
		ByteBuffer sendBuffer = ByteBuffer.allocate(post.length() + 4);
		sendBuffer.clear();
		if(error == -1)
			sendBuffer.putInt(error);
		else 
			sendBuffer.putInt(post.length());
		sendBuffer.put(post.getBytes());
		try {
			send(sendBuffer, client);
		}catch(IOException ex) {
			ex.printStackTrace();
			return;
		}
	}
	
	private int showFeed() {
		//Dobbiamo prendere tutti i post di tutti gli utenti che
		//myUsername segue
		Gson gson = new Gson();
		//Mi preparo un buffer molto grande
		ByteBuffer sendBuffer = ByteBuffer.allocate(LARGE_BUFFER*3);
		User me = social.getUser(myUsername);
		List<String> followingUsers = me.getFollowing();
		//Per ogni utente seguito
		for(String username : followingUsers) {
			User user = social.getUser(username);
			List<Long> userBlog = user.getBlog();
			//Per ogni post dell'utente seguito
			for(Long idPost : userBlog) {
				Post p = social.getPost(idPost);
				if(p == null)
					continue;
				//Se è un rewin di un mio post non lo mando
				if(p.getAutore().equals(myUsername))
					continue;
				//Altrimenti lo trasformo in json 
				String postToSend = gson.toJson(p);
				int postSize = postToSend.length();
				//Se il buffer è vuoto controllo se c'è abbastanza spazio
				if(sendBuffer.position() == 0) {
					//Alloco un buffer più grande se non è in grado
					//di contenere neanche un singolo post
					while(postSize+4 > sendBuffer.capacity())
						sendBuffer = ByteBuffer.allocate(sendBuffer.capacity()*2);
				}else {
					//Se ci sono già altri post nel buffer
					//e non c'è più spazio per altri post
					//invio prima al client quello che c'è
					if(postSize+4 > sendBuffer.remaining()) {
						try {
							send(sendBuffer, client);
						} catch (IOException e) {
							return -1;
						}
						sendBuffer.clear();
					}
				}
				//Inserisco la dimensione della stringa json
				//e la stringa json
				sendBuffer.putInt(postToSend.length());
				sendBuffer.put(postToSend.getBytes());
			}
		}
		//Quando ho finito tutti i post invio quello che rimane 
		//e poi invio -1 per avvertire il client di aver terminato
		try {
			if(sendBuffer.hasRemaining())
				send(sendBuffer, client);
		sendBuffer.clear();
		sendBuffer.putInt(-1);
		send(sendBuffer, client);
		} catch (IOException e) {
			return -1;
		}
		return 0;		
	}
	
}