import java.util.ArrayList;

// Resource manager class is an all-aware object that
// has and maintains a list of all processes required for the algorithm
public class ResourceManager {
	protected int numProcesses, numResources;
	public ArrayList<Process> availableProcesses, allProcesses, processQueue, terminatedProcesses, abortedProcesses;
	protected int need[][], allocated[][], claim[][], availResources[], maxResources[];
	
	public ArrayList<Process> toRemove;
	private ArrayList<Integer[]> toFree;
	
	ResourceManager(int numProcesses, int numResources) {
		this.numProcesses = numProcesses;
		this.numResources = numResources;
		this.availableProcesses = new ArrayList<Process>();
		this.allProcesses = new ArrayList<Process>();
		this.processQueue = new ArrayList<Process>();
		this.terminatedProcesses = new ArrayList<Process>();
		this.abortedProcesses = new ArrayList<Process>();
		maxResources = new int[numResources];
		availResources = new int[numResources];
		toRemove = new ArrayList<Process>();
		toFree = new ArrayList<Integer[]>();
		
		need = new int[numProcesses][numResources];
		allocated = new int[numProcesses][numResources];
		claim = new int[numProcesses][numResources];
	}
	
	// Is called at the end of an execution tick
	// Removes any process in the toRemove queue
	public void terminateFinishedProcesses() {
		for (int i = 0; i < toRemove.size(); i++) {
			Process p = toRemove.get(i);
			// Set the need array for the process to none
			for (int j = 0; j < need[p.id-1].length; j++) {
				need[p.id-1][j] = 0;
//				allocated[p.id-1][j] = 0;
				availResources[j] += allocated[p.id-1][j];
			}
			terminatedProcesses.add(p);
			removeProcessFromAvailable(p);
		}
		toRemove.clear();
	}
	
	// Called at the end of an execution tick
	// remove blocked processes from the available processes
	public void updateAvailableProcesses() {
		for (int i = 0; i < processQueue.size(); i++) {
			// System.out.println("Removing duplicate process");
			if (!availableProcesses.remove(processQueue.get(i))) {
				// System.out.println("Failed to remove a blocked process from the available queue.");
			}
		}
		for (int i = 0; i < abortedProcesses.size(); i++) {
			availableProcesses.remove(abortedProcesses.get(i));
			processQueue.remove(abortedProcesses.get(i));
		}
	}
	
	// Aborts the lowest numbered deadlock task
	// If the task has no resources, abort it all the same
	public void abortLowestDeadlockedTask() {
		for (int i = 0; i < allProcesses.size(); i++) {
			Process p = allProcesses.get(i);
			if (p.hasResources()) {
				p.abort();
				return;
			}
			else {
				if (p.state != 5 && p.state != 7) {
//					System.out.println("Has no resources but aborting anyway.");
					p.abort();
				}
			}
		}
	}
	
	// Adds a process to the list of available processes
	public void addToAvailable(Process p) {
		// add at correct location
		if (!availableProcesses.contains(p)) availableProcesses.add(p);
	}
	
	// Adds a process to the list of blocked processes
	public void addToBlocked(Process p) {
		if (!processQueue.contains(p)) processQueue.add(p);
	}
	
	// Returns true if the process is deadlocked
	public boolean isDeadlocked() {
		boolean deadlock = true;
		// check if any blocked processes' requests can be Request granted
		for (int i = 0; i < processQueue.size(); i++) {
			if (processQueue.get(i).isNextRequestGrantable()) deadlock = false;
		}
		// then check if any available processes' requests can be granted
		for (int i = 0; i < availableProcesses.size(); i++) {
			if (availableProcesses.get(i).isNextRequestGrantable()) deadlock = false;
		}
		return deadlock;
	}
	
