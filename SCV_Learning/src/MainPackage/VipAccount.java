package MainPackage;

public class VipAccount extends Account {
	
	private final int MAX_RANK = 10;
	
	int rank;
	
	public VipAccount(String RRN, String name, int money, int rank) {
		super(RRN, name, money);
		
		this.setRank(rank);
	}
	
	public VipAccount(Account person, int rank) {
		this(person.getRRN(), person.getName(), person.getMoney(), rank);
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		if(rank > MAX_RANK)
			this.rank = MAX_RANK;
		else if(rank >= 1)
			this.rank = rank;
		else
			this.rank = 1;
	}
	
	@Override
	public String toString() {
		return this.getName() + "[" + this.getRank() + "] - " + this.getMoney();
	}

}
