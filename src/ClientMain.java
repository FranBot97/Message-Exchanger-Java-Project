import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

public class ClientMain {
	
	public final static String filename="configClient.txt";
	
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

	public static int SIZE_OF_INT = 4; 
	public static int SIZE_OF_DOUBLE = 8; 
	public static int MAX_SIZE = 1024;

	public static void send(ByteBuffer buffer, SocketChannel server) 
			throws IOException {
		buffer.flip();
		do {
			server.write(buffer);
		}while(buffer.hasRemaining());
		buffer.clear();
	}
	
	public static void receive(ByteBuffer buffer, SocketChannel server, int length)
			throws IOException {
		buffer.clear();
		server.read(buffer);
		//Leggo dal canale finchè non ho ricevuto tutto il contenuto
		while(buffer.position() < length) {
			System.out.println(buffer.position());
			server.read(buffer);
		}		
	}
	
	public static void main(String[] args) {
		
		ByteBuffer sendBuffer = ByteBuffer.allocate(MAX_SIZE);
		ByteBuffer smallBuffer = ByteBuffer.allocate(SIZE_OF_INT);
		String SERVER = DEFAULT_SERVER;
		int TCPPORT = DEFAULT_TCPPORT;
		@SuppressWarnings("unused")
		int UDPPORT = DEFAULT_UDPPORT;
		String MULTICAST = DEFAULT_MULTICAST;
		int MCASTPORT = DEFAULT_MCASTPORT;
		int REGPORT = DEFAULT_REGPORT;
		
		/**************** Inizio lettura FILE ***********************/
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
		        int numeric;
		      try {  
		        switch(parameter) {
		        	case("SERVER"):
			        	SERVER = value;
			        	break;

		        	case("TCPPORT"):
		           		numeric = Integer.parseInt(value);
			        	if(numeric < 0)
		        			throw new IllegalArgumentException();
		        		TCPPORT = numeric;
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
		/*****************************************************************/
		
		boolean loggedIn = false;
		String myUsername = "";
		List<Post> myBlogCache = new ArrayList<Post>(); //cache del mio blog
		List<String> myFollowers = new ArrayList<String>();
		FollowerServiceInterface server = null; //servizio di notifiche follower
		NotifyEventInterface stub = null; //stub associato

		try {
			//Connetto al server
	   		SocketAddress address = new InetSocketAddress(SERVER, TCPPORT);
	   		SocketChannel winsome = SocketChannel.open(address);
			Socket socket = winsome.socket();
			socket.setKeepAlive(true);
			winsome.configureBlocking(true);
			//Al momento della connessione
			//ricevo dal server in ordine:
			//dimensione messaggio, UDP PORT, RMI PORT,
			//MULTICAST PORT, INDIRIZZO MULTICAST
			receive(smallBuffer, winsome, SIZE_OF_INT);
			int messSize = smallBuffer.flip().getInt();
			ByteBuffer parameters = ByteBuffer.allocate(messSize);
			receive(parameters, winsome, messSize);
			parameters.flip();
			UDPPORT = parameters.getInt();
			REGPORT = parameters.getInt();
			MCASTPORT = parameters.getInt();
			byte[] dst = new byte[messSize - 3*SIZE_OF_INT]; 
			parameters.get(dst);
			MULTICAST = new String(dst);	
			//Controllo subito se il multicast va bene
    	    InetAddress group = InetAddress.getByName(MULTICAST);
    	    if(!group.isMulticastAddress()) {
    	    	System.out.println("Indirizzo multicast " 
    	    			+ group.getHostAddress() 
    	    			+ " non valido.\n"
    	    			+ "Assegnato valore di default." );
    	        MULTICAST = DEFAULT_MULTICAST;
    	        group = InetAddress.getByName(MULTICAST);
    	    }
    	    
    	    Thread notifyWallet = null;    
			Registry r = LocateRegistry.getRegistry(REGPORT);
			Scanner keyboard;

	        System.out.println("Benvenuto in Winsome! Cosa vuoi fare?"
	        		+ "\n Digita 'help' per la lista dei comandi disponibili");
		
	        while(true) {	
		   		try {
		   			//Lettura da tastiera
		   			String messaggioDaInviare = "quit";
		   			keyboard = new Scanner(System.in);
		   			System.out.println("Digita il prossimo comando:");
		   			try {
		   				messaggioDaInviare = keyboard.nextLine();
		   			}catch(NoSuchElementException ex) {
		   				messaggioDaInviare = "quit";
		   			}
		   			String[] dati = messaggioDaInviare.split(" ");
		   			while(dati.length == 0) {
		   				try {
			   				messaggioDaInviare = keyboard.nextLine();
			   				dati = messaggioDaInviare.split(" ");
			   			}catch(NoSuchElementException ex) {
			   				messaggioDaInviare = "quit";
			   				break;
			   			}
		   			}

		   			//In base a cosa ho letto eseguo una richiesta
		   			/****************       REGISTER     ***********************/
		   			if(dati[0].equals("register")) {
		   				if(loggedIn) {
		   					System.out.println("Devi fare il logout prima"
		   							+ " di registare una nuova utenza.");
		   					continue;
		   				}
		   				if(dati.length < 3) {
		   					System.out.println("Formato errato,"
		   							+ " esegui come: register <username> <password> <tags>");
		   					continue;
		   				}
		   				if(dati.length > 8) {
		   					System.out.println("Sono consentiti al massimo 5 tag, riprovare");
		   					continue;
		   				}	
		   				
		   				String username = dati[1];
						if(username.length() > 20) {
							System.out.println("Username troppo lungo, massimo 20 caratteri");
		   					continue;	
						}	
						
						String password = dati[2];
						List<String> tags = new ArrayList<String>(5);
						boolean tooLong = false;
						for(int i = 3; i < dati.length; i++) {
							if(dati[i].length() > 20) {
									System.out.println("Tag troppo lungo, "
											+ "massimo 20 caratteri");
									tooLong = true;
				   					break;	
							}
						tags.add(dati[i].toLowerCase());
						}
						if(tooLong)
							continue;
						RegistrationService serverObject;
						Remote RemoteObject;
						try {
							RemoteObject = r.lookup("REGISTRAZIONE");
						}catch(Exception ex) {
							System.out.println("Server irraggiungibile, riprovare - "+ ex.getMessage());
							keyboard.close();
							return;
						}
						serverObject = (RegistrationService) RemoteObject;
						serverObject.register(username, Hash.bytesToHex(Hash.sha256(password)), tags);
						System.out.println("Registrazione effettuata"); 	
		   			}
		   			
		   			/****************       LOGIN      ***********************/
		   			else if(dati[0].equals("login")) {
		   				if(loggedIn) {
		   					System.out.println("Login già effettuato, per rieffettuare"
		   							+ " il login digita prima 'logout'");
		   					continue;
		   				}
		   				if(dati.length != 3) {
		   					System.out.println("Formato errato, "
		   							+ "esegui come: login <username> <password>");
		   					continue;
		   				}
		   				try {
		   					//Preparo i dati e il buffer per l'invio 
		   					//della richiesta
		   					String username = dati[1];
		   					String password = Hash.bytesToHex(Hash.sha256(dati[2]));
		   					String content = "login," + username +","+  password;
		   					sendBuffer.clear();
		   					//sendBuffer.putInt(messageSize);
							sendBuffer.put(content.getBytes());
							//Invio al server per lettura
							send(sendBuffer, winsome);
							//Leggo responso
							receive(smallBuffer, winsome, SIZE_OF_INT);
							smallBuffer.flip();
							int size = smallBuffer.getInt();
							ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
							receive(receiveBuffer, winsome, size);
							receiveBuffer.flip();
		            	 	byte[] receivedData = new byte[size];
		            	 	receiveBuffer.get(receivedData);
		            	 	String print = new String(receivedData);

		            	 	//Se è andato a buon fine
		            	 	//chiedo la lista dei followers
		            	 	//e aggiorno i parametri
		            	 	if(print.equals("Login effettuato")) {           	 		
		            	 		sendBuffer.putInt(1);
		            	 		send(sendBuffer, winsome);
		            	 		//Aspetto la dimensione della stringa json
		            	 		receive(smallBuffer, winsome, SIZE_OF_INT);
		            	 		size = smallBuffer.flip().getInt();
		            	 		receiveBuffer = ByteBuffer.allocate(size);
		            	 		//Leggo la lista
		            	 		receive(receiveBuffer, winsome, size);
								receiveBuffer.flip();
			            	 	receivedData = new byte[size];
			            	 	receiveBuffer.get(receivedData);
			            	 	String followersStr = new String(receivedData);
			            	 	//Ottengo l'oggetto
			            	 	Gson gson = new Gson();
			            	 	Type listOfMyClassObject = new TypeToken<ArrayList<String>>(){}.getType();
			            	 	myFollowers = gson.fromJson(followersStr, listOfMyClassObject );
		            	 		//Aggiorno parametri
		            	 		loggedIn = true;
		            	 		myUsername = username;
		            	 		//Mi registro al servizio di notifiche followers
		            	 		//System.out.println("Registering for callback..");
		            	 		try {
		            	 			server = (FollowerServiceInterface)r.lookup("FOLLOWERS");
								}catch(Exception ex) {
									System.out.println("Server irraggiungibile, riprovare - "+ ex.getMessage());
									continue;
								}
		            	 		NotifyEventInterface callbackObj = new NotifyEventImpl(myFollowers);
		            	 		stub = (NotifyEventInterface)UnicastRemoteObject.exportObject(callbackObj, 0);
		            	 		server.registerForCallback(myUsername, stub);
		            	 		
		            	 		 //Creo il thread per ricevere i messaggi multicast
		                	    //Avviato nella fase di login e interrotto nella fase di logout
		            	 		notifyWallet = new Thread(new clientMulticastRunnable(MCASTPORT, group));
		            	 		notifyWallet.start();
		            	 		
		            	 	}
		            	 	//Stampo messaggio di errore o di successo
		            	 	System.out.println(print);
		            	 	continue;
								
		   				}catch (IOException ex) {
		   					System.out.println("Errore di comunicazione col server: "+
		   					ex.getMessage());
		   					keyboard.close();
		   					return;
		   				}catch (NoSuchAlgorithmException ex) {
		   					System.out.println("Errore di sicurezza della password" +
		   					ex.getMessage());
		   				}
		   			}
		   			
		   			/*****************		LOGOUT		**************/
		   			else if(dati[0].equals("logout")) {
		   				if(dati.length != 1) {
		   					System.out.println("Formato errato, esegui come: logout");
		   					continue;
		   					}
	   					if(!loggedIn)
	   						System.out.println("Sei già disconnesso");
	   					else {
		   					try {	server.unregisterForCallback(myUsername, stub );
		   					
		   					}catch(RemoteException ex) {
		   						System.out.println("Errore di comuncazione col server:"
		   								+ ex.getMessage());
		   						keyboard.close();
		   						return;
		   					}
		   						myFollowers.clear();
		   						loggedIn = false;
		   						myUsername = "";
		   						System.out.println("Logout effettuato");
		   						notifyWallet.interrupt();
	   					}
	   					continue;
		   			}	
		   			
		   					   			
		   		/***********		LIST USERS     *******************/
		   			else if(messaggioDaInviare.equals("list users")) {
		   				if(dati.length != 2) System.out.println("Formato errato, "
		   						+ "esegui come: list users");
	   					if(!loggedIn) {
	   						System.out.println("Devi prima effettuare il login");
	   						continue;
	   					}	
	   					
	   					//Devo inviare al server il comando listUsers e il mio username
	   					try {
		   					sendBuffer.clear();
		   					String content = "listUsers," + myUsername;
							sendBuffer.put(content.getBytes());
							//Invio al server per lettura
							send(sendBuffer, winsome);	
							//Leggo la dimensione della stringa da ricevere
							receive(smallBuffer, winsome, SIZE_OF_INT);
							smallBuffer.flip();
							int size = smallBuffer.getInt();
							ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
							//Ricevo la stringa
							receive(receiveBuffer, winsome, size);
							receiveBuffer.flip();
							byte[] receivedData = new byte[size];
							receiveBuffer.get(receivedData);
							String receivedStr = new String(receivedData);
							
							//Preparo la stampa
							System.out.println("|   UTENTE           |      TAG      ");
							System.out.println("--------------------------------------");
							
							//Gestisco la decifrazione della stringa
							String[] listUsers = receivedStr.toString().split(":");
							for(String utenteTags : listUsers) {
								String[] tokens = utenteTags.split(",");
								String username = tokens[0];
								if(username.equals("")) continue;
								int c = 0;
								System.out.print("| "+username);
								while(username.length() + c < 19) {
									System.out.print(" ");
									c++;
								}
								System.out.print("|");
								for(int j = 1; j<tokens.length; j++) {
									System.out.print(tokens[j]);
									if(j != (tokens.length - 1))
										System.out.print(",");
								}
								System.out.print("\n");
							}								
						
						System.out.println("---------------------------------------");
	   					}catch (IOException ex) {
		   					System.out.println("Errore di comunicazione col server: "+
		   					ex.getMessage());
		   					keyboard.close();
		   					return;
		   				}
		   			}
		   			
		   			/************** LIST FOLLOWERS ******************/
		   			else if(messaggioDaInviare.equals("list followers")) {
		   				
		   				System.out.println("|  UTENTI CHE TI SEGUONO  |");
						System.out.println("|-------------------------|");
						for(String user : myFollowers) {
							int c = user.length();
							System.out.print("| "+ user);
							while(c < 24) {
								System.out.print(" ");
								c++;
							}
							System.out.print("|");
							System.out.print("\n");
						}								
						System.out.println("|_________________________|");		
		   			}
		   			
		   			
		   			
		   			/**************		 LIST FOLLOWING			******************/
		   			else if(messaggioDaInviare.equals("list following")) {
		   				if(dati.length != 2) System.out.println("Formato errato,"
		   						+ " esegui come: list following");
	   					if(!loggedIn) {
	   						System.out.println("Devi prima effettuare il login");
	   						continue;
	   					}	
	   					
	   					//Devo inviare al server il comando listFollowing e il mio username
	   					try {
		   					sendBuffer.clear();
		   					String content = "listFollowing," + myUsername;
							sendBuffer.put(content.getBytes());
							//Invio al server per lettura
							send(sendBuffer, winsome);
							//Leggo la dimensione della risposta
							receive(smallBuffer, winsome, SIZE_OF_INT);
							smallBuffer.flip();
							int size = smallBuffer.getInt();
							ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
							receive(receiveBuffer, winsome, size);
							receiveBuffer.flip();
							byte[] str = new byte[size];
							receiveBuffer.get(str);
							String receivedData = new String(str);
													
							System.out.println("|    UTENTI CHE SEGUI     |");
							System.out.println("|-------------------------|");
							String[] names = receivedData.split(",");
							for(String n : names) {
								int c = n.length();
								System.out.print("| "+ n);
								while(c < 19) {
									System.out.print(" ");
									c++;
								}
								System.out.print("|");
								System.out.print("\n");
							}								
							System.out.println("|________________________|");			
		   				}catch (IOException ex) {
		   					System.out.println("Errore di comunicazione col server: "+
		   					ex.getMessage());
		   					keyboard.close();
		   					return;
		   				}
		   			}
		   			
		   			/**************		 FOLLOW USER 		***********************/
		   			else if(dati[0].equals("follow")) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 2) {
		   					System.out.println("Formato errato, esegui come: follow <username>");
		   					continue;
		   				}
		   				if(dati[1].equals(myUsername)) {
		   					System.out.println("Non puoi seguire te stesso!");
		   					continue;
		   				}
		   			try {	
		   				String userToFollow = dati[1];
	   					sendBuffer.clear();
	   					String content = "follow," + myUsername + "," +  userToFollow;
						sendBuffer.put(content.getBytes());
						//Invio al server per lettura
						send(sendBuffer, winsome);
						receive(smallBuffer, winsome, SIZE_OF_INT);
						smallBuffer.flip();
						int size = smallBuffer.getInt();
						ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
						//Leggo responso	
						receive(receiveBuffer, winsome, size);
						receiveBuffer.flip();
	            	 	byte[] receivedData = new byte[size];
	            	 	receiveBuffer.get(receivedData);
	            	 	String print = new String(receivedData);
	            	 	System.out.println(print); 	
	            	 							
	   				}catch (IOException ex) {
	   					System.out.println("Errore di comunicazione col server: "+
	   					ex.getMessage());
	   					}	
		   			}
		   			
		   			
		   			/******************	 UNFOLLOW USER		******************/
		   			else if(dati[0].equals("unfollow")) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 2) {
		   					System.out.println("Formato errato, esegui come: unfollow <username>");
		   					continue;
		   				}
		   				if(dati[1].equals(myUsername)) {
		   					System.out.println("Non puoi smettere di seguire te stesso!");
		   					continue;
		   				}
		   			try {	
		   				String userToFollow = dati[1];
	   					sendBuffer.clear();
	   					String content = "unfollow," + myUsername + "," +  userToFollow;
						sendBuffer.put(content.getBytes());
						//Invio al server per lettura
						send(sendBuffer, winsome);
						//Leggo responso	
						receive(smallBuffer, winsome, SIZE_OF_INT);
						smallBuffer.flip();
						int size = smallBuffer.getInt();
						ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
						receive(receiveBuffer, winsome, size);
						receiveBuffer.flip();
	            	 	byte[] receivedData = new byte[size];
	            	 	receiveBuffer.get(receivedData);
	            	 	String print = new String(receivedData);
	            	 	System.out.println(print);
				
	   				}catch (IOException ex) {
	   					System.out.println("Errore di comunicazione col server: "+
	   					ex.getMessage());
	   					}	
		   			}
		   			
		   			/*********** 	VIEW BLOG 		******************/
		   			else if(dati[0].equals("blog")) {
		   				
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				} 
		   				if(dati.length != 1) {
		   					System.out.println("Formato errato, esegui come: blog");
		   					continue;
		   				}
		   				
		   				try {	
			   				String content = "viewBlog," + myUsername;
			   				sendBuffer.clear();
			   				sendBuffer.put(content.getBytes());
			   				send(sendBuffer, winsome);
			   				//Leggo risposta
			   				//Leggo prima dimensione stringa json
			   				receive(smallBuffer, winsome, SIZE_OF_INT);
			   				smallBuffer.flip();
			   				int size = smallBuffer.getInt();
			   				//Leggo tutto il resto			   				
							ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
							receive(receiveBuffer, winsome, size);
							receiveBuffer.flip();
		            	 	byte[] receivedData = new byte[size];
		            	 	receiveBuffer.get(receivedData);
		            	 	String receivedString = new String(receivedData);
		            	 	Gson gson = new Gson();
		            	 	Type listOfMyClassObject = 
		            	 			new TypeToken<ArrayList<Post>>(){}.getType();
		            	 	List<Post> myBlog = gson.fromJson(receivedString, listOfMyClassObject);
		            	 	//Tengo una copia dei post in una lista
		            	 	//poichè mi aspetto che dopo aver chiesto
		            	 	//di vedere i post il client ne voglia aprire uno
		            	 	myBlogCache = myBlog;
		            		System.out.println("|------------------- IL MIO BLOG --------------------|");
							System.out.println("|Id     | Autore              | Titolo               |");
							System.out.println("|----------------------------------------------------|");
		            	 	if(myBlog.isEmpty())
		            	 		System.out.println("| Non hai ancora pubblicato niente                   |");
		            	 	for(Post p : myBlog) {
		            	 		String idPost = p.getId() + "";
			            	 	String autore = p.getAutore();
			            	 	String titolo = p.getTitolo();
			            	 	//Stampa corretta ID
								System.out.print("| "+ idPost);
								int c = idPost.length();
									while(c < 6) {
										System.out.print(" ");
										c++;
									}						
								//Stampa corretta Autore
								System.out.print("| "+ autore);
								c = autore.length();
									while(c < 20) {
										System.out.print(" ");
										c++;
									}
								//Stampa corretta Titolo
									System.out.print("| "+ titolo);
									c = titolo.length();
										while(c < 21) {
											System.out.print(" ");
											c++;
										}
									System.out.print("|");
									System.out.print("\n"); 	
		            	 	}
		            		System.out.println("|____________________________________________________|");
		   				}catch (IOException ex) {
		   					System.out.println("Errore di comunicazione col server: "+
		   					ex.getMessage());
		   					}	

		   			}
		   				   		
		   			/**************			CREATE POST		***********************/
		   			else if(dati[0].equals("post")) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				String[] str = messaggioDaInviare.split("\"");	
		   				if(str.length != 4 ||  messaggioDaInviare.charAt(messaggioDaInviare.length()-1) != '\"') {
		   					System.out.println("Formato errato, esegui come: post <titolo> <contenuto>");
		   					System.out.println("Inserisci il titolo e il contenuto tra virgolette");
		   					continue;
		   				}
			   			String titoloPost = str[1];
			   			String testoPost = str[3]; 
		   			try {	
		   			
		   				if(titoloPost.length()>20) {
		   					System.out.println("Usare al massimo 20 caratteri per il titolo");
		   					continue;
		   				}
		   				if(testoPost.length()>500) {
		   					System.out.println("Usare al massimo 500 caratteri per il contenuto");
		   					continue;
		   				}
		   		  						   					
		   				int titleSize = titoloPost.length(); 
		   				//Invio sul canale "post," + myUsername + "," 
		   				//+ titleSize + titoloPost + testoPost
		   				String content = "post," + myUsername + ",";
		   				sendBuffer.clear();
	   					sendBuffer.put(content.getBytes());
	   					sendBuffer.putInt(titleSize);
	   					content = titoloPost + testoPost;
						sendBuffer.put(content.getBytes());
						//Invio al server per lettura
						send(sendBuffer, winsome);
						
						//Leggo responso	
						receive(smallBuffer, winsome, SIZE_OF_INT);
						int size = smallBuffer.flip().getInt();
						ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
						receive(receiveBuffer, winsome, size);
						receiveBuffer.flip();
	            	 	byte[] receivedData = new byte[size];
	            	 	receiveBuffer.get(receivedData);
	            	 	String print = new String(receivedData);
	            	 	System.out.println(print);
			
	   				}catch (IOException ex) {
	   					System.out.println("Errore di comunicazione col server: "+
	   					ex.getMessage());
	   					}	
		   			}
		   			
