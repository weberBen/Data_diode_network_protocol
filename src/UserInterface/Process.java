package UserInterface;

import java.util.HashMap;

public abstract class Process 
{
	private static int count = 0;
	private final int id;
	
	public Process()
	{
		this.id = count;
		count++;
	}
	
	public abstract void start(String parms);
	public abstract void stop();
}

/*class Start extends Process
{
	private HashMap<String, Process> processus;
	
	public Start()
	{
		processus = new HashMap<String, Process>();
		processus.put("menu", );
		processus.put("sender", );
		processus.put("receiver", );
	}
	
	
	public abstract void start()
	public abstract void stop();
}*/