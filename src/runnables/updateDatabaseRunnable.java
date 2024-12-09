import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class updateDatabaseRunnable implements Runnable {

	long interval;
	Winsome winsome;
	String db_filepath;
	
	public updateDatabaseRunnable(Winsome winsome, long interval, String db_filepath)
	{
		this.interval = interval;
		this.winsome = winsome;
		this.db_filepath = db_filepath;
	}
	
	//Funzione che svuota il buffer sul canale
	public static void send(ByteBuffer buffer, FileChannel channel)
				throws IOException {
		
		buffer.flip();
		do {
			channel.write(buffer);
		}while(buffer.hasRemaining());
		buffer.clear();
	}
	
	//Funzione che svuota un file
	public void clearFile(FileOutputStream file) {
		PrintWriter writer = new PrintWriter(file);
		writer.print("");
	}
	
	private void saveProgressiveID() throws FileNotFoundException, IOException {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		FileOutputStream file = new FileOutputStream(db_filepath + "progressiveID.txt");
		clearFile(file);
		FileChannel channel = file.getChannel(); 
		buffer.clear();
		
		String idStr = winsome.getProgressiveID() + "";
		buffer.put(idStr.getBytes());
		send(buffer, channel);
		file.close();
	}
	
	
	//Funzione che salva tutte le password
	private void savePasswords(Gson gson) throws FileNotFoundException, IOException {
		ByteBuffer buffer = ByteBuffer.allocate(3096);
		FileOutputStream file = new FileOutputStream(db_filepath + "passwords.txt");
		clearFile(file);
		FileChannel channel = file.getChannel(); 
		buffer.clear();
		
		ConcurrentHashMap<String, String> passwords = winsome.getPasswords();
		String passStr = gson.toJson(passwords);
		buffer.put(passStr.getBytes());
		send(buffer, channel);
		file.close();
		
	}
	
	//Funzione che salva la lista dei post
	private void savePosts(Gson gson) throws FileNotFoundException, IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		FileOutputStream file = new FileOutputStream(db_filepath + "posts.txt");
		clearFile(file);
		FileChannel channel = file.getChannel(); 
		buffer.clear();
		
		buffer.clear();
		//Scrivo la prima parentesi
		String symbols = "[";
		buffer.put(symbols.getBytes());
		send(buffer, channel);
		
		List<Post> allPosts = winsome.getAllPosts();
		int size = allPosts.size();
		int c = 0;
		for(Post p : allPosts ) {
			c++;
			String postStr = gson.toJson(p);
			while(postStr.length() > buffer.capacity())
				buffer = ByteBuffer.allocate(buffer.capacity()*2);
			buffer.put(postStr.getBytes());
			symbols = ",";
			//Se non è l'ultimo post aggiungo la virgola
			if( c != (size) ) buffer.put(symbols.getBytes());
			send(buffer, channel);
			buffer.clear();
		}
		buffer.clear();
		//Scrivo l'ultima parentesi
		symbols = "]";
		buffer.put(symbols.getBytes());
		send(buffer, channel);
		file.close();
		
	}
	
	//Funzione che salva la lista dei post aggiornati all'ultima iterazione
		private void saveOldPosts(Gson gson) throws FileNotFoundException, IOException {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			FileOutputStream file = new FileOutputStream(db_filepath + "lastRewarding.txt");
			clearFile(file);
			FileChannel channel = file.getChannel(); 
			buffer.clear();
			
			buffer.clear();
			//Scrivo la prima parentesi
			String symbols = "[";
			buffer.put(symbols.getBytes());
			send(buffer, channel);
			
			List<Post> allPosts = winsome.getLastRewarding();
			int size = allPosts.size();
			int c = 0;
			for(Post p : allPosts ) {
				c++;
				String postStr = gson.toJson(p);
				while(postStr.length() > buffer.capacity())
					buffer = ByteBuffer.allocate(buffer.capacity()*2);
				buffer.put(postStr.getBytes());
				symbols = ",";
				//Se non è l'ultimo post aggiungo la virgola
				if( c != (size) ) buffer.put(symbols.getBytes());
				send(buffer, channel);
				buffer.clear();
			}
			buffer.clear();
			//Scrivo l'ultima parentesi
			symbols = "]";
			buffer.put(symbols.getBytes());
			send(buffer, channel);
			file.close();
			
		}
	
	//Funzione che salva la lista degli utenti
	public void saveUsers(Gson gson) throws FileNotFoundException, IOException {
	
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		FileOutputStream file = new FileOutputStream(db_filepath + "users.txt");
		clearFile(file);
		FileChannel channel = file.getChannel(); 
		buffer.clear();
		
		buffer.clear();
		//Scrivo la prima parentesi
		String symbols = "[";
		buffer.put(symbols.getBytes());
		send(buffer, channel);
		
		List<User> allUsers = winsome.getAllUser();
		int size = allUsers.size();
		int c = 0;
		for(User u : allUsers ) {
			c++;
			String userStr = gson.toJson(u);
			while(userStr.length() > buffer.capacity())
				buffer = ByteBuffer.allocate(buffer.capacity()*2);
			buffer.put(userStr.getBytes());
			symbols = ",";
			//Se non è l'ultimo utente aggiungo la virgola
			if( c != (size) ) buffer.put(symbols.getBytes());
			send(buffer, channel);
			buffer.clear();
		}
		buffer.clear();
		//Scrivo l'ultima parentesi
		symbols = "]";
		buffer.put(symbols.getBytes());
		send(buffer, channel);
		file.close();
	}

	
	public void run(){
		while(true) {
		try {
			/*Legge tutte le strutture dati del social e salva i dati
			 * su file*/
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			
			saveUsers(gson);
			savePosts(gson);
			saveOldPosts(gson);
			savePasswords(gson);
			saveProgressiveID();
			
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        String message = df.format(
	              new Date(System.currentTimeMillis()));
			System.out.println("Salvataggio effettuato: " 
			+ message);
		} catch (FileNotFoundException e) {
		System.out.println("Impossibile salvare i dati: " + e.getMessage());
		} catch (IOException e) { 
			System.out.println("Impossibile salvare i dati: " + e.getMessage());
		}finally {
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				return;
			}
		}
		
		}
	}

	
}
