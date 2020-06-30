package PacketConstructor;

import Exceptions.ReadingPacketException;
import PacketTools.Checksum;

public class PacketReader extends Packet
{
	private PacketHeader header;
	private boolean corrupted;
	
	public PacketReader() {}
	
	public PacketReader(int offset_buffer, int blokc_size, boolean with_checksum)
	{
		super(offset_buffer, blokc_size, with_checksum);
	}
	
	public PacketReader(int blokc_size, boolean with_checksum)
	{
		this(0, blokc_size, with_checksum);
	}
	
	public PacketReader(int blokc_size)
	{
		this(0, blokc_size, true);
	}
	
	public PacketReader(PacketBufferInfo info)
	{
		super(info);
	}
	
	
	private void setCorruption()
	{
		corrupted = true;
		header = null;
	}
	
	public boolean depack(byte[] buffer)
	{
		return depack(buffer, info.offsetBuffer, info.blockSize, info.withChecksum);
	}
	
	public boolean depack(byte[] buffer, int offset_buffer, int block_size) throws IllegalArgumentException
	{
		return depack(buffer, offset_buffer, block_size, true);
	}
	
	public boolean depack(byte[] buffer, int offset_buffer, int block_size, boolean with_checksum) throws IllegalArgumentException
	{
		if(buffer==null || (buffer.length-offset_buffer)<info.totalLength)
		{
			System.out.println("length huffer="+buffer.length+"  |  offset_buffer="+offset_buffer+" | "+"total size="+info.totalLength);
			throw new IllegalArgumentException();
		}
		this.buffer = buffer;
		
		if(with_checksum)
		{
			if(!Checksum.dataMatchChecksum(buffer, info.offsetChecksum, buffer, info.offsetRaw, info.lenRaw))
			{
				setCorruption();
				return false;
			}
		}
		
		/*
		if(header==null)
		{
			header = PacketHeader.readExternal(buffer, offset_header, len_header);
		}else
		{
			boolean valid = header.set(buffer, offset_header, len_header);//update value
			if(!valid)
			{
				setCorruption();
				return false;
			}
		}*/
		
		header = PacketHeader.readExternal(buffer, info.offsetHeader, info.lenHeader);
		if(header==null)
		{
			setCorruption();
			return false;
		}
		
		return true;
	}
	
	
	public PacketHeader getHeader()
	{
		return header;
	}
	
	public boolean corrupted()
	{
		return corrupted;
	}
	
	public int getLenData()
	{
		if(header==null)
			return 0;
		return header.getLength();
	}
	
	public static Manifest getManifest(byte[] buffer, int offset_buffer, boolean with_checksum)
	{
		PacketBufferInfo info = new PacketBufferInfo(offset_buffer, Manifest.BYTES, with_checksum);
		PacketReader packet = new PacketReader(info);
		
		if(!packet.depack(buffer))
		{
			return null;
		}
		
		return Manifest.readExternal(buffer, packet.getOffsetData());
	}
	
	public static Manifest getManifest(byte[] buffer, int offset_buffer)
	{
		return getManifest(buffer, offset_buffer, true);
	}
	
	public static Manifest getManifest(byte[] buffer)
	{
		return getManifest(buffer, 0, true);
	}
}
