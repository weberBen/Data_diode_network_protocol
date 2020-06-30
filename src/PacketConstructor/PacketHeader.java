package PacketConstructor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import EnvVariables.Environment;
import EnvVariables.Environment.PacketEnumType;
import GeneralTools.Serialization;

public class PacketHeader
{	
	public static final int BYTES = (new PacketHeader(0, 1,1,PacketType.Unspecified())).writeExternal().length;
	
	private long index;
	private int length;
	private PacketType type;
	private long stream_id;
	
	public PacketHeader(long stream_id, long index, int length, PacketType type)
	{
		this.index = index;
		this.length = length;
		this.type = type;
		this.stream_id = stream_id;
	}
	
	public byte[] writeExternal()
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try 
		{
			ObjectOutputStream out =  new ObjectOutputStream(bos); 
			
			
			out.writeLong(stream_id);
			out.writeLong(index);
	    	out.writeInt(length);
	    	out.writeInt(type.getId());
	    	
	    	
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

	public static PacketHeader readExternal(byte[] data)
	{
		return readExternal(data, 0, data.length);
	}
	
	public static PacketHeader readExternal(byte[] data, int offset, int len)
    {	
		ByteArrayInputStream bis = new ByteArrayInputStream(data, offset, len);
		ObjectInput in = null;
		try 
		{
		  in = new ObjectInputStream(bis);
		  
		  long stream_id = in.readLong();
		  long index = in.readLong();
		  int length = in.readInt();
		  PacketType type = PacketType.getType(in.readInt());
		  
		  return new PacketHeader(stream_id, index, length, type); 
		  
		}catch(IOException ex)
		{
			return null;
		}
		finally
	    {
	    	try
	    	{
	    		if(in!=null)
	    			in.close();
	    		
	    	} catch (IOException ex) {}
	    }
		
    }
	
	public boolean set(byte[] data, int offset, int len)
    {	
		ByteArrayInputStream bis = new ByteArrayInputStream(data, offset, len);
		ObjectInput in = null;
		try 
		{
		  in = new ObjectInputStream(bis);
		  
		  long stream_id = in.readLong();
		  long index = in.readLong();
		  int length = in.readInt();
		  PacketType type = PacketType.getType(in.readInt());
		  
		  this.stream_id = stream_id;
		  this.index = index;
		  this.length = length;
		  this.type = type;
		  
		  return true;
		  
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
		
		return false;
    }
	
	public long getIndex()
	{
		return index;
	}
	
	public PacketType getType()
	{
		return type;
	}
	
	public boolean isDataFull()
	{
		return (length==-1);
	}

	
	public int getLength()
	{
		return length;
	}
	
	public long getStreamId()
	{
		return stream_id;
	}
	
	/*private  void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException 
	{
		//read in same order as write
		this.index = ois.readInt();
		
		int size = ois.readInt();
		this.checksum = new byte[size];
		ois.read(this.checksum);
		
		size = ois.readInt();
		this.data = new byte[size];
		ois.read(this.data);
		
		this.length = ois.readInt();
	}

    private  void writeObject(ObjectOutputStream oos)
    throws IOException {

       // écriture de toute ou partie des champs d'un objet
      oos.writeInt(index);
      oos.writeInt(checksum.length);
      oos.write(checksum);
      oos.writeInt(data.length);
      oos.write(data);
      oos.writeInt(length);
   }*/
	
	
}
