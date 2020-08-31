package MainPackage;

public class Main {

	
	public static void main(String[] args) {
		Account account = new Account("123456-1234567", "홍길동", 1000);
		VipAccount vip = new VipAccount(account, 5);
//		
//		person.addMoney(-5000);
//		
//		System.out.println(person.toString());
//		System.out.println(vip.toString());
		
		Bank bank = new Bank();
		
		for(int i = 0; i < 10; i++) {
			bank.createAccount(account);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		bank.createAccount(vip);
		
		System.out.println(bank.getLog());
	}
	
}
