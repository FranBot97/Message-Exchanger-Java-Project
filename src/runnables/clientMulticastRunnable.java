import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class clientMulticastRunnable implements Runnable{
	
	MulticastSocket multiSocket;
	InetAddress group;
		
	public clientMulticastRunnable(int MCASTPORT, InetAddress group) {
		try {
			this.multiSocket = new MulticastSocket(MCASTPORT);
		} catch (IOException e) {
			System.out.println("Impossibile connettersi al multicast: " 
			+ e.getMessage());
			return;
		}
		this.group = group;
	}

	@SuppressWarnings("deprecation")
	public void run() {
		try {
			multiSocket.joinGroup(group);
		
		while(!Thread.currentThread().isInterrupted()) {
			int size = 1024;
			 DatagramPacket packet = new DatagramPacket(new byte[size], size);
	         // Ricevo il pacchetto.
	         multiSocket.receive(packet);
	         //Dato che mi blocco alla receive controllo se sono stato interrotto
	         if(Thread.currentThread().isInterrupted())
	        	 return;
	         System.out.println(new String(packet.getData(), packet.getOffset(),
	              packet.getLength()));
	       }
		} catch (IOException e) {
			System.out.println("Errore comunicazione multicast:"
					+ e.getMessage());
					return;
		}
		}
	}
	

