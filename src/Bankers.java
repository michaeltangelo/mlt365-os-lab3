import java.io.FileNotFoundException;
import java.util.ArrayList;

// Represents the Bankers algorithm for lab3
public class Bankers extends Lab3 {
	private int cycleNum, seen[];
	
	Bankers(String fileName) throws FileNotFoundException {
		super(fileName);
		init();
		cycleNum = 0;
		seen = new int[manager.numProcesses];
	}
	
	// Called right after initialization
	// Aborts all processes with claims that exceed resource allotment
	private void preAbortOverclaimingProcesses() {
		// manager.printAvailResources();
		for (int i = 0; i < manager.claim.length; i++) {
//			System.out.printf("Process #%d claims: ", i);
			for (int j = 0; j < manager.claim[i].length; j++) {
				Process p = manager.getProcessById(i+1);
				if (manager.claim[i][j] > manager.maxResources[j]) {
					// System.out.println("Aborting process with id#"+(i+1));
					p.abort();
				}
//				System.out.printf("%d ", manager.claim[i][j]);
			}
//			System.out.println();
		}
	}
	
	// Represents one cpu cycle for all processes
	private void tickProcesses() {
		if (cycleNum == 1) {
			preAbortOverclaimingProcesses();
		}
//		if (cycleNum == 10) System.exit(1);
		cycleNum++;
		seen = new int[manager.numProcesses];
		// System.out.printf("\n\n\n=====================\n===== Cycle %d-%d =====\n=====================\n", cycleNum-1, cycleNum);
		// manager.printAvailResources();
		// System.out.println();
		
		// Check blocked queue first
		ArrayList<Process> newProcessQueue = new ArrayList<Process>();
		while (!manager.processQueue.isEmpty()) {
			// System.out.println("Found a blocked task");
			// if it is safe to grant resources to the process (meaning all tasks can finish), process activity
			Process p = manager.processQueue.remove(0);
			if (seen[p.id-1] < 1) {
				if (isSafe(p)) {
					p.processNextActivity();
				}
				else {
					// else block
					p.updateCount();
					if (p.state != 6) manager.addToBlocked(p);
					p.state = 6;
					// System.out.printf("Task %d's is blocked and its request cannot be granted (not safe). So %d is blocked\n", p.id, p.id);
					newProcessQueue.add(p);
				}
				seen[p.id-1] = 1;
			}
			else {
				// System.out.println("Process with id: " + p.id + " was seen before. Skipping");
				continue;
			}
		}
		manager.processQueue = newProcessQueue;

		// Then check rest of available processes
		for (int i = 0; i < manager.availableProcesses.size(); i++) {
			Process p = manager.availableProcesses.get(i);
			if (seen[p.id-1] < 1) {
				if (isSafe(p)) {
					p.processNextActivity();
				}
				else {
					p.updateCount();
					p.blocked++;
					if (p.state != 6) manager.addToBlocked(p);
					p.state = 6;
					// System.out.printf("Task %d's request cannot be granted (not safe). So %d is blocked\n", p.id, p.id);
				}
				seen[p.id-1] = 1;
			}
			else {
				// System.out.println("Process with id: " + p.id + " was seen before. Skipping");
				continue;
			}
		}
		manager.freeBuffer();
		manager.updateAvailableProcesses();
		manager.terminateFinishedProcesses();
	}

	// Runs the entire algorithm and doesn't stop until all processes are either aborted or terminated
	public void exec() {
		// Go through each process and attempt to complete the activity
		
		while (manager.terminatedProcesses.size() + manager.abortedProcesses.size() < manager.numProcesses) {
			tickProcesses();
		}
		// System.out.println("All processes have terminated.");
		// manager.printResults();
	}
	
	// returns true if a request does not exceed the process's claim
	public boolean isRequestUnderClaim(Activity a, Process p) {
		if (manager.claim[p.id-1][a.getResource()-1] < a.getAmount() + manager.allocated[p.id-1][a.getResource()-1]) {
			// System.out.printf("Task with id #%d's request exceeds its claim; aborting.\n", p.id);
			return false;
		}
		return true;
	}
	
