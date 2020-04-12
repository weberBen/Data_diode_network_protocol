package PacketManager;

public class PacketContent 
{
	public final boolean inMemory;
	public final Object content;
	
	public PacketContent(boolean in_memory, Object content)
	{
		this.inMemory = in_memory;
		this.content = content;
	}
	
	public PacketContent(byte[] buffer)
	{
		this(true, buffer);
	}
	
	public PacketContent(String filename)
	{
		this(false, filename);
	}
	
	public byte[] getInMemoryContent()//byte array
	{
		if(!inMemory)
			return null;
		
		return (byte[])content;
	}
	
	public String getOnDiskContent()//filename
	{
		if(inMemory)
			return null;
		
		return (String)content;
	}
}
