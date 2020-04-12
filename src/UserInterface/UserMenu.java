package UserInterface;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;

import EnvVariables.Parms;
import Exceptions.SaveObjectException;
import generalTools.Tools;

public class UserMenu 
{	
	private Menu menu;
	
	public UserMenu()
	{
		Menu menu = new Menu("General");
		Menu submenu;
		MenuItem item;
		MenuItemInterface itf;
		
		
		//****************************************************
		
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					int val = Integer.parseInt(line);
					if(val<=0)
						throw new IllegalArgumentException("input value is <=0");
					
					Parms.instance().setBufferFileSize(val);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
				}catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("input value is not an integer");
				}
				
			}
			
			public String prevValue()
			{
				return ""+Parms.instance().getBufferFileSize();
			}
		};
		
		item = new MenuItem("Buffer file size", 
				"set the length of the buffer used to cache file before read/write on the filesystem", 
				"interger greater than 0", 
				itf);
		menu.addItem(item);
				
				
		//****************************************************
		submenu = new Menu("Sender parms", "set parameters for the sender part", null);
		
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					int val = Integer.parseInt(line);
					if(val<=0)
						throw new IllegalArgumentException("input value is <=0");
					
					Parms.instance().sender().setMMUPacketLength(val);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
				}catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("input value is not an integer");
				}
			}
			
			public String prevValue()
			{
				return ""+Parms.instance().sender().getMMUPacketLength();
			}
		};
		
		item = new MenuItem("MMU packet size", 
				"set the maximum packet size to send without slicing it", 
				"interger greater than 0", 
				itf);
		submenu.addItem(item);
		
		
		menu.addItem(submenu);
		
		
		//****************************************************
		submenu = new Menu("Receiver parms", "set parameters for the receiver part", null);
		
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					File file = new File(line);
					if(!file.exists() || (file.exists() && !file.isDirectory()))
						if(!file.mkdir())
						{
							throw new IllegalArgumentException("Cannot create directory");
						}
					
					
					Parms.instance().receiver().setOutPath(line);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
					
				}catch(SecurityException e)
				{
					throw new IllegalArgumentException(e.getMessage());
				}
				
			}
			
			public String prevValue()
			{
				return Parms.instance().receiver().getOutPath();
			}
		};
		
		item = new MenuItem("Output directory", 
				"set the directory where received content will be save", 
				null, 
				itf);
		submenu.addItem(item);
		
		
					//****************************************************
		
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					File file = new File(line);
					if(!file.exists() || (file.exists() && !file.isDirectory()))
						if(!file.mkdir())
						{
							throw new IllegalArgumentException("Cannot create directory");
						}
					
					
					Parms.instance().receiver().setWorkspace(line);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
					
				}catch(SecurityException e)
				{
					throw new IllegalArgumentException(e.getMessage());
				}
				
			}
			
			public String prevValue()
			{
				return Parms.instance().receiver().getWorkspace();
			}
		};
		
		item = new MenuItem("workspace directory", 
				"set the directory where incomming packet will be temporarily stored", 
				null, 
				itf);
		submenu.addItem(item);
		
					//****************************************************
			
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					int val = Integer.parseInt(line);
					if(val==0 || val<=Parms.instance().receiver().getNumberPacketBlock())
						throw new IllegalArgumentException("input value is <"+Parms.instance().receiver().getNumberPacketBlock());
					
					Parms.instance().receiver().setNumberPacketToHold(val);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
				}catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("input value is not an integer");
				}
				
			}
			
			public String prevValue()
			{
				return ""+Parms.instance().receiver().getNumberPacketToHold();
			}
		};
		
		item = new MenuItem("Number packets in memory", 
				"define how many packets must be kept in memory to increase the speed of the reassembling", 
				"interger greater than the number of packets per block and not 0", 
				itf);
		submenu.addItem(item);
		
		
					//****************************************************
			
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					int val = Integer.parseInt(line);
					if(val<=0)
						throw new IllegalArgumentException("input value is <=0");
					
					Parms.instance().receiver().setNumberPacketBlock(val);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
				}catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("input value is not an integer");
				}
				
			}
			
			public String prevValue()
			{
				return ""+Parms.instance().receiver().getNumberPacketBlock();
			}
		};
		
		item = new MenuItem("Number packets per block", 
				"define the length of the block that are used to reassembling the content. \nDuring the reception of packets, packets are kept in memory until a reasonable amount of packets are in the current block, which allow in good circompstances to write the whole content in a single file at once", 
				"interger > 0", 
				itf);
		submenu.addItem(item);
		
		
					//****************************************************
		
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					long val = Long.parseLong(line);
					if(val<=0)
						throw new IllegalArgumentException("input value is <=0");
					
					Parms.instance().receiver().setTimeoutNs(val);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
				}catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("input value is not an long");
				}
				
			}
			
			public String prevValue()
			{
				return ""+Parms.instance().receiver().getNumberPacketBlock();
			}
		};
		
		item = new MenuItem("Timeout", 
				"set the maximum time to wait a packet", 
				"time in nanosecond (long > 0)", 
				itf);
		submenu.addItem(item);
		
		
		menu.addItem(submenu);
		
		
		//****************************************************
		submenu = new Menu("Network configuration", "set the network parameters for sending and receiving", null); 
		
		
		itf = new MenuItemInterface()
		{
			@Override
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				try
				{
					InetAddress ip;
					try
					{
						ip = InetAddress.getByName(line);
						
					}catch(UnknownHostException e)
					{
						throw new IllegalArgumentException(e.getMessage());
					}
					
					Parms.instance().getNetworkConfig().setIp(ip);
					
					try
					{
						Parms.save();
					}catch(JAXBException e)
					{
						throw new SaveObjectException(e);
					}
					
				}catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("input value is not an integer");
				}
				
			}
			
			@Override
			public String prevValue()
			{
				if(Parms.instance().getNetworkConfig().getIp()==null)
					return null;
				
				return Parms.instance().getNetworkConfig().getIp().getHostAddress();
			}
		};
		
		item = new MenuItem("Ip address", 
				itf);
		submenu.addItem(item);
		
		
		itf = new MenuItemInterface()
		{
			public void applyChange(String line) throws IllegalArgumentException, SaveObjectException
			{
				String[] tmp = line.split(" ");
				int[] ports = new int[tmp.length];
				
				for(int i=0; i<tmp.length; i++)
				{
					String t = tmp[i];
					try
					{
						ports[i] = Integer.parseInt(t);
						if(!Tools.isCorrectIpPort(ports[i]))
							throw new NumberFormatException("<<"+t+">>"+" element at position "+i+" is not a valid ip port");
						
					}catch(NumberFormatException e)
					{
						throw new NumberFormatException("<<"+t+">>"+" element at position "+i+" is not an integer");
					}
				}
				
				Parms.instance().getNetworkConfig().setPorts(ports);
				
				try
				{
					Parms.save();
				}catch(JAXBException e)
				{
					throw new SaveObjectException(e);
				}
				
			}
			
			public String prevValue()
			{
				int[] ports = Parms.instance().getNetworkConfig().getPorts();
				
				StringBuffer output = new StringBuffer();
				output.append('[');
				if(ports==null)
				{
					output.append(' ');
				}else
				{
					for(int i=0; i<ports.length-1; i++)
					{
						output.append(""+ports[i]+" ");
					}
					output.append(""+ports[ports.length-1]);
				}
				output.append(']');
				
				return output.toString();
			}
		};
		
		item = new MenuItem("Ip port", 
				null,
				"Ip port must be >0 and separate by a space",
				itf);
		
		submenu.addItem(item);
		
		menu.addItem(submenu);
		
		
		//*********
		this.menu = menu;
	}
	
	public void show()
	{
		menu.show();
	}
}
