package SendTools;

import java.io.IOException;
import java.io.InputStream;

import PacketConstructor.Manifest;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketType;
import PacketConstructor.PacketWriter;

public class InputStreamBuilder 
{
	private InputStream stream;
	private int block_size;
	private long index;
	private PacketType type;
	private PacketHeader p_header;
	private PacketWriter packet;
	private long id;
	
	public InputStreamBuilder(long id, InputStream stream, int block_size, long index_start, PacketType type)
	{
		if(stream==null)
			throw new IllegalArgumentException();
		if(block_size<=0)
			throw new IllegalArgumentException();
		
		this.id = id;
		this.stream = stream;
		this.block_size = block_size;
		this.index = index_start;
		this.type = type;
		
		this.packet = new PacketWriter(block_size);
	}
	
	public InputStreamBuilder(Manifest manifest, InputStream stream, int block_size, long index_start, PacketType type)
	{
		this(manifest.id, stream, block_size, index_start, type);
	}
	
	public PacketType getPacketsType()
	{
		return type;
	}
	
	public int getBlockSize()
	{
		return block_size;
	}
	
	public long getIndex()
	{
		return index;
	}
	
	public boolean closeStream()
	{
		try {
			stream.close();
			return true;
		} catch (IOException e) 
		{
			return false;
		}
	}
	
	public byte[] getNextPacket() throws IOException
	{
		//update index
		byte[] data = getNextPacket(index);
		index++;
		
		return data;
	}
	
	public byte[] getNextPacket(long index) throws IOException
	{
		//read input data
		int number_byte_read = packet.writeData(stream);
		if(number_byte_read==-1)
		{
			return null;
		}
		
		//read header bytes
		p_header =  new PacketHeader(id, index, number_byte_read, type);
		byte[] b_header = p_header.writeExternal();
		packet.writeHeader(b_header);
		
		//compute checksum
		packet.writeChecksum();
		System.out.println("send packet type="+type+", index="+index);
		return packet.getBuffer();
	}
}
