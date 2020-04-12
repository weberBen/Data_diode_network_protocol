package PacketConstructor;

public class PacketData 
{
	public final PacketHeader header;
	public final byte[] data;
	
	public PacketData(PacketHeader header, byte[] data)
	{
		if(header==null || data==null || header.getLength()!=data.length)
			throw new IllegalArgumentException();
		
		this.header = header;
		this.data = data;
	}
}
