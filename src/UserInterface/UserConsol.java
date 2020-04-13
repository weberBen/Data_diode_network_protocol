package UserInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class UserConsol
{
	private HashMap<String, ConsoleInterface> actions;
	private Scanner scanner;
	private HashMap<Integer, Process> processus;
	private static final String SEPARATOR = " ";
	private static final String EXIT_COMMAND = "exit";
	
	public UserConsol()
	{
		super();
		actions = new HashMap<String, ConsoleInterface>();
		scanner = new Scanner(System.in);
		processus = new HashMap<Integer, Process>();
	}
	
	public void addAction(String command, ConsoleInterface action)
	{
		if(actions.get(command)!=null)
			throw new IllegalArgumentException();
		
		actions.put(command, action);
	}
	
	private void write(String s)
	{
		System.out.println(">>"+s);
	}
	
	//@Override
	public void start()
	{
		String line;
		String[] elems;
		String command;
		String[] parms;
		ConsoleInterface action;
		
		while(true)
		{
			line = scanner.nextLine();
			elems = line.split(SEPARATOR);
			
			command = elems[0];
			if(elems.length-1>0)
			{
				parms = new String[elems.length-1];
				for(int i=1; i<elems.length; i++)
				{
					parms[i-1] = elems[i];
				}
			}else
			{
				parms = null;
			}
			
			action = actions.get(command);
			if(action==null)
			{
				write("No command named <<"+command+">> found");
			}
			//action()
			
		}
	}
	
}
