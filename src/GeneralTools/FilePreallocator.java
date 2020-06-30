package GeneralTools;

import java.io.File;
import java.io.IOException;

public class FilePreallocator 
{
	static 
	{ 
		String libname = "libmaLib.so";
		
		String tmp = System.getProperty("java.class.path");
		String[] paths = tmp.split(":");
		
		for(String path : paths)
		{
			try
			{
				tmp = new File(path).getParent();
				tmp = tmp + File.separator + "jni"  + File.separator + libname;
				
				System.load(tmp);
				
				break;
			}catch(UnsatisfiedLinkError e)
			{
				continue;
			}
		}
	}
	
	
	
	public void preallocate(File file, long size) throws IOException
	{
		if (file==null)
			throw new IllegalArgumentException();
		
		preallocate(file.getAbsolutePath(), size); 
	}
	
	public void preallocate(String filename, long size) throws IOException
	{
		if(filename==null)
			throw new IllegalArgumentException();
		
		_preallocate(filename, size);
	}
	
	private native void _preallocate(String file, long size) throws IOException;
}