	// For debugging: Prints the needs of a process
	public void printProcessNeeds(Process p) {
		for (int i = 0; i < need[p.id-1].length; i++) {
			System.out.printf("%d\t", need[p.id-1][i]);
		}
		System.out.println();
	}
	public void freeAll(int id) {
		for (int i = 0; i < numResources; i++) {
			int amount = allocated[id-1][i];
			allocated[id-1][i] = 0;
			availResources[i] += amount;
			need[id-1][i] = 0;
		}
	}
	
	// Assumes that the process has been approved to access resources
	public boolean acquire(int id, int resourceType, int amount) {
		allocated[id-1][resourceType-1] += amount;
		availResources[resourceType-1] -= amount;
		need[id-1][resourceType-1] -= amount;
		return true;
	}
	
	// Called at the end of an execution tick
	// Frees all resources from each process in the freeBuffer
	public void freeBuffer() {
//		System.out.println("freeBuffer called.");
		for (int i = 0; i < toFree.size(); i++) {
			Integer[] freeBuffer = toFree.get(i);
			int id = freeBuffer[0];
			int resourceType = freeBuffer[1];
			int amount = freeBuffer[2];
			// System.out.println("Releasing Resources from process " + id + ": " + amount + " of resource " + resourceType);
			allocated[id-1][resourceType-1] -= amount;
			availResources[resourceType-1] += amount;
			need[id-1][resourceType-1] += amount;
		}
		toFree.clear();
	}
	
	// Add a process to the queue of processes to Free
	public boolean addToFree(int id, int resourceType, int amount) {
		// add to buffer to complete at end of tick
		Integer[] newFreeBuffer = new Integer[3];
		newFreeBuffer[0] = id;
		newFreeBuffer[1] = resourceType;
		newFreeBuffer[2] = amount;
		toFree.add(newFreeBuffer);
		return true;
	}

	// Adds a claim to the claim[][] while initializing the resource Manager
	public void addClaim(int id, int resourceType, int amount) {
//		System.out.printf("Adding Process #%d's claim for amount %d of resource %d\n", id, resourceType, amount);
		claim[id-1][resourceType-1] = amount;
		need[id-1][resourceType-1] = amount;
//		printClaim();
	}
	
	// Queues a process to remove at the end of an execution tick
	public void queueProcessToRemove(Process p) {
		toRemove.add(p);
	}
	
	// returns true to a request if there are enough resources to grant
	public boolean requestIfPossible(int id, int resourceType, int amount) {
		// check if request is greater than claim
		if (amount > claim[id-1][resourceType-1]) {
			System.out.println("Requested resource is greater than claim. Request denied.");
			return false;
		}
		// check if request is larger than current available resources
		if (availResources[resourceType-1] < amount) {
			System.out.println("Not enough resources available. Request denied.");
			return false;
		}
		return true;
	}
	
	// Returns an available process with the input id
	public Process getAvailableProcessById(int id) {
		for (int i = 0; i < availableProcesses.size(); i++) {
			if (availableProcesses.get(i).id == id) return availableProcesses.get(i);
		}
		return null;
	}
	
	// Adds a resource while initializing the resource Manager
	public void addResource(int resourceType, int amount) {
		maxResources[resourceType] = amount;
		availResources[resourceType] = amount;
	}
	
	// Adds a process while initializing the resource Manager
	public void addProcess(Process p) {
		availableProcesses.add(p);
		allProcesses.add(p);
	}
	
	// Removes a process from the list of available processes
	public void removeProcessFromAvailable(Process p) {
		if (!availableProcesses.remove(p)) {
			System.out.println("Process p with id: " + p.id + " could not be removed from manager's process list.");
			System.exit(1);
		}
	}
	
	// Returns a process with the input id
	public Process getProcessById(int id) {
//		System.out.println("getProcessById()");
		for (int i = 0; i < availableProcesses.size(); i++) {
			Process p = availableProcesses.get(i);
//			System.out.println("found a process, id is: " + p.id);
			if (p.id == id) return p;
		}
		return null;
	}
	
