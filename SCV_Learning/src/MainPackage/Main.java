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
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for(int i = 0; i < 11; i++) {
			bank.createAccount(vip);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(bank.getLog());
	}
	
}
