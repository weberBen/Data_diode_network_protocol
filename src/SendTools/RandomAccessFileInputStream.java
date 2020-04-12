package SendTools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileInputStream extends RandomAccessInputStream
{
	private RandomAccessFile file;
	
	public RandomAccessFileInputStream(String filename) throws FileNotFoundException
	{
		file = new RandomAccessFile(filename, "r");
	}
	
	public void seek(long pos) throws IOException
	{
		file.seek(pos);
	}
	
	public int read() throws IOException
	{
		return file.read();
	}
	
	public int read(byte[] buffer) throws IOException
	{
		return file.read(buffer);
	}
	
	public int read(byte[] buffer, int offset, int len) throws IOException
	{
		return file.read(buffer, offset, len);
	}
	
	public void close() throws IOException
	{
		file.close();
	}
}