	// Adds an activity while initializing the resource Manager	
	public void addActivity(Activity activity) {
//		System.out.println("Attempting to find activity with ID: " + activity.getId());
		Process p = getProcessById(activity.getId());
		if (p == null) {
//			System.out.println("Could not find process with id: " + activity.getId());
			System.exit(1);
		}
		else {
//			System.out.println("Added a " + activity.getName() + " activity to process #" + activity.getId());
			p.addActivity(activity);
		}
	}
	
	// Debug print need list
	public void printNeed() {
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
	
	// debug print allocated list
	public void printAllocated() {
		System.out.println("Allocated");
		System.out.println("---------");
		for (int i = 0; i < allocated.length; i++) {
			System.out.print("Process #" + i + " currently has: ");
			for (int j = 0; j < allocated[i].length; j++) {
				System.out.printf("%d\t", allocated[i][j]);
			}
			System.out.println();
		}
		System.out.println();
	}
	
	// debug print all claims
	public void printClaim() {
		System.out.println("Claims");
		System.out.println("------");
		for (int i = 0; i < claim.length; i++) {
			System.out.print("Process #" + i + " requires: ");
			for (int j = 0; j < claim[i].length; j++) {
				System.out.printf("%d\t", claim[i][j]);
			}
			System.out.println();
		}
		System.out.println();
	}

	// debug print the two queues of processes (blocked or available)
	public void printProcesses() {
		System.out.println("Processes");
		System.out.println("---------");
		System.out.println("Blocked");
		System.out.println("-------");
		for (int i = 0; i < processQueue.size(); i++) {
			processQueue.get(i).printSelf();
		}
		System.out.println("Available");
		System.out.println("---------");
		for (int i = 0; i < availableProcesses.size(); i++) {
			availableProcesses.get(i).printSelf();
		}
	}
	
	// A verbose debug print of the current state of the resource manager
	public void printSelf() {
		System.out.println("Resource Manager Queues");
		System.out.println("-----------------------");
		printAvailableProcesses();
		printProcessQueue();
		System.out.println("Resource Manager States");
		System.out.println("-----------------------");
		printAllocated();
		printClaim();
		printNeed();
		printAvailResources();
		printProcesses();
		System.out.println("-----------------------");
	}
	
	// prints results for the end of the algorithm
	public void printResults() {
		int[] total = new int[]{0,0};
		for (int i = 0; i < allProcesses.size(); i++) {
			int[] processResults = allProcesses.get(i).printResults();
			// If the process is not aborted
			if (processResults[0] >= 0) {
				total[0] += processResults[0];
				total[1] += processResults[1];
			}
		}
		float waitPercentage = (float)total[1]/total[0];
		System.out.printf("Total|\t%d\t%d\t%.0f%%\n", total[0], total[1], waitPercentage*100);
	}
	
	// debug prints available resources
	public void printAvailResources() {
		System.out.println("Available Resources");
		System.out.println("-------------------");
		for (int i = 0; i < availResources.length; i++) {
			System.out.printf("Resource %d: %d units\n", i+1, availResources[i]);
		}
	}
	
	// debug prints the blocked queue
	public void printProcessQueue() {
		System.out.print("Process Queue       | ");
		for (int i = 0; i < processQueue.size(); i++) {
			System.out.printf("#%d\t", processQueue.get(i).id);
		}
		System.out.println();
	}
	
	// debug prints all terminated processes
	public void printTerminatedProcesses() {
		System.out.print("Process Queue       | ");
		for (int i = 0; i < terminatedProcesses.size(); i++) {
			System.out.printf("#%d\t", terminatedProcesses.get(i).id);
		}
		System.out.println();
	}
	
	// debug prints all available processes
	public void printAvailableProcesses() {
		System.out.print("Available Processes | ");
		for (int i = 0; i < availableProcesses.size(); i++) {
			System.out.printf("#%d\t", availableProcesses.get(i).id);
		}
		System.out.println();
	}
}