	// deadlock detection algorithm
	// Simulates granting a process a resource and then checks
	// if all processes can finish 
	public boolean isSafe(Process p) {
		// System.out.println("Is safe called. Process id: " + p.id);
		// If all processes can finish, then it is safe to grant request
		boolean[] canFinish = new boolean[manager.numProcesses];
		for (int i = 0; i < canFinish.length; i++) canFinish[i] = false;
		
		// Simulate granting the process the resources
		Activity a = p.activities.peek();
		if (a == null) {
			p.terminate();
		}
		if (!a.getName().equals("request")) return true;
		if (!isRequestUnderClaim(a, p)) {
			p.abort();
			return true;
		}
		if (manager.terminatedProcesses.contains(p) || manager.toRemove.contains(p)) {
			// System.out.println("detecting a terminated process with id : " + p.id);
			return true;
		}
		// System.out.printf("Simulating granting process #%d %d unit of resource %d\n", p.id-1, a.getAmount(), a.getResource());
//		int[][] needs = p.manager.need.clone();
//		int[] available = p.manager.availResources.clone();
		int[][] needs = new int[p.manager.numProcesses][p.manager.numResources];
		for (int i = 0; i < p.manager.numProcesses; i++) {
			for (int j = 0; j < p.manager.numResources; j++) {
				needs[i][j] = p.manager.need[i][j];
			}
		}
		int[] available = new int[p.manager.numResources];
		for (int i = 0; i < p.manager.numResources; i++) {
			available[i] = p.manager.availResources[i];
		}
		
		int[][] allocated = new int[p.manager.numProcesses][p.manager.numResources];
		for (int i = 0; i < p.manager.numProcesses; i++) {
			for (int j = 0; j < p.manager.numResources; j++) {
				allocated[i][j] = p.manager.allocated[i][j];
			}
		}
		
		int resourceType = a.getResource();
		int amount = a.getAmount();
		if (available[resourceType-1] < 1) {
			// System.out.println("Not enough resources available to complete request. Blocking.");
			return false;
		}
		available[resourceType-1] -= amount;
		needs[p.id-1][resourceType-1] -= amount;
		allocated[p.id-1][resourceType-1] += amount;
		
//		System.out.println("MANAGER NEEDS");
//		manager.printNeed();
//		System.out.println("DEADLOCK NEEDS");
//		printNeeds(needs);
//		printAvailable(available);
		
		// Check if deadlocked
		boolean deadlocked = true;
		// Loop through all processes
		while (deadlocked) {
//			System.out.println("Deadlock Detection Loop");
//			System.out.println("-----------------------");
			// Update canFinished
			boolean changed = false;
			for (int i = 0; i < canFinish.length; i++) {
//				printNeeds(needs);
//				printAvailable(available);
//				System.out.println();
				// Skip all processes that can 100% finish
				if (canFinish[i]) {
					continue;
				}
				
				
				// check if process can finish
				boolean canComplete = true;
				for (int j = 0; j < needs[i].length; j++) {
					if (needs[i][j] > available[j]) {
//						System.out.println("Process #" + i + " CANNOT complete in current resource state.");
						canComplete = false;
					}
				}
				// if so, set the canFinish flag to true and simulate release of resources
				if (canComplete) {
//					System.out.printf("Process #%d can complete. Releasing it's current resources.\n", i);
//					printNeeds(needs);
					changed = true;
					canFinish[i] = true;
					for (int j = 0; j < needs[i].length; j++) {
						available[j] += allocated[i][j];
						allocated[i][j] = 0;
						needs[i][j] = 0;
					}
//					System.out.print("After Release\n");
//					printNeeds(needs);
//					printAvailable(available);
				}
			}
			// Break loop if all processes can finish
			boolean shouldBreak = true;
			for (int i = 0; i < canFinish.length; i++) {
				if (!canFinish[i]) shouldBreak = false;
			}
			if (shouldBreak) deadlocked = false;
			
			// Signal deadlock is nothing changed and not all processes can finish
			if (!changed) {
				return false;
			}
		}
		// System.out.println("All requests can be satisfied. State is SAFE!");
		// System.out.println();
		return true;
	}
	
	// debug print all process needs
	private void printNeeds(int[][] need) {
		System.out.println("Needs");
		System.out.println("-----");
		for (int i = 0; i < need.length; i++) {
			System.out.print("Process #" + i + " needs: ");
			for (int j = 0; j < need[i].length; j++) {
				System.out.printf("%d\t", need[i][j]);
			}
			System.out.println();
		}
		System.out.println();
	}
	
	// print available resources
	private void printAvailable(int[] availResources) {
		System.out.println("Available Resources");
		System.out.println("-------------------");
		for (int i = 0; i < availResources.length; i++) {
			System.out.printf("Resource %d: %d units\n", i+1, availResources[i]);
		}
	}
}
