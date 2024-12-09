import java.util.Calendar;

public class Comment {
	
	private String autore;
	private Calendar timestamp;
	private String contenuto;
	
	public Comment(String contenuto, String autore) {
		this.timestamp = Calendar.getInstance();
		this.autore = autore;
		this.contenuto = contenuto;
	}
	
	public boolean equals(Comment toCheck) {
		
		if(this.autore.equals(toCheck.autore)
			&&
			this.contenuto.equals(toCheck.contenuto)
			&&
			this.timestamp.equals(toCheck.timestamp)
			)
			return true;
		else
			return false;	
	}
	
	public String getAutore() {
		return this.autore;
	}
	public Calendar getTimestamp() {
		return timestamp;
	}
	public String getContenuto() {
		return contenuto;
	}

}
