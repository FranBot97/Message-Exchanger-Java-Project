import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;


public class ServerMain {
	
	//La dimensione del buffer non è modificabile, il server sa
	//già quanto possono essere lunghe al massimo le richieste
	public final static int BUFFER_SIZE = 1024;
	//percorso per i file contenenti i dati da caricare
	public final static String db_filepath = "database\\";
	//nome del file di configurazione
	public final static String filename = "configServer.txt";

	/*VALORI DI DEFAULT*/
	//# Indirizzo del server
	public final static String DEFAULT_SERVER= "localhost";
	//# Porta TCP del server
	public final static int DEFAULT_TCPPORT = 1919;
	//# Porta UDP del server
	public final static int DEFAULT_UDPPORT = 33333;
	//# Indirizzo di multicast
	public final static String DEFAULT_MULTICAST = "228.5.6.7";
	//# Porta di multicast
	public final static int DEFAULT_MCASTPORT = 44444;
	//# Porta del registry RMI
	public final static int DEFAULT_REGPORT = 1920;
	//# Intervallo del calcolo ricompense
	public final static long DEFAULT_REWARD_INTERVAL = 30000; //millis
	//# Percentuale ricompensa autore
	public final static int DEFAULT_AUTHOR_REWARD=70; //percentage
	//# Intervallo salvataggio su disco
	public final static long DEFAULT_UPDATE_INTERVAL=60000; //millis
	//# Timeout della socket
	public final static long DEFAULT_TIMEOUT=0; //millis //0 = no timeout
	//# Dimensione threadpool
	public final static int DEFAULT_THREADPOOL_SIZE = 10;

	
	public static void main(String[] args){
		
		String SERVER = DEFAULT_SERVER;
		int TCPPORT = DEFAULT_TCPPORT;
		int UDPPORT = DEFAULT_UDPPORT;
		String MULTICAST = DEFAULT_MULTICAST;
		int MCASTPORT = DEFAULT_MCASTPORT;
		int REGPORT = DEFAULT_REGPORT;
		long REWARD_INTERVAL = DEFAULT_REWARD_INTERVAL;
		int AUTHOR_REWARD = DEFAULT_AUTHOR_REWARD;
		long UPDATE_INTERVAL = DEFAULT_UPDATE_INTERVAL;
		long TIMEOUT = DEFAULT_TIMEOUT;
		int THREADPOOL_SIZE = DEFAULT_THREADPOOL_SIZE;
		
				
		/**************** Inizio lettura FILE ***********************/
		System.out.println("--------LETTURA FILE DI CONFIGURAZIONE------------");
		try {
		      File myObj = new File(filename);
		      Scanner myReader = new Scanner(myObj);
		      System.out.println("File di configurazione " + filename + " trovato"
			      		+ "\nConfigurazione parametri in corso..");
		      while (myReader.hasNextLine()) {
		        String line = myReader.nextLine();
		        //Se la linea è vuota o inizia con '#' la ignoro
		        if(line.isBlank() || line.charAt(0) == '#')
		        	continue;	
		        String[] tokens = line.split("=");
		        String parameter = tokens[0];
		        String value = tokens[1];
		        long numeric;
        		//System.out.println(value);
		      try {  
		        switch(parameter) {
		        	case("SERVER"):
			        	SERVER = value;
			        	break;

		        	case("TCPPORT"):
		           		numeric = Integer.parseInt(value);
		        		if(numeric < 0)
		        			throw new IllegalArgumentException();
		        		TCPPORT = (int) numeric;
		        		break;
	
		        	case("UDPPORT"):
		        		numeric = Integer.parseInt(value);
		        		if(numeric < 0)
		        			throw new IllegalArgumentException();
		        		UDPPORT = (int) numeric;
		        		break;
		        	
		        	case("MULTICAST"):
			        	MULTICAST = value;
			        	break;
			        
		        	case("MCASTPORT"):
		        		numeric = Integer.parseInt(value);
		        		if(numeric < 0)
		        			throw new IllegalArgumentException();
		        		MCASTPORT = (int) numeric;
		        		break;
		        		
		        	case("REGPORT"):
		        		numeric = Integer.parseInt(value);
		        		if(numeric < 0)
		        			throw new IllegalArgumentException();
		        		REGPORT = (int) numeric;
		        		break;
		        		
		        	case("REWARD_INTERVAL"):
		        		numeric = Long.parseLong(value);
	        			if(numeric < 0)
	        				throw new IllegalArgumentException();
		        		REWARD_INTERVAL = numeric;
		        		break;
		        		
		        	case("AUTHOR_REWARD"):
		        		numeric = Integer.parseInt(value);
		        		if(numeric < 0 || numeric > 100)
		        			throw new IllegalArgumentException();
		        		AUTHOR_REWARD = (int) numeric;
		        		break;
		        		
		        	case("UPDATE_INTERVAL"):
		        		numeric = Integer.parseInt(value);
		        		if(numeric < 0)
		        			throw new IllegalArgumentException();
		        		UPDATE_INTERVAL = numeric;
		        		break;
		        		
		        	case("TIMEOUT"):
		        		numeric = Integer.parseInt(value);
		        		if(numeric < 0)
		        			throw new IllegalArgumentException();
		        		TIMEOUT = numeric;
		        		break;
		        		
		        	case("THREADPOOL_SIZE"):
		        		numeric = Integer.parseInt(value);
	        			if(numeric <= 0)
	        				throw new IllegalArgumentException();
		        		THREADPOOL_SIZE = (int) numeric;
		        		break;
		        }
		      }catch(NumberFormatException ex) {
		        	System.out.println("Valore " + parameter + " non valido.\n"
	        		  		+ "Assegnato il valore di default.");
	        		  continue;
		       }catch(IllegalArgumentException ex) {
		    	   	System.out.println("Valore " + parameter + " non valido.\n"
	        		  		+ "Assegnato il valore di default.");
	        		  continue;
		       }
		      }
		      myReader.close();
		    } catch (FileNotFoundException e) {
		      System.out.println("File di configurazione " + filename +" non trovato.\n"
		      		+ "Assegnati i valori di default.");
		    }finally {
		    	System.out.println("Configurazione parametri terminata.");
		    }
		System.out.println("--------------------------------------------");
		    

    	/*Funzione che inizializza le strutture dati del server
    	 * leggendo da file*/
    	System.out.println("--------CARICAMENTO DATI DA FILE------------");
    	Winsome WINSOME = new Winsome();
    	try {
    		InitializeDatabase(WINSOME);
    	}catch( IOException | JsonIOException | JsonSyntaxException e) {
			System.out.println("Impossibile leggere correttamente"
					+ " i file di inizializzazione dati"
					+ "\n" + e.getMessage());
					e.printStackTrace();
			return;
		} 
    	System.out.println("--------------------------------------------");
		
		/********* Inizializzazione *********/ 
		System.out.println("--------------AVVIO DEL SERVER--------------");
		//Oggetto condiviso da tutti i thread 
		//contiene i client attualmente serviti da un thread
		processingClients inProgress = new processingClients();
		//Threadpool
		ExecutorService service = Executors.newFixedThreadPool(THREADPOOL_SIZE);
		Registry registry;
    	ServerSocketChannel serverChannel;
    	Selector selector;
    	ServerSocket ss;
    	InetAddress group;
    	//Imposto timeout
    	boolean isTimeout;
    	if(TIMEOUT == 0) //Se è 0 non c'è timeout
    		isTimeout = false;
    	else
    		isTimeout = true;
    	//Calcolo il momento di timeout
    	long timeoutTime = Calendar.getInstance().getTimeInMillis() + TIMEOUT;
    	
    	try {
    	    // Controllo validità del gruppo multicast
    	    group = InetAddress.getByName(MULTICAST);
    	    if(!group.isMulticastAddress()) {
    	    	System.out.println("Indirizzo multicast " 
    	    			+ group.getHostAddress() 
    	    			+ " non valido.\n"
    	    			+ "Assegnato valore di default." );
    	        MULTICAST = DEFAULT_MULTICAST;
    	        group = InetAddress.getByName(MULTICAST);
    	    }
    		registry = LocateRegistry.createRegistry(REGPORT);
    		serverChannel = ServerSocketChannel.open();
    		ss = serverChannel.socket();
    		InetSocketAddress address = new InetSocketAddress(SERVER, TCPPORT);
    		ss.bind(address);
    		serverChannel.configureBlocking(false);
    		selector = Selector.open();
    		serverChannel.register(selector, SelectionKey.OP_ACCEPT);    		
    	}catch(BindException ex) {
    		System.out.println("Indirizzo già in uso, "
    				+ "modificare il file di configurazione e riprovare"
    				+ "\n " + ex.getMessage());
    		return;
    	}catch(IOException ex) {
    		System.out.println("Errore nella creazione del server.\n"
    				+ ex.getMessage());
    		return;
    	}
    	System.out.println("In attesa di connessioni sulla porta " + TCPPORT);
    	System.out.println("--------------------------------------------");
    	
     	
    	/********************* START RMI ********************************/
    	System.out.println("------------AVVIO SERVIZIO RMI--------------");
    	//Mi salvo l'oggetto per passarlo al task
    	FollowerServiceImpl callback = startRMI(REGPORT, WINSOME);
    	System.out.println("--------------------------------------------");
    	/****************************************************************/

    	/*Faccio partire un thread che si occupa di aggiornare i dati
    	 * sul disco a intervalli regolari*/
    	Thread updater = 
    			new Thread(new updateDatabaseRunnable(
    					WINSOME,
    					UPDATE_INTERVAL, db_filepath));
    	updater.start();
    	
    	/*Faccio partire il thread che si occupa di calcolare e comunicare
    	 * le ricompense a intervalli regolari
    	 * */
    	Thread rewarder =
    			new Thread(new estimateRewardRunnable(
    					WINSOME, 
    					REWARD_INTERVAL,
    					AUTHOR_REWARD,
    					MCASTPORT,
    					group));
    	rewarder.start();
    	
    	/*Inizio ciclo server*/
    	//Se è stato impostato il timeout
    	//continuo a iterare finché non ho raggiunto il tempo limite
    	while ( !isTimeout || (Calendar.getInstance().getTimeInMillis()) < timeoutTime ){
  		   try { 			  
  			   if(TIMEOUT == 0) {
  				   if(selector.select() == 0)
  					   continue;
  			   }else{
  				   if(selector.select(TIMEOUT) == 0)
  					   continue;
  			   }
  			  } catch (final IOException ex) {
     			break;
  			  }
      		
  		   //Se ho selezionato un canale ricalcolo il tempo di timeout
  		   	timeoutTime = Calendar.getInstance().getTimeInMillis() + TIMEOUT;
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
             SelectionKey key = iter.next();
                
             //Se la richiesta è una richiesta di connessione
             if (key.isAcceptable()) {
            	 SocketChannel client;
					try{ 	
						client = serverChannel.accept();
						System.out.println("Nuova connessione ricevuta da " 
								+ client.getRemoteAddress());
						client.configureBlocking(false);
						client.register(selector, SelectionKey.OP_READ);
						//invio subito al client UDPPORT, REGPORT, MCASTPORT, MULTICAST
						ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
						buffer.clear();
						buffer.putInt(12 + MULTICAST.length());
						buffer.putInt(UDPPORT);
						buffer.putInt(REGPORT);
						buffer.putInt(MCASTPORT);
						buffer.put(MULTICAST.getBytes());
						buffer.flip();
						do {
							client.write(buffer);
						}while(buffer.hasRemaining());
						
					} catch (IOException e) {
						System.out.println("Errore durante connessione di un client:"
								+ " " + e.getMessage());
				          iter.remove();
						  e.printStackTrace();
						continue;
					}
             }	
             //Se la richiesta è una richiesta di lettura
             if (key.isReadable()) {
            	 //Controllo se la richiesta del client è 
            	 //indirizzata già ad un altro thread
            	 if(inProgress.contains(key)) {
            		 iter.remove();
            		 continue;
            	 }
            	 	SocketChannel client = (SocketChannel) key.channel();
            	 	ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            	 	buffer.clear();
            	 	try {
            	 		int letti = 0;
            	 		int lastRead;
            	 		//Leggo finchè c'è qualcosa nel canale
            	 		while((lastRead = client.read(buffer)) > 0) {
            	 			 	letti = lastRead;
            	 		}
        				//Controllo non ci siano errori
        				if(letti <= 0) {
        					System.out.println("Client disconnesso, rimosso dal set: " 
        							+ client.getRemoteAddress());
        					iter.remove();
        					client.close();
        					continue;
        				}
					}catch (IOException e) {
						System.out.println("Impossibile leggere da client: " + e.getMessage());
						iter.remove();
						try {
							client.close();
						} catch (IOException e1) {
							//e1.printStackTrace();
							continue;
						}
						continue;
					}
            	 	buffer.flip();
            	 	byte[] receivedData = new byte[buffer.limit() - buffer.position()];
            	 	buffer.get(receivedData);
            	 	//Aggiungo il client a quelli attualmente serviti
            	 	inProgress.addKey(key);
            	 	//Assegno la richiesta del client al threadpool
            	 	service.execute(new RequestHandler(WINSOME, 
            	 			receivedData, key, inProgress, callback));
                 }
             iter.remove();
             }
    	}
    	//Chiusura del server
    	
