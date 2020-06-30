package GeneralTools;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class Serialization 
{
	public static byte[] serialize(Object obj)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try 
		{
		  out = new ObjectOutputStream(bos);   
		  out.writeObject(obj);
		  out.flush();
		  
		  return bos.toByteArray();
		}catch (IOException ex) 
	    {
			// ignore close exception
	    }finally
	    {
	    	try{bos.close();} catch (IOException ex) {}
	    }
		
		
		return null;
	}
	
	
	public static Object deserialize(byte[] byte_array)
	{
		if(byte_array==null)
			return null;
		
		return deserialize(byte_array, 0, byte_array.length);
	}
	
	public static Object deserialize(byte[] byte_array, int offset, int lenght)
	{
		if(byte_array==null)
			return null;
		
		ByteArrayInputStream bis = new ByteArrayInputStream(byte_array, offset, lenght);
		ObjectInput in = null;
		try {
		  in = new ObjectInputStream(bis);
		  return in.readObject(); 
		  
		}catch(IOException ex)
		{
			
		}
		catch(ClassNotFoundException ex)
		{
			
		}
		finally
	    {
	    	try
	    	{
	    		if(in!=null)
	    			in.close();
	    		
	    	} catch (IOException ex) {}
	    }
		
		return null;
	}
	
	public static byte[] longToBytes(long x) 
	{
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(x);
	    return buffer.array();
	}
	
	public static long bytesToLong(byte[] bytes) 
	{
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}
	
	public static byte[] intToBytes(int x) 
	{
	    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
	    buffer.putInt(x);
	    return buffer.array();
	}

	public static int bytesToInt(byte[] bytes) 
	{
	    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getInt();
	}
	
	public static String bytesArrayToString(byte[] data)
	{
		if(data==null)
			return null;
		
		try 
		{
			return new String(data, "UTF-8");
		}catch(UnsupportedEncodingException e) 
		{
			return null;
		}
	}
	
}