		   			/*************** SHOW FEED ***************************/
		   			else if(dati[0].equals("show") && dati[1].equals("feed")) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 2) {
		   					System.out.println("Formato errato, esegui come: show feed");
		   					continue;
		   				}
		   				String content = "showFeed," + myUsername;
	   					sendBuffer.clear();
						sendBuffer.put(content.getBytes());
						//Invio al server per lettura
						send(sendBuffer, winsome);
						//Leggo i post ricevuti
						Gson gson = new Gson();
						boolean empty = true;
						System.out.println("|------------------- IL MIO FEED --------------------|");
						System.out.println("|Id     | Autore              | Titolo               |");
						System.out.println("|----------------------------------------------------|");
						while(true) {
								receive(smallBuffer, winsome, SIZE_OF_INT);
								int postSize = smallBuffer.flip().getInt();
							if(postSize > 0) {
								empty = false;
								ByteBuffer receiveBuffer = ByteBuffer.allocate(postSize);
								receive(receiveBuffer, winsome, postSize);
								byte[] receivedData = new byte[postSize];
								receiveBuffer.flip().get(receivedData);
								String receivedString = new String(receivedData);
								Post postToShow = gson.fromJson(receivedString, Post.class);
			            	 	String idPost = postToShow.getId() + "";
			            	 	String autore = postToShow.getAutore();
			            	 	String titolo = postToShow.getTitolo();
			            	 	//Stampa corretta ID
								System.out.print("| "+ idPost);
								int c = idPost.length();
									while(c < 6) {
										System.out.print(" ");
										c++;
									}						
								//Stampa corretta Autore
								System.out.print("| "+ autore);
								c = autore.length();
									while(c < 20) {
										System.out.print(" ");
										c++;
									}
								//Stampa corretta Titolo
									System.out.print("| "+ titolo);
									c = titolo.length();
										while(c < 21) {
											System.out.print(" ");
											c++;
										}
									System.out.print("|");
									System.out.print("\n"); 	
							}else {
								//finito
								if(empty)
			            	 		System.out.println("| Non c'è niente qui                                 |");
			            	 	System.out.println("|____________________________________________________|");
			            	 	break;
							}
						}
		   			}
		   			
   			
		   			/***************		 SHOW POST		********************/
		   			else if(dati[0].equals("show") && dati[1].equals("post")) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 3) {
		   					System.out.println("Formato errato, esegui come: show post <id>");
		   					continue;
		   				}
		   				long idLong;
		   				try{
		   					idLong = Long.parseLong(dati[2]);
		   				}catch(NumberFormatException ex) {
		   					System.out.println("ID post non valido");
		   					continue;
		   				}
		   				try {
		   					String idPost = dati[2];
		   					Post postToShow = null;
		   					//Se il post è tra quelli in cache
		   					//mi risparmio di interrogare il server.
		   					//Se il post era un rewin sarà ancora visibile
		   					//anche se cancellato dall'autore finchè non viene 
		   					//richiesta nuovamente la lista dei post
		   					for(Post p : myBlogCache) {
		   						if (p.getId() == idLong) {
		   							postToShow = p;
		   						}
		   					}
		   					if(postToShow == null) {
			   					String content = "showPost," + myUsername +"," + idPost;
			   					sendBuffer.clear();
								sendBuffer.put(content.getBytes());
								//Invio al server per lettura
								send(sendBuffer, winsome);
								//Leggo responso
								receive(smallBuffer, winsome, SIZE_OF_INT);
								int size = smallBuffer.flip().getInt();
								if(size == -1) {
									System.out.println("Il post non esiste o "
											+ "non appartiene al tuo feed");
									continue;
								}
								ByteBuffer receiveBuffer = ByteBuffer.allocate(size);
								receive(receiveBuffer, winsome, size);
								receiveBuffer.flip();
								byte[] str = new byte[size];
								receiveBuffer.get(str);
								String json = new String(str);
							//	System.out.println(json);
				   				Gson gson = new Gson();
				   				postToShow = gson.fromJson(json, Post.class);
		   						}
				   			System.out.println("|------------- POST " + postToShow.getId() + " ------------");
				   			System.out.println("| Autore: " + postToShow.getAutore());
				   			System.out.println("| Titolo: " + postToShow.getTitolo());
				   			System.out.println("| Contenuto: " + postToShow.getContenuto());
				   			System.out.println("| [Likes " + postToShow.getLike() + "]"
				   			+ "     [Dislikes " + postToShow.getDislike() + "]");
				   			System.out.println("Commenti: " + postToShow.getNumero_commenti());
				   			for(Comment c : postToShow.getCommenti()) {
				   				System.out.println("|	" + c.getAutore() + ": " + c.getContenuto());   					
				   			}
				   			System.out.println("|_______________________________________");
		            	 	continue;		
		   				}catch (IOException ex) {
		   					System.out.println("Errore di comunicazione col server: "+
		   					ex.getMessage());	
		   				}
		   			}
		   			
		   			/***************		DELETE POST		********************/
		   			else if(dati[0].equals("delete") && dati[1].equals("post")) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 3) {
		   					System.out.println("Formato errato, esegui come: delete post <id>");
		   					continue;
		   				}
		   				long idLong;
		   				try{
		   					idLong = Long.parseLong(dati[2]);
		   				}catch(NumberFormatException ex) {
		   					System.out.println("ID post non valido");
		   					continue;
		   				}
		   				try {
		   					String idPost = dati[2];
		   					String content = "deletePost," + myUsername +"," + idPost;
		   					sendBuffer.clear();
							sendBuffer.put(content.getBytes());
							//Invio al server per lettura
							send(sendBuffer, winsome);
							//Leggo responso
							receive(smallBuffer, winsome, SIZE_OF_INT);
							int error = smallBuffer.flip().getInt();
							if(error == -1) {
								System.out.println("Il post non esiste o non è tuo");
								continue;
							} else {
								System.out.println("Post cancellato correttamente dal tuo blog");
								for(Post p : myBlogCache) {
									if (p.getId() == idLong) {
										myBlogCache.remove(p);
										break;
									}
								}
								continue;
							}
								
		   				}catch (IOException ex) {
		   					System.out.println("Errore di comunicazione col server: "+
		   					ex.getMessage());	
		   				}
		   			}
		   			/*************** 	REWIN POST 	******************/
		   			else if(dati[0].equals("rewin")) {
		   				
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 2) {
		   					System.out.println("Formato errato, esegui come: rewin <id>");
		   					continue;
		   				}
		   				try{
		   					Integer.parseInt(dati[1]);
		   				}catch(NumberFormatException ex) {
		   					System.out.println("ID post non valido");
		   					continue;
		   				}
		   				//errore  0 -> OK il post esiste ed è nel feed
		   				//errore -1 -> il post non esiste
		   				//errore -2 -> il post esiste ma è un proprio post
		   				//errore -3 -> il post esiste ma non è nel feed

		   				try {
		   					String idPost = dati[1];
		   					String content = "rewin," + myUsername +"," + idPost;
		   					sendBuffer.clear();
							sendBuffer.put(content.getBytes());
							//Invio al server per lettura
							send(sendBuffer, winsome);
							//Leggo responso
							receive(smallBuffer, winsome, SIZE_OF_INT);
							int error = smallBuffer.flip().getInt();
							switch(error) {
								case -1:
									System.out.println("Il post non esiste");
									break;
								case -2:
									System.out.println("Non puoi fare il rewin dei tuoi post!");
									break;
								case -3:
									System.out.println("Il post non è nel tuo feed");
									break;
								case -4:
									System.out.println("Hai già fatto il rewin di questo post!");
									break;
								default:
									System.out.println("Rewin effettuato, "
											+ "ora puoi trovare il post anche nel tuo blog.");								
							}
															
		   				}catch (IOException ex) {
		   					System.out.println("Errore di comunicazione col server: "+
		   					ex.getMessage());	
		   				}
		   			}
		   			
		   			/**************** RATE POST ***********************/
		   			else if(dati[0].equals("rate")) {
			   				if(!loggedIn) {
			   					System.out.println("Devi prima effettuare il login");
			   					continue;
			   				}
			   				if(dati.length != 3) {
			   					System.out.println("Formato errato, esegui come: rate <idPost> <vote>");
			   					continue;
			   				}
			   				long idLong;
			   				try{
			   					idLong = Long.parseLong(dati[1]);
			   				}catch(NumberFormatException ex) {
			   					System.out.println("ID post non valido");
			   					continue;
			   				}
			   				String voto = dati[2];
			   				if( !voto.equals("-1") && !voto.equals("+1") ) {
			   					System.out.println("Formato voto non valido, voti ammessi: +1/-1");
			   					continue;
			   				}
			   				
			   				try {
			   					String idPost = dati[1];
			   					String content = "rate," + myUsername +"," + idPost + "," + voto;
			   					sendBuffer.clear();
								sendBuffer.put(content.getBytes());
								//Invio al server per lettura
								send(sendBuffer, winsome);
								//Leggo responso
								receive(smallBuffer, winsome, SIZE_OF_INT);
								int error = smallBuffer.flip().getInt();
								switch(error) {
									case -1:
										System.out.println("Il post non esiste");
										break;
									case -2:
										System.out.println("Non puoi votare i tuoi post!");
										break;
									case -3:
										System.out.println("Il post non è nel tuo feed");
										break;
									case -4:
										System.out.println("Hai già votato questo post!");
										break;
									default:
										System.out.println("Voto registrato correttamente");
										//Rimuovo il post dalla cache se l'ho votato
										Iterator<Post> iter = myBlogCache.iterator();
										while (iter.hasNext()) {
										    Post p = iter.next();
										    if (p.getId() == idLong)
										        iter.remove();
										}
			   					
								}	
			   				}catch (IOException ex) {
			   					System.out.println("Errore di comunicazione col server: "+
			   					ex.getMessage());	
			   				}
			   			}
		   				
		   			
		   			/**************** COMMENT POST ***********************/
		   			else if(dati[0].equals("comment")) {
			   				if(!loggedIn) {
			   					System.out.println("Devi prima effettuare il login");
			   					continue;
			   				}
			   				if(dati.length < 3) {
			   					System.out.println("Formato errato, esegui come: comment <idPost> <comment>\n"
			   							+ "Inserisci il commento tra virgolette.");
			   					continue;
			   				}
			   				long idLong;
			   				try{
			   					idLong = Long.parseLong(dati[1]);
			   				}catch(NumberFormatException ex) {
			   					System.out.println("ID post non valido");
			   					continue;
			   				}
			   				String[] str =  messaggioDaInviare.split("\"");
			   				if(str.length != 2 || messaggioDaInviare.charAt(messaggioDaInviare.length()-1) != '\"') {
			   					System.out.println("Formato errato, esegui come: comment <idPost> <comment>\n"
			   							+ "Inserisci il commento tra virgolette.");
			   					continue;
			   				}
			   				String commento = str[1];
			   				int commentLength = commento.length();
			   				if(commentLength > 50) {
			   					System.out.println("Il tuo commento ha " + 
			   							commentLength + 
			   							" caratteri.\n Limite di 50 caratteri raggiunto, riprova.");
			   					continue;
			   				}
			   				
			   				try {
			   					//Invio al server un buffer del tipo
			   					//comment,myUsername,idPost,[lunghezzaCommento]commento
			   					String idPost = dati[1];
			   					String content = "comment," + myUsername +"," + idPost + ",";
			   					sendBuffer.clear();
								sendBuffer.put(content.getBytes());
								sendBuffer.putInt(commentLength);
								sendBuffer.put(commento.getBytes());
								//Invio al server per lettura
								send(sendBuffer, winsome);
								//Leggo responso
								receive(smallBuffer, winsome, SIZE_OF_INT);
								int error = smallBuffer.flip().getInt();
								switch(error) {
									case -1:
										System.out.println("Il post non esiste");
										break;
									case -2:
										System.out.println("Non puoi commentare i tuoi post!");
										break;
									case -3:
										System.out.println("Il post non è nel tuo feed");
										break;
									
									default:
										System.out.println("Commento aggiunto correttamente");	
										//Rimuovo il post dalla cache se l'ho commentato
										Iterator<Post> iter = myBlogCache.iterator();
										while (iter.hasNext()) {
										    Post p = iter.next();
										    if (p.getId() == idLong)
										        iter.remove();
										}
								}	
			   				}catch (IOException ex) {
			   					System.out.println("Errore di comunicazione col server: "+
			   					ex.getMessage());	
			   					continue;
			   				}
			   			}
		   			
		   			
		   			/******************  WALLET **************************/
		   			else if(dati[0].equals("wallet") && dati.length<= 1) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 1) {
		   					System.out.println("Formato errato, esegui come: wallet");
		   					continue;
		   				}
		   			try {	//Invio la richiesta
		   				String content = "wallet," + myUsername;
		   				sendBuffer.clear();
		   				sendBuffer.put(content.getBytes());
		   				send(sendBuffer, winsome);
		   				
		   				ByteBuffer receiveBuffer = ByteBuffer.allocate(12); //double+int
		   				receive(receiveBuffer, winsome, 12);
		   				receiveBuffer.flip();
		   				double myTotal = receiveBuffer.getDouble();
		   				int numberOfTransactions = receiveBuffer.getInt();
		   				receiveBuffer = ByteBuffer.allocate(8+19);
		   				byte[] receivedData = new byte[19];
		   				//Preparo la stampa
						System.out.println("|------------ IL TUO PORTAFOGLIO ------------|");
						System.out.println("| totale:   "+ myTotal);
						System.out.println("|--------------------------------------------|");
		   				for(int i = 0; i < numberOfTransactions; i++) {
							//Mi metto in attesa di ricevere la coppia Valore,Transazione
							receive(receiveBuffer, winsome, 8+19);
							receiveBuffer.flip();
							double movimento = receiveBuffer.getDouble();
							receiveBuffer.get(receivedData);
		   					
		   					String timestamp = 
		   							new String(receivedData);
		   					System.out.println("| " + timestamp + " - Reward: +" + movimento);
						}
		   				if(numberOfTransactions == 0)
							System.out.println("Nessuna transazione, interagisci con la community");
						System.out.println("|____________________________________________|");	
						continue;
		   			}catch(IOException ex) {
		   				System.out.println("Errore di comunicazione col server: "+
			   				ex.getMessage());
		   				continue;
		   			}
						
		   			}
		   			else if(dati[0].equals("wallet") && dati[1].equals("btc")) {
		   				if(!loggedIn) {
		   					System.out.println("Devi prima effettuare il login");
		   					continue;
		   				}
		   				if(dati.length != 2) {
		   					System.out.println("Formato errato, esegui come: wallet btc");
		   					continue;
		   				}
		   			try {
		   				//Invio la richiesta
		   				String content = "walletBtc," + myUsername;
		   				sendBuffer.clear();
		   				sendBuffer.put(content.getBytes());
		   				send(sendBuffer, winsome);
		   				//Ricevo il saldo 
		   				ByteBuffer receiveBuffer = ByteBuffer.allocate(SIZE_OF_DOUBLE); 
		   				receive(receiveBuffer, winsome, SIZE_OF_DOUBLE);
		   				receiveBuffer.flip();
		   				double myTotal = receiveBuffer.getDouble();
		   				System.out.println("Il valore del tuo portafoglio in bitcoin è: " + myTotal);
		   				
						continue;
		   			}catch(IOException ex) {
		   				System.out.println("Errore di comunicazione col server: "+
			   				ex.getMessage());
		   				continue;
		   				}
						
		   			}
		   			
		   			
		   			else if(dati[0].equals("help")) {
		   				
		   				if(dati.length != 1) {
		   					System.out.println("Formato errato, esegui come: help");
		   					continue;
		   				}
		   				
		   				System.out.println("Azioni disponibili:");
		   				System.out.print("register <username> <password> <tags>"
		   						+ "\n	Registra una nuova utenza memorizzando "
		   						+ "\n	username, password e massimo 5 tags."
		   						+"\n\n login <username> <password>"
		   						+ "\n	Effettua il login a Winsome"
		   						+ "\n\n logout"
		   						+ "\n	Esci da Winsome"
		   						+ "\n\n list users"
		   						+ "\n	Visualizza gli utenti di Winsome che"
		   						+ "\n	hanno i tuoi stessi interessi"
		   						+ "\n\n list followers"
		   						+ "\n	Visualizza gli utenti che ti seguono"
		   						+ "\n\n list following"
		   						+ "\n	Visualizza gli utenti che stai seguendo"
		   						+ "\n\n follow <username>"
		   						+ "\n	Segui l'utente"
		   						+ "\n\n unfollow <username>"
		   						+ "\n	Smetti di seguire l'utente"
		   						+ "\n\n blog"
		   						+ "\n	Visualizza il tuo blog"
		   						+ "\n\n post <title> <content>"
		   						+ "\n	Pubblica un nuovo post"
		   						+ "\n\n delete <idPost>"
		   						+ "\n	Cancella il tuo post o il tuo rewin"
		   						+ "\n\n rewin <idPost>"
		   						+ "\n 	Pubblica nel tuo blog il post di"
		   						+ "\n	un altro utente che segui"
		   						+ "\n\n show feed"
		   						+ "\n	Visualizza i post di tutti gli utenti"
		   						+ "\n	che segui"
		   						+ "\n\n show post <idPost>"
		   						+ "\n	Visualizza il post"
		   						+ "\n\n rate post <idPost> -1"
		   						+ "\n	Metti 'non mi piace' al post"
		   						+ "\n\n rate post <idPost> +1"
		   						+ "\n	Metti 'mi piace' al post"
		   						+ "\n\n comment <idPost> <comment>"
		   						+ "\n	Commenta il post"	
		   						+ "\n\n wallet"
		   						+ "\n	Mostra le info del tuo portafoglio"
		   						+ "\n\n wallet btc"
		   						+ "\n	Mostra il valore del tuo portafoglio"
		   						+ "\n	in bitcoin\n"
		   						+ "\n\n Digita 'quit' per interrompere il client\n"
		   						
		   						);
		   				
		   			}
	
		   			/*RICHIESTA DI STOP*/
		   			else if(dati[0].equals("quit")) {
		   					if(dati.length != 1) 
		   						System.out.println("Formato errato, esegui come: quit");
		   					winsome.close();
		   					socket.close();
		   					keyboard.close();
		   					System.out.println("Terminato");
		   					return;
					}	
		   			
		   			/*COMANDO NON RICONOSCIUTO*/
		   			else {
		   				System.out.println("Comando non riconosciuto."
		   						+ " Per la lista dei comandi disponibili digitare 'help'");
		   				continue;
		   			}
		   		}
		   		 catch(RemoteException ex) {
					ex.printStackTrace();
					continue;
				}catch(UsernameAlreadyInUseException ex) {
					System.out.println(ex.getMessage());
					continue;	
				}catch (Exception e) {
					e.printStackTrace();
					System.out.println("Problemi lato server:" + e.getMessage());
					continue;
				}
		   	}
		}
		catch(ConnectException ex) {
			System.out.println("Server irraggiungibile, riprovare - "+ ex.getMessage());
			return;
		}
		catch(Exception ex) {
			System.out.println("Errore: " + ex.getMessage());
			return;
		}
	}

}

	
