package lab3;


// check to see if process correctly goes to aborted processes after aborting
// make sure to remove the aborted process from the BLOCKED queue as WELL as the available queue
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ORM extends Lab3 {
	private int cycleNum, seen[];
	
	ORM(String fileName) throws FileNotFoundException {
		super(fileName);
		init();
		cycleNum = 0;
		seen = new int[manager.numProcesses];
	}
	
	private void tickProcesses() {
		cycleNum++;
		seen = new int[manager.numProcesses];
		boolean deadlock = true;
		System.out.printf("\n\n\n=====================\n===== Cycle %d-%d =====\n=====================\n", cycleNum-1, cycleNum);
		manager.printAvailResources();
		System.out.println();
		
		// Check blocked queue first
		ArrayList<Process> newProcessQueue = new ArrayList<Process>();
		while (!manager.processQueue.isEmpty()) {
			System.out.println("Found a blocked task");
			Process p = manager.processQueue.remove(0);
			if (seen[p.id-1] < 1) { // if process has never been seen before in this cycle
				boolean success = p.processNextActivity();
				if (success) deadlock = false;
				else newProcessQueue.add(p);
				seen[p.id-1] = 1;
			}
			else {
				System.out.println("Process with id: " + p.id + " was seen before. Skipping");
				continue;
			}
		}
		manager.processQueue = newProcessQueue;

		// Then check rest of available processes
		for (int i = 0; i < manager.availableProcesses.size(); i++) {
			Process nextProcess = manager.availableProcesses.get(i);
			if (seen[nextProcess.id-1] < 1) {
//				System.out.println("calling ProcessNextActivity on process with id: " + manager.availableProcesses.get(i).id);
				boolean success = nextProcess.processNextActivity();
				if (success) {
					deadlock = false;
				}
				seen[nextProcess.id-1] = 1;
			}
			else {
				System.out.println("Process with id: " + nextProcess.id + " was seen before. Skipping");
				continue;
			}
		}
		manager.freeBuffer();
		manager.updateAvailableProcesses();
		manager.terminateFinishedProcesses();
		
		if (deadlock) {
			while (manager.isDeadlocked()) {
				System.out.println("Deadlock detected. Aborting lowest task.");
				manager.abortLowestDeadlockedTask();
				manager.printSelf();
			}
//			System.exit(1);
		}
	}
	public void exec() {
		// Go through each process and attempt to complete the activity
		while (manager.terminatedProcesses.size() + manager.abortedProcesses.size() < manager.numProcesses) {
			tickProcesses();
		}
		System.out.println("All processes have terminated.");
		manager.printResults();
	}
}
