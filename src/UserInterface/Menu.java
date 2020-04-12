package UserInterface;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Scanner;

public class Menu extends MenuItem
{
	private final ArrayList<MenuItem> items;
	private Scanner input;
	private static final int SPACE_BETWEEN_COLUMNS = 6;
	
	public Menu(String name, String description, String info)
	{
		super(name, description, info, null);
		this.items = new ArrayList<MenuItem>();
		this.input = new Scanner(System.in);
	}	
	
	public Menu(String name)
	{
		this(name, null, null);
	}
	
	public void addItem(MenuItem item)
	{
		if(item==null)
			return;
		
		items.add(item);
	}
	
	private void addItemToDisplay(String[][] columns, int id, MenuItem item)
	{
		columns[0][id] = "("+id+")";
		columns[1][id] = item.name.toUpperCase();
		//columns[2][id] = item.description;
	}
	
	private void addExitToDisplay(String[][] columns)
	{
		String[] tmp = getExitTxt();
		for(int i=0; i<tmp.length; i++)
		{
			columns[i][items.size()+2] = tmp[i];
		}
	}
	
	private void addGoToDisplay(String[][] columns)
	{
		String[] tmp = getGoToTxt();
		for(int i=0; i<tmp.length; i++)
		{
			columns[i][items.size()+1] = tmp[i];
		}
	}
	
	private void addGoBackDisplay(String[][] columns)
	{
		String[] tmp = getGoBackTxt();
		for(int i=0; i<tmp.length; i++)
		{
			columns[i][items.size()] = tmp[i];
		}
	}
	
	public String toString(String path, char limit_pattern, int nb_indent)
	{
		final int size = items.size()+3;
		String[][] columns = new String[2][size];
		int[] h_width = new int[columns.length];
		
		for(int index=0; index<items.size(); index++)
		{
			MenuItem item = items.get(index);
			
			addItemToDisplay(columns, index, item);
			
			for(int i=0; i<columns.length; i++)
			{
				if(columns[i][index]==null)
					continue;
				
				if(columns[i][index].length()>h_width[i])
				{
					h_width[i] = columns[i][index].length();
				}
			}
		}
		addGoBackDisplay(columns);
		for(int i=0; i<columns.length; i++)
		{
			int index = columns.length-1;
			if(columns[i][index]==null)
				continue;
			
			if(columns[i][index].length()>h_width[index])
				h_width[index] = columns[i][index].length();
		}
		addGoToDisplay(columns);
		for(int i=0; i<columns.length; i++)
		{
			int index = columns.length-1;
			if(columns[i][index]==null)
				continue;
			
			if(columns[i][index].length()>h_width[index])
				h_width[index] = columns[i][index].length();
		}
		addExitToDisplay(columns);
		for(int i=0; i<columns.length; i++)
		{
			int index = columns.length-1;
			if(columns[i][index]==null)
				continue;
			
			if(columns[i][index].length()>h_width[index])
				h_width[index] = columns[i][index].length();
		}
		
		//---------------------
		StringBuffer output = new StringBuffer();
		
		if(path!=null)
		{
			addPatern(output, '\t', nb_indent); 
			output.append(path);
			output.append("\n");
		}
		
		int total_width = 2 + SPACE_BETWEEN_COLUMNS;//char pattern at the start and the end, and start begin after some spaces
		for(int i=0; i<h_width.length; i++)
		{
			total_width+=(h_width[i]+SPACE_BETWEEN_COLUMNS);
		}
		
		addPatern(output, '\t', nb_indent);
		addPatern(output, limit_pattern, total_width);
		output.append("\n");
		for(int l=0; l<size; l++)
		{
			addPatern(output, '\t', nb_indent);
			output.append(limit_pattern);
			addPatern(output, ' ', SPACE_BETWEEN_COLUMNS);
			
			for(int c=0; c<columns.length; c++)
			{
				if(columns[c][l]==null)
				{
					addPatern(output, ' ', h_width[c]  + SPACE_BETWEEN_COLUMNS);
				}else
				{
					output.append(columns[c][l]);
					addPatern(output, ' ', (h_width[c]-columns[c][l].length())  + SPACE_BETWEEN_COLUMNS);
				}
			}
			
			output.append(limit_pattern);
			output.append("\n");
		}
		addPatern(output, '\t', nb_indent);
		addPatern(output, limit_pattern, total_width);
		
		if(description!=null)
		{
			output.append("\n");
			addPatern(output, '\t', nb_indent); 
			output.append("Descrption : "+description);
		}
		if(info!=null)
		{
			output.append("\n");
			addPatern(output, '\t', nb_indent); 
			output.append("Additional information : "+info);
			output.append("\n");
		}
		
		return output.toString();
	}
	
	@Override
	public int show(String path, int id, int nb_indent)
	{
		int choice;
		
		if(path==null)
			path = name.toUpperCase()+ " ("+id+")";
		else
			path = path + TREE_SEPARATOR + name.toUpperCase()+ " ("+id+")";
		
		String menu = toString(path, '*', nb_indent);
		
		while(true)
		{
			
			System.console().writer().println(menu);
			System.console().writer().println(getFormatedLine(nb_indent, "Choice : "));
			
			String tmp;
			choice = items.size();
			do
			{
				try
				{
					tmp = input.nextLine();
					choice = Integer.parseInt(tmp);
					break;
				}catch(NumberFormatException e)
				{
					System.console().writer().println(getFormatedLine(nb_indent, "! Invalid choice !"));
					continue;
				}
			}while(choice<-2 || choice>=items.size());
			
			if(choice==-1)
				break;
			else if(choice==-2)
			{
				int goTo_id = gotTo(id, input, nb_indent);
				if(goTo_id==id)
					continue;
				
				return goTo_id;
			}else if(choice==-3)
			{
				return -1;
			}
			
			int goTo_id = items.get(choice).show(path, id+1, nb_indent+1);
			if(goTo_id>=id)
				continue;
			else
				return goTo_id;
		}
		
		return id;
	}
	
	@Override
	public int show()
	{
		return show(null, 0, 0);
	}
}
