package PacketConstructor;

import PacketTools.Checksum;

public class PacketBufferInfo 
{
	public final int totalLength;
	public final int offsetChecksum;
	public final int lenChecksum;
	public final int offsetHeader;
	public final int lenHeader;
	public final int offsetData;
	public final int offsetRaw;
	public final int lenRaw;
	
	public final int blockSize;
	public final int offsetBuffer;
	public final boolean withChecksum;
	
	private PacketBufferInfo(int totalLength, int offsetChecksum, int lenChecksum, int offsetHeader, 
			int lenHeader, int offsetData, int offsetRaw, int lenRaw, int blockSize, int offsetBuffer, boolean withChecksum) 
	{
		this.totalLength = totalLength;
		this.offsetChecksum = offsetChecksum;
		this.lenChecksum = lenChecksum;
		this.offsetHeader = offsetHeader;
		this.lenHeader = lenHeader;
		this.offsetData = offsetData;
		this.offsetRaw = offsetRaw;
		this.lenRaw = lenRaw;
		this.blockSize = blockSize;
		this.offsetBuffer = offsetBuffer;
		this.withChecksum = withChecksum;
		
	}
	
	public PacketBufferInfo(int offset_buffer, int block_size, boolean with_checksum)
	{
		this.offsetBuffer = offset_buffer;
		this.blockSize = block_size;
		this.totalLength = getSize(block_size, with_checksum);
		
		this.withChecksum = with_checksum;
		if(!with_checksum)
		{
			this.offsetChecksum = 0;
			this.lenChecksum = 0;
		}else
		{
			this.offsetChecksum = this.offsetBuffer;
			this.lenChecksum = Checksum.LENGTH;
		}
		
		this.offsetHeader = this.offsetChecksum + this.lenChecksum;
		this.lenHeader = PacketHeader.BYTES;
		
		this.offsetData = this.offsetHeader + this.lenHeader;
		
		this.offsetRaw = this.offsetChecksum + this.lenChecksum;
		this.lenRaw = this.lenHeader + block_size;
	}
	
	public PacketBufferInfo(int offset_data, int blokc_size)//data only
	{
		this.offsetBuffer = offset_data;
		this.totalLength = blokc_size;
		this.withChecksum = false;
		
		this.offsetChecksum = 0;
		this.lenChecksum = 0;
		
		this.offsetHeader = 0;
		this.lenHeader = 0;
		
		this.offsetRaw = offset_data;
		this.lenRaw = blokc_size;
		
		this.blockSize = blokc_size;
		
		this.offsetData = offset_data;
	}
	
	
	public PacketBufferInfo(int block_size, boolean with_checksum)
	{
		this(0, block_size, with_checksum);
	}
	
	public PacketBufferInfo(int block_size)
	{
		this(0, block_size, true);
	}
	
	public static int getSize(int block_size, boolean with_checksum)
	{
		if(with_checksum)
		{
			return Checksum.LENGTH + PacketHeader.BYTES + block_size;
		}else
		{
			return PacketHeader.BYTES + block_size;
		}
	}
	
	public static int getSize(int block_size)
	{
		return getSize(block_size, true);
	}
}
