package PacketManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import EnvVariables.Environment;
import Exceptions.IncompleteContentException;
import PacketConstructor.Manifest;
import PacketConstructor.PacketBufferInfo;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketType;
import PacketTools.Range;

public class InMemoryPacketManager extends PacketManager
{
	private final byte[][] buffer_list;
	private final boolean[][] item_list;
	private final int[] count_list;
	private final long total_length;
	
	public InMemoryPacketManager(Manifest manifest, PacketType[] allowed_type)
	{
		super(manifest, allowed_type);
		
		this.buffer_list = new byte[mapTypeIndex.size()][];
		this.item_list = new boolean[mapTypeIndex.size()][];
		this.count_list = new int[mapTypeIndex.size()];//set to 0 by default
		
		long length;
		int nb_packets;
		long t_length = 0;
		for (Map.Entry<Integer, Integer>entry : mapTypeIndex.entrySet()) 
		{
		    Integer type_id = entry.getKey();
		    Integer index = entry.getValue();
		    
		    length = Environment.getLength(manifest, type_id);
		    if(length!=0)
		    {
		    	if(length>Integer.MAX_VALUE)
		    		throw new IllegalArgumentException();
		    	
		    	buffer_list[index] = new byte[(int)length];
		    	
		    	nb_packets = (int)Environment.getNumberPackets(manifest, type_id);
		    	item_list[index] = new boolean[nb_packets];//set to false by default
		    	t_length+=item_list[index].length;
		    }
		    
		}
		this.total_length = t_length;
	}
	
	public InMemoryPacketManager(Manifest manifest)
	{
		this(manifest, DEFAULT_ALLOWED_TYPE);
	}
	
	private byte[] getBuffer(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		return buffer_list[index];
	}
	
	private byte[] getBuffer(PacketType type)
	{
		return getBuffer(type.getId());
	}
	
	private boolean[] getItem(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		return item_list[index];
	}
	
	private boolean[] getItem(PacketType type)
	{
		return getItem(type.getId());
	}
	
	private int getCount(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		return count_list[index];
	}
	
	private int getCount(PacketType type)
	{
		return getCount(type.getId());
	}
	
	private void upCount(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		count_list[index]++;
	}
	
	private void upCount(PacketType type)
	{
		upCount(type.getId());
	}
	
	private void lowCount(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		count_list[index]--;
	}
	
	private void lowCount(PacketType type)
	{
		lowCount(type.getId());
	}
	
	private void copy(byte[] from_buffer, int offset, int len, byte[] to_buffer, int pos)
	{
		for(int i=offset; i<offset+len; i++, pos++)
		{
			to_buffer[pos] = from_buffer[i];
		}
	}
	
	public boolean add(PacketHeader header, byte[] buffer, int offset_data)
	{
		if(header==null || buffer==null)
			throw new IllegalArgumentException();
		
		if(!isWellFormedPacket(header, buffer, offset_data))
			throw new IllegalArgumentException();
		
		byte[] merge_buffer = getBuffer(header.getType());
		
		if(merge_buffer==null)
			return false;
		
		int index = (int)header.getIndex();
		int pos = index*manifest.blockSize;
		if(pos>=merge_buffer.length)
			throw new IllegalArgumentException();
		
		copy(buffer, offset_data, header.getLength(), merge_buffer, pos);
		
		boolean[] items = getItem(header.getType());
		if(items[index]==false)
		{
			items[index] = true;
			upCount(header.getType());
		}
		
		return true;
	}
	
	public boolean isReassemblyFinished()
	{
		for (Map.Entry<Integer, Integer>entry : mapTypeIndex.entrySet()) 
		{
		    Integer type_id = entry.getKey();
		    //Integer index = entry.getValue();
		    
		    if(getNumberMissingPacket(type_id)!=0)
		    	return false;
		}
		
		return true;
	}
	
	public java.util.List<PacketType> update() throws IOException 
	{
		return null;
	}
	
	public void close(boolean save_objects){}
	
	public void flush() {}
	
	public void clear() throws IOException {}
	
	public Iterator<Range> getMissingPacket(int type_id)
	{
		return (new MissingPacketIterator(type_id).iterator());
	}
	
	
	public byte[] getArray(int type_id) throws IncompleteContentException
	{
		if(!isComplete(type_id))
			throw new IncompleteContentException();
		
		return getBuffer(type_id);
	}
	
	public byte[] getArray(PacketType type)
	{
		return getBuffer(type.getId());
	}
	
	
	public long getNumberMissingPacket(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		if(item_list[index]==null)
			return 0;
		return item_list[index].length - count_list[index];
	}
	
	public String getWorkDirectory()
	{
		return null;
	}
	
	public long totalLength()
	{
		return total_length;
	}
	
	public long length()
	{
		long count = 0;
		
		for(int i=0; i<count_list.length; i++)
		{
			if(item_list[i]==null)
				continue;
			count+=count_list[i];
		}
		
		return count;
	}
	
	public PacketContent getContent(int type_id) throws IncompleteContentException
	{
		byte[] buffer = getArray(type_id);
		
		return new PacketContent(buffer);
	}
	
	
	private class MissingPacketIterator implements Iterable<Range>
	{
		
		private boolean has_next;
		private Range previous;
		private int index;
		private boolean[] list;
		private PacketType type;
		
		public MissingPacketIterator(int type_id)
		{
			this.type = PacketType.getType(type_id);
			this.list = getItem(type_id);
			int count = getCount(type_id);
			if(list==null || count==getItem(type_id).length)
			{
				has_next = false;
			}else
			{
				this.index = 0;
				this.previous = getNext();
				if(previous==null)
					has_next = false;
				else
					has_next = true;
			}
		}
		
		public MissingPacketIterator(PacketType type)
		{
			this(type.getId());
		}
		
		public Range getNext()
		{//System.out.println("-------------");
			//System.out.println("start with="+index);
			while(index<list.length && list[index]!=false)
			{
				index++;
			}
			
			if(index==list.length)
				return null;
			
			int start = index;
			while(index<list.length && list[index]==false)
			{
				index++;
			}
			int end = index-1;
			
			return new Range(start, end , type);
		}
		
		@Override
	    public Iterator<Range> iterator() {
	        Iterator<Range> it = new Iterator<Range>() {
	            @Override
	            public boolean hasNext() 
	            {
	                return has_next;
	            }

	            @Override
	            public Range next()
	            {
	            	Range tmp = previous;
	            	previous = getNext();
	            	if(previous==null)
	            		has_next = false;
	            	
	            	return tmp;
	            }

	            @Override
	            public void remove() 
	            {
	            	throw new UnsupportedOperationException();
	            }
	        };
	        return it;
	    }
	}
}
