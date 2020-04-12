package SendTools;

public class RandomAccessByteArrayInputStream extends RandomAccessInputStream
{
	private byte[] data;
	private int offset;
	
	public RandomAccessByteArrayInputStream(byte[] data)
	{
		this.data = data;
	}
	
	public void seek(long pos)
	{
		offset = (int)pos;
	}
	
	public int read()
	{
		if(data==null)
			return -1;
		if(offset>data.length)
			return -1;
		
		int b = data[offset];
		offset++;
		
		return b;
	}
	
	private int getRemainingBytes()
	{
		if(data==null)
			return -1;
		if(offset>=data.length)
			return 0;
		
		return data.length-offset;
	}
	
	public int read(byte[] buffer, int off, int len)
	{
		if(data==null)
			return -1;
		
		int end = Math.min(len, getRemainingBytes());
		if(end==0)
			return -1;
		System.out.println("len="+len+"   |  remaining="+getRemainingBytes()+"  | end="+end+"   | offset="+offset+"   | length_="+data.length);
		for(int i=0; i<end;i++)
		{System.out.println("\tindex="+(this.offset + i));
			buffer[off+i] = data[this.offset + i];
		}
		this.offset+=end;
		
		return end;
	}
	
	public int read(byte[] buffer)
	{
		return read(buffer, 0, buffer.length);
	}
	
	public void close()
	{
		data = null;
	}
}
