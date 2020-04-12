package PacketConstructor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import EnvVariables.Environment;
import PacketTools.Checksum;


public abstract class Packet
{
	
	protected PacketBufferInfo info;
	protected byte[] buffer;
	
	public Packet(int offset_buffer, int block_size, boolean with_checksum)
	{
		this.info = new PacketBufferInfo(offset_buffer, block_size, with_checksum);
	}
	
	protected Packet(int block_size, boolean with_checksum)
	{
		this(0, block_size, with_checksum);
	}
	
	protected Packet(int block_size)
	{
		this(0, block_size, true);
	}
	
	protected Packet(PacketBufferInfo info)
	{
		this.info = info;
	}
	
	
	
	protected Packet() {}
	
	public byte[] getBuffer()
	{
		return buffer;
	}
	
	public int getOffsetBuffer()
	{
		return info.offsetBuffer;
	}
	
	public int getTotalLength()
	{
		return info.totalLength;
	}
	
	public int getOffsetChecksum()
	{
		return info.offsetChecksum;
	}
	
	public int getLenChecksum()
	{
		return info.lenChecksum;
	}
	
	public int getOffsetHeader()
	{
		return info.offsetHeader;
	}
	
	public int getLenHeader()
	{
		return info.lenHeader;
	}
	
	public int getOffsetData()
	{
		return info.offsetData;
	}
	
	public int getOffsetRaw()
	{
		return info.offsetRaw;
	}
	
	public int getLenRaw()
	{
		return info.lenRaw;
	}
	
	public PacketBufferInfo getInfo()
	{
		return info;
	}
	
	public abstract int getLenData();
}

/*
public class Packet 
{
	private PacketHeader header;
	private boolean adjusted;
	private byte[] data;
	
	
	public Packet(PacketHeader header, byte[] data, int offset, int len, boolean adjust)
	{
		this.header = header;
		this.adjusted = adjust;
		if(adjust)
		{
			this.data = ajustData(header, data, offset, len);
		}else
		{
			this.data = data;
		}
	}
	
	public Packet(PacketHeader header, byte[] data, int offset, int len)
	{
		this(header, data, 0, data.length, true);
	}
	
	public Packet(PacketHeader header, byte[] data)
	{
		this(header, data, 0, data.length, true);
	}
	
	public Packet(PacketHeader header, byte[] data, boolean adjust)
	{
		this(header, data, 0, data.length, adjust);
	}
	
	public PacketHeader getHeader()
	{
		return header;
	}
	
	public boolean dataAdjusted()
	{
		return adjusted;
	}
	
	
	private static byte[] ajustData(PacketHeader header, byte[] data, int offset, int len)
	{
		if(data==null)
			return null;
		if(header==null)
			return null;
		
		int size;
		if(header.getLength()==-1)
		{
			if(offset==0 && len==data.length)
				return data;
			
			size = len;
		}else
		{
			size = header.getLength();
		}
		
		return Arrays.copyOfRange(data, offset, offset+size);
	}
	
	public boolean isCorrupted()
	{
		return (header==null || data==null);
	}
}
*/
