package lab3;

public class Activity {
	private String name;
	private int id, resource, amount;
	
	Activity(String name, int a, int b, int c) {
		this.setName(name);
		this.setId(a);
		this.setResource(b);
		this.setAmount(c);
	}
	
	public void printSelf() {
		System.out.printf("%s: %d | %d | %d\n", name, id, resource, amount);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getResource() {
		return resource;
	}

	public void setResource(int resource) {
		this.resource = resource;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

}
