import java.util.ArrayList;
import java.util.LinkedList;

// Process class represents a single process in the algorithm
// It tracks how many cycles it has completed 
// and also maintains its state (listed below)
// All processes have a reference to the (same) ResourceManager instance
public class Process {
	
	// States:
	// 0 - not yet started
	// 1 - initialized / ready
	// 2 - requesting
	// 3 - releasing
	// 4 - calculating
	// 5 - terminated
	// 6 - blocked
	// 7 - aborted
	public int id, state, count, blocked, claimCount;
	public ArrayList<Integer> resources;
	public LinkedList<Activity> activities;
	public ResourceManager manager;
	private int computeCyclesRemaining, totalComputeCycles;

	Process(ResourceManager manager, int id) {
		this.activities = new LinkedList<Activity>();
		this.manager = manager;
		this.id = id;
		this.state = 0;
		this.claimCount = 0;
		this.computeCyclesRemaining = 0;
		this.totalComputeCycles = 0;
	}
	
	// Adds an activity to the activity queue
	public void addActivity(Activity a) {
		activities.add(a);
	}
	
	// Peeks the next activity and processes it appropriately
	public boolean processNextActivity() {
		// if computing
		if (state == 4) {
			// System.out.printf("Task %d is computing (%d of %d cycles)\n", id, totalComputeCycles-computeCyclesRemaining, totalComputeCycles);
			updateCount();
			return true;
		}
		
		if (state == 6) {
			// switch from blocked to available (will be set back to blocked if necessary)
			manager.addToAvailable(this);
		}
		
		if (state == 7) {
			return true;
		}
		
		Activity a = activities.peek();
		if (a == null) {
			terminate();
		}
		boolean success = false;
		switch (a.getName()) {
			case "initiate":
				// System.out.println("Initializing process " + id);
				success = initiate(a);
				break;
			case "request":
				// System.out.printf("process #%d requesting %d units of resource %d\n", id-1, a.getAmount(), a.getResource());
				success = request(a);
				break;
			case "release":
				// System.out.printf("process #%d releasing %d units of resource %d\n", id-1, a.getAmount(), a.getResource());
				success = release(a);
				break;
			case "terminate":
				// System.out.println(id+"terminate case called.");
				success = terminate();
				break;
			case "compute":
				// System.out.print("Task " + id + " computes ");
				success = compute(a);
				break;
			default:
				// System.out.println("Action unknown. Terminating.");
				System.exit(1);
				break;
		}
		
		// Update count based on state
		updateCount();
		
		if (success) {
//			System.out.println("Successful execution, removing activity");
			activities.remove();
//			manager.printSelf();
			return true;
		}
		else {
//			System.out.println("Not successful.");
//			manager.printSelf();
			return false;
		}
	}
	
	// Updates the process's count depending on its state
	public void updateCount() {
		if (state != 5) this.count++;
		if (state == 6) this.blocked++;
		if (state == 4) {
			if (computeCyclesRemaining > 0) computeCyclesRemaining--;
			else {
				state = 1;
			}
		}
	}
	
	// Used in initializing the process
	private boolean initiate(Activity a) {
		this.state = 1;
		manager.addClaim(this.id, a.getResource(), a.getAmount());
		return true;
	}
	
	// Makes a request to the resource manager for a specific number of resources
	// Handles state change depending on whether or not the request is granted
	private boolean request(Activity a) {
		// make a request to the manager with process id, resource type, and amount
		boolean granted = manager.requestIfPossible(this.id, a.getResource(), a.getAmount());
		if (granted) {
			// System.out.println("Request granted.");
			this.state = 2;
			if (manager.acquire(this.id, a.getResource(), a.getAmount())) {
//				manager.printAllocated();
				return true;
			}
			else return false;
		}
		else {
			if (this.state != 6) manager.addToBlocked(this);
			// System.out.println("Request could not be granted. Task is blocked.");
			// manager.printAllocated();
			// add to blocked queue if previously blocked
			// set state to blocked
			this.state = 6;
			return false;
		}
	}
	
	// Checks to see if the next request is possible
	public boolean isNextRequestGrantable() {
		Activity a = activities.peek();
		boolean grantable = manager.requestIfPossible(this.id, a.getResource(), a.getAmount());
		return grantable;
	}
	
	// Handles the release option for an Activity
	private boolean release(Activity a) {
		this.state = 3;
		if (manager.addToFree(this.id, a.getResource(), a.getAmount())) return true;
		else return false;
	}
	
	// Handles the terminate option for an Activity
	public boolean terminate() {
		// System.out.println("Terminating process " + this.id + " and removing from manager list.");
		this.state = 5;
		manager.queueProcessToRemove(this);
		manager.need[this.id-1] = new int[manager.numResources];
		return true;
	}

	// Handles the compute option for an Activity	
	private boolean compute(Activity a) {
		this.state = 4;
		this.totalComputeCycles = a.getResource();
		this.computeCyclesRemaining = this.totalComputeCycles-1;
		// System.out.printf("(%d of %d cycles)\n", this.totalComputeCycles - this.computeCyclesRemaining, this.totalComputeCycles);
		return true;
	};
	
	// Prints self (for debugging)
	public void printSelf() {
		System.out.printf("I am Process #%d and have %d activities remaining.\n", this.id, activities.size());
	}
	
	// Used to print final results of the algorithm
	public int[] printResults() {
		int[] results = new int[2];
		float waitPercentage = (float)this.blocked/this.count;
//		System.out.println("Process with id %" + this.id+ " state: " + this.state);
		if (this.state != 7) {
			System.out.printf("Task %d|\t%d\t%d\t%.0f%%\n",this.id, this.count, this.blocked, waitPercentage*100);
			results[0] = this.count;
			results[1] = this.blocked;
		}
		else {
			System.out.printf("Task %d|\taborted\n", this.id);
			results[0] = -1;
			results[1] = -1;
		}
		return results;
	}
	
	// returns true if the process is currently blocked
	public boolean isBlocked() {
		return this.id == 6 ? true : false;
	}
	
	// Immediately aborts the process
	public void abort() {
		// System.out.println("Aborting process " + this.id + " and removing from manager list.");
		this.state = 7;
		manager.freeAll(id);
		manager.abortedProcesses.add(this);
		manager.availableProcesses.remove(this);
		manager.processQueue.remove(this);
	}
	
	// Checks whether or not the process has allocated resources
	public boolean hasResources() {
		for (int i = 0; i < manager.allocated[id-1].length; i++) {
			if (manager.allocated[id-1][i] > 0) return true;
		}
		return false;
	}
}
