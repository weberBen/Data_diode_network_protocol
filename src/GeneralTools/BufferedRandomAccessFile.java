package GeneralTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRandomAccessFile 
{
	private RandomAccessFile file;
	private byte[] buffer;
	private int buffer_offset;
	
	public BufferedRandomAccessFile(File file, String mode, int buffer_size) throws FileNotFoundException
	{
		this.file = new RandomAccessFile(file, mode);
		this.buffer = new byte[buffer_size];
		this.buffer_offset = 0;
	}
	
	public void flush() throws IOException
	{
		if(buffer_offset==0)
			return ;
		
		file.write(buffer, 0, buffer_offset);
		buffer_offset = 0;
	}
	
	public void write(byte[] array) throws IOException
	{
		write(array, 0, array.length);
	}
	
	private void writeToBuffer(int offset, byte val)
	{
		buffer[offset] = val;
		buffer_offset++;
	}
	
	public void write(byte[] array, int offset, int len) throws IOException
	{
		if(buffer_offset+len>=buffer.length)
		{
		
			while(buffer_offset+len>=buffer.length)
			{
				for(int i=0; i<buffer.length; i++)
				{
					writeToBuffer(buffer_offset, array[i+offset]);
				}
				
				flush();
				len -= buffer.length;
				offset += buffer.length;
			}
			
			for(int i=0; i<len; i++)
			{
				writeToBuffer(buffer_offset, array[i+offset]);
			}
			
		}else
		{
			for(int i=0; i<len; i++)
			{
				writeToBuffer(buffer_offset, array[i+offset]);
			}
		}
	}
	
	public void seek(long pos) throws IOException
	{
		long current_pos = file.getFilePointer();
		if(current_pos==pos)
			return;
		
		flush();
		file.seek(pos);
	}
	
	public void close() throws IOException
	{
		flush();
		file.close();
	}
}
