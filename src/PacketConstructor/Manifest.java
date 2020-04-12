package PacketConstructor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.io.UnsupportedEncodingException;

import EnvVariables.Environment;
import Metadata.DataType;
import generalTools.Serialization;
import generalTools.StreamData;
import generalTools.Tools;

public class Manifest
{
	public static final int BYTES = (new Manifest(1,1,1,DataType.Unspecified,1)).writeExternal().length;
	
	public final long id;//fixed size
	public final long dataLength;
	public final int blockSize; //number of bytes
	public final DataType type;
	public final long metaLength;
	
	public Manifest(long id, long data_length, int block_size, DataType type, long metadata_length)
	{
		this.id = id;
		this.dataLength = data_length;
		this.blockSize = block_size;
		this.type = type;
		this.metaLength = metadata_length;
	}
	
	public byte[] writeExternal()
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try 
		{
			ObjectOutputStream out =  new ObjectOutputStream(bos); 
			
			
			out.writeLong(id);
			out.writeLong(dataLength);
	    	out.writeInt(blockSize);
	    	out.writeInt(type.getId());
	    	out.writeLong(metaLength);
	    	
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

	public static Manifest readExternal(byte[] data)
	{
		return readExternal(data, 0);
	}
	
	public static Manifest readExternal(byte[] data, int offset)
    {	
		ByteArrayInputStream bis = new ByteArrayInputStream(data, offset, BYTES);
		ObjectInput in = null;
		try 
		{
		  in = new ObjectInputStream(bis);
		  
		  long id = in.readLong();
		  long data_length = in.readLong();
		  int packet_length = in.readInt();
		  int id_type = in.readInt();
		  long meta_length = in.readLong();
	    	
	    	
		  return new Manifest(id, data_length, packet_length, DataType.getType(id_type), meta_length); 
		  
		}catch(IOException ex)
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
	
	public static Manifest readExternal(String filename)
    {	
		try 
		{
			byte[] data = Tools.getBytesFromFile(filename);
			return Manifest.readExternal(data);
			
		} catch (IOException e) 
		{
			return null;
		}
    }
	
	
	public String toString()
	{
		return "Manifest(id="+id+", type="+type+", block_size="+blockSize+", data_length="+dataLength+", meta_length="+metaLength+")";
	}
	
	
	public StreamData getStream()
	{
		byte[] data = this.writeExternal();
		return new StreamData(new ByteArrayInputStream(data), data.length, StreamData.CONTINUE_STREAM);
	}
}
