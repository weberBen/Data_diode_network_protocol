package PacketConstructor;

import java.io.IOException;
import java.io.InputStream;

import PacketTools.Checksum;

public class PacketWriter extends Packet
{
	private int len_data;
	
	public PacketWriter(byte[] buffer, int offset_buffer, int block_size, boolean with_checksum) throws IllegalArgumentException
	{
		super(offset_buffer,  block_size, with_checksum);
		setBuffer(buffer);
	}
	
	private void setBuffer(byte[] buffer)
	{
		if(buffer==null)
		{
			if(info.offsetBuffer!=0)
			{
				throw new IllegalArgumentException();
			}
			this.buffer = new byte[info.totalLength];
		}else
		{
			if((buffer.length-info.offsetBuffer)<info.totalLength)
			{
				throw new IllegalArgumentException();
			}
			this.buffer = buffer;
		}
	}
	
	public PacketWriter(byte[] buffer, int offset_buffer, int block_size)
	{
		this(buffer, offset_buffer, block_size, true);
	}
	
	public PacketWriter(byte[] buffer, int block_size)
	{
		this(buffer, 0, block_size, true);
	}
	
	public PacketWriter(int block_size)
	{
		this(null, 0, block_size, true);
	}
	
	public PacketWriter(byte[] buffer, PacketBufferInfo info)
	{
		super(info);
		setBuffer(buffer);
	}
	
	public PacketWriter(PacketBufferInfo info)
	{
		this(null, info);
	}
	
	private int write(byte[] array, int offset_array, int offset, int len)
	{
		if(len==0)
			return 0;
		
		if(len!=array.length-offset_array)
		{
			throw new IndexOutOfBoundsException();
		}
			
		for(int i=offset, j=offset_array; i<offset+len; i++, j++)
		{
			buffer[i] = array[j];
		}
		
		return len;
	}
	
	
	public int writeCheksum(byte[] array, int offset) throws IndexOutOfBoundsException
	{
		return write(array, offset, info.offsetChecksum, info.lenChecksum);
	}
	
	public int writeCheksum(byte[] array) throws IndexOutOfBoundsException
	{
		return writeCheksum(array, 0);
	}
	
	public int writeChecksum()
	{
		byte[] checksum =  Checksum.compute(buffer, info.offsetRaw, info.lenRaw);
		return writeCheksum(checksum);
	}
	
	public int writeHeader(byte[] array, int offset) throws IndexOutOfBoundsException
	{
		return write(array, offset, info.offsetHeader, info.lenHeader);
	}
	
	public int writeHeader(byte[] array) throws IndexOutOfBoundsException
	{
		return writeHeader(array, 0);
	}
	
	public int writeData(byte[] array, int offset, int len) throws IndexOutOfBoundsException
	{
		if(len>info.blockSize)
		{
			throw new IndexOutOfBoundsException();
		}
		this.len_data = len;
		
		return write(array, offset, info.offsetData, len);
	}
	
	public int writeData(byte[] array) throws IndexOutOfBoundsException
	{
		return writeData(array, 0, array.length);
	}
	
	public int writeData(InputStream stream) throws IOException
	{
		int number_bytes_read = stream.read(buffer, info.offsetData, info.blockSize);
		this.len_data = number_bytes_read;
		
		return number_bytes_read;
	}
	
	public int getLenData()
	{
		return len_data;
	}
}
