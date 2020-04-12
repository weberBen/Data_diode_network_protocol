package PacketManager;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import PacketConstructor.Manifest;

import PacketConstructor.PacketBufferInfo;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketReader;
import PacketConstructor.PacketType;
import PacketTools.Range;

public abstract class PacketManager 
{
	//protected final PacketBufferInfo info;
	protected final Manifest manifest;
	protected final Map<Integer, Integer> mapTypeIndex;
	protected static final PacketType[] DEFAULT_ALLOWED_TYPE = new PacketType[] {PacketType.Data(), PacketType.Metadata()};
	protected boolean is_closed;
	
	public PacketManager(Manifest manifest, PacketType[] allowed_type)
	{
		if(manifest==null || allowed_type==null)
			throw new IllegalArgumentException();
		
		this.manifest = manifest;
		this.is_closed = false;
		
		HashMap<Integer, Integer> tmp = new HashMap<Integer, Integer>();
		for(int i=0; i<allowed_type.length; i++)
		{
			tmp.put(allowed_type[i].getId(), i);
		}
		mapTypeIndex = Collections.unmodifiableMap(tmp);
	}
	
	public PacketManager(Manifest manifest)
	{
		this(manifest, DEFAULT_ALLOWED_TYPE);
	}
	
	public boolean isAllowedType(int type_id)
	{
		return (mapTypeIndex.get(type_id)!=null);
	}
	
	public boolean isAllowedType(PacketType type)
	{
		return isAllowedType(type.getId());
	}
	
	public boolean isWellFormedPacket(PacketHeader header, byte[] buffer, int offset_data)
	{
		return isAllowedType(header.getType()) && header.getLength()<=manifest.blockSize && offset_data+header.getLength()<=buffer.length && header.getStreamId()==manifest.id;
	}
	
	public abstract boolean add(PacketHeader header, byte[] buffer, int offset_data) throws IOException;
	
	public abstract Iterator<Range> getMissingPacket(int type_id);
	public Iterator<Range> getMissingPacket(PacketType type)
	{
		return getMissingPacket(type.getId());
	}
	
	public abstract void flush() throws IOException;
	
	public abstract void close(boolean save_objects) throws IOException;
	public void close() throws IOException
	{
		close(true);
	}
	
	public boolean isClosed()
	{
		return is_closed;
	}
	
	public abstract boolean isReassemblyFinished();
	public abstract long getNumberMissingPacket(int type_id);
	public long getNumberMissingPacket(PacketType type)
	{
		return getNumberMissingPacket(type.getId());
	}
	
	public long getNumberMissingPacket()
	{
		long count = 0;
		for (Map.Entry<Integer, Integer>entry : mapTypeIndex.entrySet()) 
		{
		    Integer type_id = entry.getKey();
		    //Integer index = entry.getValue();
		    
		    count+=getNumberMissingPacket(type_id);
		}
		
		return count;
	}
	
	public boolean isComplete(int type_id)
	{
		return getNumberMissingPacket(type_id)==0;
	}
	
	public boolean isComplete(PacketType type)
	{
		return isComplete(type.getId());
	}
	
	public boolean isComplete()
	{
		for (Map.Entry<Integer, Integer>entry : mapTypeIndex.entrySet()) 
		{
		    Integer type_id = entry.getKey();
		    //Integer index = entry.getValue();
		    
		    if(!isComplete(type_id))
		    	return false;
		}
		
		return true;
	}
	
	public abstract PacketContent getContent(int type_id);
	public PacketContent getContent(PacketType type)
	{
		return getContent(type.getId());
	}
	
	public abstract long totalLength();
	public abstract long length();
	public abstract String getWorkDirectory();
	public abstract void update() throws IOException;
	
	public Manifest getManifest()
	{
		return manifest;
	}
}
