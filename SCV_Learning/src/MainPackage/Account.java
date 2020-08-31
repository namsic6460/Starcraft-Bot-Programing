package MainPackage;

public class Account {
	
	private String RRN;
	private String name;
	private int money;
	
	public Account (String RRN, String name, int money) {
		this.setRRN(RRN);
		this.setName(name);
		this.setMoney(money);
	}
	
	public String getRRN() {
		return RRN;
	}
	
	private void setRRN(String RRN) {
		this.RRN = RRN;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getMoney() {
		return money;
	}
	
	public void setMoney(int money) {
		if(money > 0)
			this.money = money;
		else
			this.money = 0;
	}
	
	public void addMoney(int money) {
		this.setMoney(this.getMoney() + money);
	}
	
	@Override
	public String toString() {
		return this.getName() + " - " + this.getMoney();
	}

}
