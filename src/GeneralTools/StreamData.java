package GeneralTools;

import java.io.InputStream;

public class StreamData 
{
	public static final int RANDOM_ACCESS_STREAM = 0;
	public static final int CONTINUE_STREAM = 1;
	
	private InputStream stream;
	private long length;
	private int type;
	
	public StreamData(InputStream stream, long length, int type)
	{
		this.stream = stream;
		this.length = length;
		this.type = type;
	}
	
	public InputStream getStream()
	{
		return stream;
	}
	
	public long getLength()
	{
		return length;
	}
	
	public int getType()
	{
		return type;
	}
}
