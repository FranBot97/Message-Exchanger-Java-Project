import java.text.SimpleDateFormat;
import java.util.Date;

public class Transaction {

	private double value;
	private String timestamp;
	
	
	public Transaction(double value) {
			
		this.value = value;		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.timestamp = df.format(
        		  new Date(System.currentTimeMillis()));
	}


	public double getValue() {
		return this.value;
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
}
