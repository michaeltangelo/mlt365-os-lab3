import java.io.FileNotFoundException;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		// parse command line args
		String file = "";
		if (args.length != 1) {
			System.out.println("An input file is required.");
			System.exit(1);
		}
		else {
			file = args[0];
		}
		
		// Instance of Optimistic Resource Manager
		Lab3 orm = new ORM(file);

		// Instance of Bankers
		Lab3 bankers = new Bankers(file);
		
		orm.exec();
		bankers.exec();
		
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
