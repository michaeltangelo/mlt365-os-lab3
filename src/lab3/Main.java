package lab3;

import java.io.FileNotFoundException;

public class Main {
	
	
	public static void main(String[] args) throws FileNotFoundException {
		String file = "";
		if (args.length != 1) {
			System.out.println("An input file is required.");
			System.exit(1);
		}
		else {
			file = args[0];
		}
		
		Lab3 orm = new ORM(file);
		Lab3 bankers = new Bankers(file);
		orm.exec();
		bankers.exec();
		System.out.println("\n\n\n\n\n");
		System.out.println("-----------");
		System.out.println("ORM RESULTS");
		System.out.println("-----------");
		orm.manager.printResults();
		
		System.out.println("--------------");
		System.out.println("BANKER RESULTS");
		System.out.println("--------------");
		bankers.manager.printResults();

	}

}
