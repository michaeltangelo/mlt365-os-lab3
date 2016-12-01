package lab3;

import java.util.ArrayList;
import java.util.LinkedList;

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
	
	public void addActivity(Activity a) {
		activities.add(a);
	}
	
	public boolean processNextActivity() {
		// if computing
		if (state == 4) {
			System.out.printf("Task %d is computing (%d of %d cycles)\n", id, totalComputeCycles-computeCyclesRemaining, totalComputeCycles);
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
				System.out.println("Initializing process " + id);
				success = initiate(a);
				break;
			case "request":
				System.out.printf("process #%d requesting %d units of resource %d\n", id-1, a.getAmount(), a.getResource());
				success = request(a);
				break;
			case "release":
				System.out.printf("process #%d releasing %d units of resource %d\n", id-1, a.getAmount(), a.getResource());
				success = release(a);
				break;
			case "terminate":
				System.out.println(id+"terminate case called.");
				success = terminate();
				break;
			case "compute":
				System.out.print("Task " + id + " computes ");
				success = compute(a);
				break;
			default:
				System.out.println("Action unknown. Terminating.");
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
	
	private boolean initiate(Activity a) {
		this.state = 1;
		manager.addClaim(this.id, a.getResource(), a.getAmount());
		return true;
	}
	
	private boolean request(Activity a) {
		// make a request to the manager with process id, resource type, and amount
		boolean granted = manager.requestIfPossible(this.id, a.getResource(), a.getAmount());
		if (granted) {
			System.out.println("Request granted.");
			this.state = 2;
			if (manager.acquire(this.id, a.getResource(), a.getAmount())) {
//				manager.printAllocated();
				return true;
			}
			else return false;
		}
		else {
			if (this.state != 6) manager.addToBlocked(this);
			System.out.println("Request could not be granted. Task is blocked.");
			manager.printAllocated();
			// add to blocked queue if previously blocked
			// set state to blocked
			this.state = 6;
			return false;
		}
	}
	
	public boolean isNextRequestGrantable() {
		Activity a = activities.peek();
		boolean grantable = manager.requestIfPossible(this.id, a.getResource(), a.getAmount());
		return grantable;
	}
	
	private boolean release(Activity a) {
		this.state = 3;
		if (manager.addToFree(this.id, a.getResource(), a.getAmount())) return true;
		else return false;
	}
	
	public boolean terminate() {
		System.out.println("Terminating process " + this.id + " and removing from manager list.");
		this.state = 5;
		manager.queueProcessToRemove(this);
		manager.need[this.id-1] = new int[manager.numResources];
		return true;
	}
	
	private boolean compute(Activity a) {
		this.state = 4;
		this.totalComputeCycles = a.getResource();
		this.computeCyclesRemaining = this.totalComputeCycles-1;
		System.out.printf("(%d of %d cycles)\n", this.totalComputeCycles - this.computeCyclesRemaining, this.totalComputeCycles);
		return true;
	};
	
	public void printSelf() {
		System.out.printf("I am Process #%d and have %d activities remaining.\n", this.id, activities.size());
	}
	
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
	
	public boolean isBlocked() {
		return this.id == 6 ? true : false;
	}
	
	public void abort() {
		System.out.println("Aborting process " + this.id + " and removing from manager list.");
		this.state = 7;
		manager.freeAll(id);
		manager.abortedProcesses.add(this);
		manager.availableProcesses.remove(this);
		manager.processQueue.remove(this);
	}
	
	public boolean hasResources() {
		for (int i = 0; i < manager.allocated[id-1].length; i++) {
			if (manager.allocated[id-1][i] > 0) return true;
		}
		return false;
	}
}