    	//Stop threadpool
    	service.shutdown();
		try {
			if (!service.awaitTermination(5000, TimeUnit.MILLISECONDS))
				service.shutdownNow();
		}
		catch (InterruptedException e) {
			service.shutdownNow();
		}
		//Stop RMI
    	try {
			ss.close();
			selector.close();
			registry.unbind("REGISTRAZIONE");
			registry.unbind("FOLLOWERS");
			UnicastRemoteObject.unexportObject(registry, true);
		} catch (NoSuchObjectException e) {
			System.out.println(e.getMessage());  
		} catch (IOException e) {
			System.out.println(e.getMessage());  
 		} catch (NotBoundException e) {
 			System.out.println(e.getMessage());  
		}
    	System.out.println("Server timeout, terminato");  
    	return;
   }
	
	/** Restituisce il contenuto di un file come stringa
	 * @param fis Il file input stream del file
	 * @param encoding Il tipo di codifica della stringa
	 * **/
	public static String getFileContent(
			   FileInputStream fis,
			   String          encoding ) throws IOException
			 {
			   try( BufferedReader br =
			           new BufferedReader( new InputStreamReader(fis, encoding )))
			   {
			      StringBuilder sb = new StringBuilder();
			      String line;
			      while(( line = br.readLine()) != null ) {
			         sb.append( line );
			         sb.append( '\n' );
			      }
			      return sb.toString();
			   }
			}
	
	/** Prende in ingresso il social da inizializzare e legge i dati
	 * dai file "users.txt", "password.txt", "posts.txt", "lastRewarding.txt"
	 * e "progressiveID.txt". Aggiunge i dati raccolti al social, se i file
	 * sono vuoti non viene aggiunto niente, se i file sono mal formattati
	 * o non sono stati trovati viene lanciata un'eccezione
	 * 
	 * @param winsome Il social da inizializzare
	 * */
	private static void InitializeDatabase(Winsome winsome) 
			throws JsonIOException, JsonSyntaxException, IOException {
			try {
					/*UTENTI*/
					FileInputStream file = new FileInputStream(db_filepath + "users.txt");
					JsonReader reader = new JsonReader(new InputStreamReader(file));	
					try{
						reader.beginArray();
						//Leggo ogni oggetto "Utente" dal file e passo la lista movimenti al pool
						while (reader.hasNext()) {
							User userToAdd = new Gson().fromJson(reader, User.class);
							winsome.addUtente(userToAdd);
						}
						int size = winsome.getAllUser().size();
						System.out.println("Utenti caricati da file: " + size);
						reader.endArray();
					}catch(IOException ex) {
						//significa che il file è vuoto
					}
			}catch(FileNotFoundException ex) {
				System.out.println("File users mancante. "
						+ "Utenti caricati da file: 0 ");
			}
			
			try {	/*PASSWORDS*/
					FileInputStream file = new FileInputStream(db_filepath + "passwords.txt");
					String hashPasswords = getFileContent(file, "UTF-8");
					if(hashPasswords.isBlank() || hashPasswords.isEmpty()) {
						
					}else {
					//Leggo le passwords
					Type listOfMyClassObject = 
            	 			new TypeToken<ConcurrentHashMap<String, String>>(){}.getType();
					
						ConcurrentHashMap<String, String> hashPassw = 
								new Gson().fromJson(hashPasswords, listOfMyClassObject);
						winsome.setPasswords(hashPassw);
						System.out.println("Credenziali utenti caricate correttamente.");
					}
			}catch(FileNotFoundException ex) {
				System.out.println("File password mancante.");
			}
			
			try {
					/*POST*/
					FileInputStream file = new FileInputStream(db_filepath + "posts.txt");
					JsonReader reader = new JsonReader(new InputStreamReader(file));		
					try{
						reader.beginArray();
					//Leggo ogni oggetto "Post" dal file e passo la lista movimenti al pool
					while (reader.hasNext()) {
						Post postToAdd = new Gson().fromJson(reader, Post.class);
						winsome.addPost(postToAdd);
					}
					int size = winsome.getAllPosts().size();
					System.out.println("Post caricati da file: " + size);
					reader.endArray();
					}catch(IOException ex) {
						//significa che il file è vuoto
					}
			}catch(FileNotFoundException ex) {
				System.out.println("File posts mancante. "
						+ "Post caricati da file: 0 ");
			}	
			try {
					/*OLD POSTS*/
					FileInputStream file = new FileInputStream(db_filepath + "lastRewarding.txt");
					JsonReader reader = new JsonReader(new InputStreamReader(file));	
					try{	reader.beginArray();
					//Leggo ogni oggetto "Post" dal file e passo la lista movimenti al pool
					while (reader.hasNext()) {
						Post postToAdd = new Gson().fromJson(reader, Post.class);
						winsome.addOldPost(postToAdd);
					}
					int size = winsome.getLastRewarding().size();
					System.out.println("Vecchi post caricati da file: " + size);
					reader.endArray();
					}catch(IOException ex) {
						//significa che il file è vuoto
					}
			}catch(FileNotFoundException ex) {
				System.out.println("File lastRewarding mancante. "
						+ "Vecchi post caricati da file: 0 ");
			}							
			try {		//Leggo progressive ID
					FileInputStream file = new FileInputStream(db_filepath + "progressiveID.txt");
					String idStr = new String(file.readAllBytes());
					if(idStr.equals("") || idStr.isBlank())
						winsome.setProgressiveID(0);
					else {
						long idPost = Long.parseLong(idStr);
						winsome.setProgressiveID(idPost);
					}
					file.close();
			}catch(FileNotFoundException ex) {
				System.out.println("File contenente progressiveID mancante."
						+ "Prossimo post ID: 0 ");
			}		
			System.out.println("Lettura completata");
						
	}
		
	/**
	 * Avvia i servizi di registrazione e di notifica followers,
	 * restituisce lo stub del servizio followers.
	 * 
	 * @param RMIservice La porta su cui avviare i servizi
	 * @param social Winsome
	 * **/
	private static FollowerServiceImpl startRMI(int RMIservice, Winsome social) {
			//Creo l'oggetto remoto, lo stub e il registro per i servizi RMI
		
			try { 	Registry r = LocateRegistry.getRegistry(RMIservice);
					
					//Servizio di registrazione
					RegistrationServiceImpl RSI = new RegistrationServiceImpl(social);
					RegistrationService stubRS  =
							(RegistrationService)UnicastRemoteObject.exportObject(RSI,0);
					r.rebind("REGISTRAZIONE", stubRS);
					System.out.println("Servizio "
					 		+ "< REGISTRAZIONE > pronto sulla "
					 		+ "porta = "+RMIservice);
					
					//Servizio di notifica nuovi followers
					FollowerServiceImpl FSI = new FollowerServiceImpl( );
			    	FollowerServiceInterface stubFSI =
							(FollowerServiceInterface)UnicastRemoteObject.exportObject(FSI,0);
					String name = "FOLLOWERS";
					r.bind(name, stubFSI);
					System.out.println("Servizio "
					 		+ "< FOLLOWERS > pronto sulla "
					 		+ "porta = "+RMIservice);
					return FSI;

					} catch (RemoteException | AlreadyBoundException e) {
						System.out.println(e.getMessage() + e.getStackTrace());
						
					}
			return null;
	}
}

