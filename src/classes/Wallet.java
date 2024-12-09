import java.util.ArrayList;
import java.util.List;

public class Wallet {

	private double saldo;
	private List<Transaction> movimenti;
	
	public Wallet() {
		this.saldo = 0;
		movimenti = new ArrayList<Transaction>();
	}
	
	public void updateWallet(Transaction t) {
		movimenti.add(t);
		this.saldo += t.getValue();
	}
	
	public double getAmount() {
		return this.saldo;
	}

	public List<Transaction> getTransactions() {
		return this.movimenti;
		
	}
}

