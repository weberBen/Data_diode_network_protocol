package UserInterface;

import java.util.Scanner;

import Exceptions.SaveObjectException;

public class MenuItem 
{
	public final String name;
	public final String info;
	public final String description;
	public final MenuItemInterface itf;
	protected static final String TREE_SEPARATOR = " > ";
	
	
	public MenuItem(String name,  String description, String info, MenuItemInterface itf)
	{
		this.name = name;
		this.info = info;
		this.description = description;
		this.itf = itf;
	}
	
	public MenuItem(String name, MenuItemInterface itf)
	{
		this(name, null, null, itf);
	}
	
	protected void addPatern(StringBuffer string, char toAdd, int count)
	{
		if(string==null)
			return;
		
		for(int k=0; k<count; k++)
		{
			string.append(toAdd);
		}
	}
	
	private String itemToString()
	{
		String output = "\t"+name.toUpperCase();
		if(description!=null)
		{
			output+="\n* Description : "+description;
		}
		if(info!=null)
		{
			output+="\n* Additional information : "+info;
		}
		
		String value = itf.prevValue();
		
		output+="\n* Actual value : "+value;
		
		return output;
	}
	
	private String toString(String path, int nb_indent)
	{
		int width = 0;
		String val = itemToString();
		String[] lines = val.split("\n");
		String goBack_txt = toText(getGoBackTxt());
		String goTo_txt = toText(getGoToTxt());
		String exit_txt = toText(getExitTxt());
		
		StringBuffer output = new StringBuffer();
		
		if(path!=null)
		{
			addPatern(output, '\t', nb_indent);
			output.append(path);
			output.append("\n");
		}
		
		addPatern(output, '\t', nb_indent);
		addPatern(output, '-', 10);
		output.append("\n");
		
		for(String line : lines)
		{
			addPatern(output, '\t', nb_indent);
			output.append("|");
			output.append(line);
			output.append("\n");
		}
		
		addPatern(output, '\t', nb_indent);
		output.append("|->");
		output.append(goBack_txt + "    " + goTo_txt + "    " + exit_txt);
		output.append("\n");
		
		return output.toString();
	}
	
	protected String getFormatedLine(int nb_indent, String val)
	{
		StringBuffer string = new StringBuffer();
		
		for(int k=0; k<nb_indent; k++)
		{
			string.append("\t");
		}
		string.append("|");
		string.append(val);
		
		return string.toString();
	}
	
	protected String[] getGoBackTxt()
	{
		return new String[] {"(-1)", "GO BACK"};
	}
	
	protected String[] getGoToTxt()
	{
		return new String[] {"(-2)", "GO TO"};
	}
	
	protected String[] getExitTxt()
	{
		return new String[] {"(-3)", "EXIT"};
	}
	
	protected String toText(String[] columns)
	{
		StringBuffer output = new StringBuffer();
		
		for(String elem : columns)
		{
			output.append(elem);
			addPatern(output, ' ', 3);
		}
		
		return output.toString();
	}
	
	protected int gotTo(int id, Scanner input, int nb_indent)
	{
		int val = -1;
		String line;
		while(val<0 || val>id)
		{
			System.console().writer().println(getFormatedLine(nb_indent, "Enter the index of the item to go back to (from 0 to "+id+") : "));
			line = input.nextLine();
			
			try
			{
				val = Integer.parseInt(line);
			}catch(NumberFormatException e){}
			
		}
		
		return val;
	}
	
	public int show(String path, int id, int nb_indent)
	{
		path = path + TREE_SEPARATOR + name.toUpperCase() + " ("+id+")";
		
		Scanner input = new Scanner(System.in);
		String item = toString(path, nb_indent);
		System.console().writer().println(item);
		
		while(true)
		{
				String line = input.nextLine();
				int i;
				try
				{
					i = Integer.parseInt(line);
					
					if(i==-1)
						break ;
					else if(i==-2)
					{
						int goTo_id = gotTo(id, input, nb_indent);
						if(goTo_id==id)
						{
							System.console().writer().println(item);
							continue;
						}
						
						return goTo_id;
					}else if(i==-3)
					{
						return -1;
					}
				}catch(NumberFormatException e){}
				
			try
			{
				itf.applyChange(line);
				
			}catch(IllegalArgumentException e)
			{
				if(e.getMessage()==null)
					System.console().writer().println(getFormatedLine(nb_indent, "! Invalid arguments !"));
				else
					System.console().writer().println(getFormatedLine(nb_indent, "! Invalid arguments : "+e.getMessage()));
				
				continue;
			}catch(SaveObjectException e)
			{
				System.console().writer().println(getFormatedLine(nb_indent, "! Cannot save changes : "+e.getStackTrace()));
				continue;
			}
			break;
		}
		
		return id;
	}
	
	public int show()
	{
		return show(null, 0, 0);
	}
}

