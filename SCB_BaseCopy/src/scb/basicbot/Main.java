package scb.basicbot;

public class Main {
    public static void main() {
    	try{
            new MyBotModule().run();   		
    	}
    	catch(Exception e) {
    		System.out.println(e.toString());
    		e.printStackTrace();
    	}
    }
}