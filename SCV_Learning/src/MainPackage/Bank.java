package MainPackage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Bank {
	
	private final int MAX_ACCOUNT = 10;
	
	Date date;
	SimpleDateFormat format;
	
	private Account[] accounts;
	private int size;
	private ArrayList<String> logs;
	
	public Bank() {
		accounts = new Account[MAX_ACCOUNT];
		size = 0;
		logs = new ArrayList<>();
		format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		this.addLog("은행 설립");
		System.out.println("은행이 설립되었습니다!");
	}
	
	public void addLog(String log) {
		date = new Date();
		
		log = format.format(date) + " - " + log;
		this.logs.add(log);
	}
	
	public String getLog() {
		String output = "";
		
		for(String s : this.logs)
			output += s + "\n";
		
		return output;
	}
	
	public boolean createAccount(Account account) {
		if(size < 10) {
			this.accounts[size++] = account;
			this.addLog("계좌 추가 성공 - " + account.toString());
			
			return true;
		}
		
		else {
			if(account instanceof VipAccount) {
				for(int i = MAX_ACCOUNT - 1; i >= 0; i--) {
					if(!(account instanceof VipAccount)) {
						this.addLog("계좌 삭제 - " + this.accounts[i].toString());
						
						this.accounts[i] = account;
						this.addLog("계좌 추가 성공 - " + account.toString());
						
						return true;
					}
				}

				this.addLog("계좌 추가 실패 - " + account.toString());
				return false;
			}
			
			else {
				this.addLog("계좌 추가 실패 - " + account.toString());
				return false;
			}
		}
	}

}
