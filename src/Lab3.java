import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public abstract class Lab3 {
	protected String fileName;
	public ResourceManager manager;

	Lab3(String fileName) {
		this.fileName = fileName;
	}

	// Parse the input and create new instance of a ResourceManager	
	public void init() throws FileNotFoundException {
//		System.out.println("Initializing simulation.");
		File file = new File(fileName);
		Scanner sc = new Scanner(file);
		int numProcesses = Integer.parseInt(sc.next());
		int numResources = Integer.parseInt(sc.next());
		manager = new ResourceManager(numProcesses, numResources);

		// Initialize Resource Manager
		for (int i = 0; i < numResources; i++) {
			int r = Integer.parseInt(sc.next());
			manager.addResource(i, r);
		}
		
		// Create each process and add to manager's process list
		for (int i = 0; i < numProcesses; i++) {
			Process p = new Process(manager, i+1);
			manager.addProcess(p);
		}
		
		// Parse input line by line into an Activity and add to correct index in activities
		while (sc.hasNext()) {
//			System.out.println("Parsing a new activity.");
			String name = sc.next();
			int a = Integer.parseInt(sc.next());
			int b = Integer.parseInt(sc.next());
			int c = Integer.parseInt(sc.next());
			Activity activity = new Activity(name, a, b, c);
//			activity.printSelf();
//			System.out.println("Calling addActivity on manager.");
			manager.addActivity(activity);
		}
		
//		System.out.println("Initialization complete. Printing self.");
//		manager.printSelf();
		sc.close();
	}
	
	// To be implemented by the specific type of algorithm (ORM or Bankers)
	public abstract void exec();
}