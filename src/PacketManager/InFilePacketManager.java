package PacketManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import EnvVariables.Environment;
import EnvVariables.Parms;
import Exceptions.IncompleteContentException;
import PacketConstructor.Manifest;
import PacketConstructor.PacketBufferInfo;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketReader;
import PacketConstructor.PacketType;
import PacketTools.Range;
import generalTools.Tools;

public class InFilePacketManager extends PacketManager
{
	private final String work_dir;
	private final BufferedOutputStream[] merge_files;
	private final RandomAccessFile[] merge_random_files;
	private final List[] packet_lists;
	private final BufferedPacketList buffered_packets;
	private boolean packet_buffering;
	private final PacketPoolFile pool_file;
	private final long total_length;
	
	public static final long DEFAULT_THRESHOLD_MISSING = 10;
	public static final int DEFAULT_NUMBER_PACKET_BLOCK = Byte.MAX_VALUE - Byte.MIN_VALUE;
	public static final int DEFAULT_NUMBER_PACKET_TO_HOLD = DEFAULT_NUMBER_PACKET_BLOCK;
	
	public InFilePacketManager(String work_dir, Manifest manifest, int number_packet_hold, int nb_packet_block, int buffer_size_file) throws IOException
	{
		super(manifest);
		
		if(work_dir==null || number_packet_hold<=0 || nb_packet_block<=0 || buffer_size_file<=0)
			throw new IllegalArgumentException();
		
		//create work directory
		File f = new File(work_dir);
		if((!f.exists()) || (f.exists() && !f.isDirectory()))
			throw new IllegalArgumentException();
		
		work_dir = getWorkDirname(work_dir, manifest.id);
		File file = new File(work_dir);
		file.mkdir();
		
		//manifest to file
		File file_manifest = new File(getManifestFilename(work_dir));
		if(!file_manifest.exists() || (file_manifest.exists() && !file_manifest.isFile()))
		{
			FileOutputStream stream = new FileOutputStream(file_manifest);
			stream.write(manifest.writeExternal());
			stream.close();
		}
		
		final int size = mapTypeIndex.size();
		this.packet_buffering = true;
		this.packet_lists = new List[size];
		this.merge_files = new BufferedOutputStream[size];
		this.merge_random_files = new RandomAccessFile[size];
		this.work_dir = work_dir;
		
		Flag[] list_flag = new Flag[] {Flag.WRITTEN};
		String list_block_count_parms = "m"+";"+Flag.WRITTEN.id;
		String list_complete_parms = ""+Flag.WRITTEN.id;
		
		long count;
		boolean all_end = true;
		long length = 0;
		for (Map.Entry<Integer, Integer>entry : mapTypeIndex.entrySet()) 
		{
		    Integer type_id = entry.getKey();
		    Integer index = entry.getValue();
			
			count = Environment.getNumberPackets(manifest, type_id);
			if(count>Integer.MAX_VALUE)
				throw new IndexOutOfBoundsException("cast type <<long>> to <<int>> impossible");
			
			
			File tmp = new File(getMergeFilename(work_dir, type_id));
			FileOutputStream sream = new FileOutputStream(tmp, true);
			merge_files[index] = new BufferedOutputStream(sream, Parms.instance().getBufferFileSize());
			
			merge_random_files[index] = new RandomAccessFile(tmp, "rw");
			
			packet_lists[index] = List.getInstance(work_dir, manifest, type_id, nb_packet_block, list_flag, list_block_count_parms, list_complete_parms);
			if(packet_lists[index]!=null)
			{
				length+=packet_lists[index].length();
				all_end = false;
			}
		}
		
		total_length = length;
		if(!all_end)
			this.buffered_packets = new BufferedPacketList(number_packet_hold);
		else
			this.buffered_packets = null;
		
		this.pool_file = new PacketPoolFile(work_dir, manifest, buffer_size_file);
	}
	
	public InFilePacketManager(String work_dir, int number_packet_hold, int nb_packet_block, int buffer_size_file) throws IOException
	{
		this(work_dir, PacketReader.getManifest(Tools.getBytesFromFile(new File(getManifestFilename(work_dir)))), number_packet_hold, 
				nb_packet_block, buffer_size_file);
	}
	
	
	public InFilePacketManager(String work_dir, Manifest manifest, int number_packet_hold, int nb_packet_block) throws IOException
	{
		this(work_dir, manifest, number_packet_hold, nb_packet_block, Parms.instance().getBufferFileSize());
	}
	
	public InFilePacketManager(String work_dir, Manifest manifest, int number_packet_hold) throws IOException
	{
		this(work_dir, manifest, number_packet_hold, DEFAULT_NUMBER_PACKET_BLOCK, Parms.instance().getBufferFileSize());
	}
	
	public InFilePacketManager(String work_dir, Manifest manifest) throws IOException
	{
		this(work_dir, manifest, DEFAULT_NUMBER_PACKET_TO_HOLD, DEFAULT_NUMBER_PACKET_BLOCK, Parms.instance().getBufferFileSize());
	}
	
	private String getMergeFilename(String work_dir, int type_id)
	{
		return work_dir+File.separator+"merge_"+type_id;
	}
	
	private String getMergeFilename(String work_dir, PacketType type)
	{
		return getMergeFilename(work_dir, type.getId());
	}
	
	private static String getWorkDirname(String work_dir, long manifest_id)
	{
		return work_dir + File.separator + manifest_id;
	}
	
	private static String getManifestFilename(String work_dir)
	{
		return work_dir + File.separator + Environment.MANIFEST_FILENAME;
	}
	
	private List getList(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		return packet_lists[index];
	}
	
	private List getList(PacketType type)
	{
		return getList(type.getId());
	}
	
	private BufferedOutputStream getStream(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		return merge_files[index];
	}
	
	private BufferedOutputStream getStream(PacketType type)
	{
		return getStream(type.getId());
	}
	
	private RandomAccessFile getRandomAccessFile(int type_id)
	{
		int index = mapTypeIndex.get(type_id);
		return merge_random_files[index];
	}
	
	private RandomAccessFile getRandomAccessFile(PacketType type)
	{
		return getRandomAccessFile(type.getId());
	}
	
	private void flushBufferedPacket() throws IOException
	{
		if(buffered_packets==null)
			return ;
		
		Iterator<Integer> it = buffered_packets.itemPosIterator();
		int pos;
		long new_pos;
		long index;
		int length;
		PacketType type;
		List list;
		byte[] buffer;
		
		while(it.hasNext())
		{
			pos = it.next();
			
			index = buffered_packets.getPacketIndex(pos);
			type = buffered_packets.getPacketType(pos);
			length = buffered_packets.getPacketLength(pos);
			buffer = buffered_packets.getPacketData(pos);
			
			
			list = getList(type);
			new_pos = pool_file.write(type, index, buffer);
			list.setFile(index, new_pos);
			
			//System.out.println("**flush packet type="+type+", index="+index+", new_pos="+new_pos);
			
			it.remove();
		}
	}
	
	
	
	private boolean gather(List list, long index, byte[] buffer, int offset_data, int length, boolean complete) throws IOException
	{
		BufferedOutputStream stream_out = getStream(list.getType());
		boolean added = false;
		//System.out.println("#gather (complete="+complete+")");
		Pos item_pos;
		//System.out.println("index="+list.getIndex()+"  | complete="+completeBlock(list, list.getIndex()));
		while(list.getIndex()<list.length() && completeBlock(list, list.getIndex()))
		{
			if(list.getIndex()==index)
			{//System.out.println("gather : write index = "+index);
				list.setMemory(index, 0);//random pos
				added = true;
				stream_out.write(buffer, offset_data, length);
				list.setFlag(list.getIndex(), Flag.WRITTEN);
				//System.out.println("#gather : is index   | index="+list.getIndex());
				list.nextIndex();
				continue;
			}
			
			item_pos = list.getIndexItem();
			if(item_pos==null)
			{//System.out.println("#gather : write random   | index = "+list.getIndex());
				if(!complete || list.getIndex()==list.length()-1)
					break;
				
				for(int i=0; i<manifest.blockSize; i++)
				{
					stream_out.write(i);
				}
				
			}else if(item_pos.isFileValue())
			{//System.out.println("#gather : write from file   | index = "+list.getIndex());
				if(!complete)
					break;
				
				//System.out.println("value="+item_pos+"  index="+list.getIndex());
				byte[] data = pool_file.read(item_pos.value);
				stream_out.write(data);
				list.setFlag(list.getIndex(), Flag.WRITTEN);
				
			}else if(item_pos.isMemoryValue())
			{
				//System.out.println("#gather in memory : type="+buffered_packets.getPacketType((int)item_pos.value)+"  | index="+buffered_packets.getPacketIndex((int)item_pos.value));
				byte[] data = buffered_packets.getPacketData((int)item_pos.value);
				stream_out.write(data);
				
				list.setFlag(list.getIndex(), Flag.WRITTEN);
				buffered_packets.remove((int)item_pos.value);
			}
			
			list.nextIndex();
		}
		
		if(!added)
		{
			if(list.getIndex()==index)
			{//System.out.println("gather : write index = "+index);
				//System.out.println("index="+index+"   added !");
				added = true;
				stream_out.write(buffer, offset_data, length);
				
				list.setFlag(list.getIndex(), Flag.WRITTEN);
				list.nextIndex();
			}
		}
		
		return added;
	}
	
	private void gather(List list, boolean complete) throws IOException
	{
		gather(list, -1, null, 0, 0, complete);
	}
	
	private boolean completeBlock(List list, long index)
	{//System.out.println("complete : type="+list.getType()+" | index="+index+"  | count="+list.getBlockCount(index));
		return ( (list.getBlockCount(index)!=0) && (list.getBlockLength() - list.getBlockCount(index) < DEFAULT_THRESHOLD_MISSING));//;
	}
	
	public boolean add(PacketHeader header, byte[] buffer, int offset_data) throws IOException
	{
		if(super.is_closed)
			return false;
		
		//System.out.println("add packet : type="+header.getType()+", index="+header.getIndex());
		if(header==null || buffer==null)
			throw new IllegalArgumentException();
		
		if(!isWellFormedPacket(header, buffer, offset_data))
			throw new IllegalArgumentException();
		
		List list = getList(header.getType());
		
		if(list==null)
		{//System.out.println("\tcome back end stream");
			RandomAccessFile file = getRandomAccessFile(header.getType());
			file.seek(header.getIndex()*manifest.blockSize);
			file.write(buffer, offset_data, header.getLength());
			
			return true;
		}
		
		//System.out.println("list index="+list.getIndex()+"  - list length="+list.length());
		if(header.getIndex()<list.getIndex())
		{//System.out.println("\tcome back at position="+(header.getIndex()*manifest.blockSize));
			//System.out.println("write index="+header.getIndex());
			RandomAccessFile file = getRandomAccessFile(list.getType());
			file.seek(header.getIndex()*manifest.blockSize);
			file.write(buffer, offset_data, header.getLength());
			
			list.setFlag(header.getIndex(), Flag.WRITTEN);
			
			return true;
		}
		
		if(!packet_buffering)
		{
			//System.out.println("\twrite directrly");
			long pos = pool_file.write(header, buffer, offset_data);
			list.setFile(header.getIndex(), pos);
			//System.out.println("write directly index="+header.getIndex());
			return true;
		}
		
		if(list.isInBlockIndex(header.getIndex()))
		{//System.out.println("\tis in block  | index="+header.getIndex()+"   - block_count="+list.getBlockCount(header.getIndex()));
		
			if(buffered_packets.isFull())
			{//System.out.println("\t\t buffer full");
				boolean added = gather(list, header.getIndex(), buffer, offset_data, header.getLength(), true);
				
				if(buffered_packets.isFull())
				{//System.out.println("\t\t\t flush data");
					flushBufferedPacket();
					packet_buffering = false;
				}
				
				if(!added)
				{//System.out.println("\t\t\t add to memory");
					Long pos = buffered_packets.add(header, buffer, offset_data);
					list.setMemory(header.getIndex(), pos);
				}
				
			}else
			{//System.out.println("\t\t buffer not full");
				if(!gather(list, header.getIndex(), buffer, offset_data, header.getLength(), false))
				{//System.out.println("\t\t\t add to memory");
					Long pos = buffered_packets.add(header, buffer, offset_data);
					list.setMemory(header.getIndex(), pos);
				}else
				{
					//System.out.println("\t\t\t gather ok");
				}
			}
			
		}else
		{//System.out.println("\t not in block  |   index="+header.getIndex());
			if(buffered_packets.isFull())
			{//System.out.println("\t\t buffere full");
				gather(list, true);
				
				if(buffered_packets.isFull())
				{//System.out.println("\t\t\t buffer full");
					flushBufferedPacket();
					packet_buffering = false;
				}
			}
			//System.out.println("\t\t add to memory");
			Long pos = buffered_packets.add(header, buffer, offset_data);
			list.setMemory(header.getIndex(), pos);
		}
		
		//System.out.println("->buffer size="+buffered_packets.getFullSpaceLength());
		return true;
	}
	
	private boolean update(List list) throws IOException
	{
		if(list==null)
			return true;
		
		BufferedOutputStream stream_out = getStream(list.getType());
		//System.out.println("MANEGR number witten="+list.getCountFlag(Flag.WRITTEN));
		//System.out.println("------------------------list type="+list.getType()+"   | inedex="+list.getIndex());
		Pos pos;
		long index;
		while(list.getIndex()<list.length())
		{
			index = list.getIndex();
			pos = list.get(index);
			
			if(pos==null)
			{//System.out.println("pos null at index="+index);
				return false;
			}
			else if(pos.type==Pos.MEMORY_TYPE)
			{
				byte[] data = buffered_packets.getPacketData((int)pos.value);
				stream_out.write(data);
				//System.out.println("pos memory at index="+index+"  - length="+data.length+"  | pos="+pos);
				list.setFlag(index, Flag.WRITTEN);
				buffered_packets.remove((int)pos.value);
			}else if(pos.type==Pos.FILE_TYPE)
			{
				byte[] data = pool_file.read(pos.value);
				//System.out.println("pos file at index="+index+"  - length="+data.length+"  | pos="+pos);
				stream_out.write(data);
				list.setFlag(index, Flag.WRITTEN);
			}else
			{
				//System.out.println("unkwno t index="+index+"  pos="+pos);
				return false;
			}
			
			list.nextIndex();
		}
		
		return true;
			
	}
	
	public java.util.List<PacketType> update() throws IOException
	{
		ArrayList<PacketType> output = new ArrayList<PacketType>();
		for(List list : packet_lists)
		{
			if(list==null)
				continue;
			
			if(!update(list))
				output.add(list.getType());
		}
		
		if(output.size()==0)
			return null;
		
		return Collections.unmodifiableList(output);
	}
	

	public long getNumberMissingPacket(int type_id)
	{
		List list = getList(type_id);
		if(list==null)
			return 0;
		
		return list.length() - list.getCountNotNull();
	}
	
	public void flush() throws IOException
	{
		if(super.is_closed)
			return;
		
		//System.out.println("----------------------- FLUSH\n");
		pool_file.flush();
		
		for(List list : packet_lists)
		{
			if(list==null)
				continue;
			gather(list, true);
		}
		
		for(BufferedOutputStream stream_out : merge_files)
		{
			stream_out.flush();
		}
	}
	
	public void close(boolean save_objects) throws IOException
	{
		if(super.is_closed)
			return;
		
		if(save_objects)
		{
			flush();
			flushBufferedPacket();
		}
		
		for(List list : packet_lists)
		{
			if(list==null)
				continue;
			list.close(save_objects);
		}
		
		pool_file.close(save_objects);
		
		for(BufferedOutputStream stream_out : merge_files)
		{
			stream_out.close();
		}
		
		super.is_closed = true;
	}
	
	public void deleteAll() throws IOException
	{
		if(super.is_closed)
			return;
		
		pool_file.close();
		
		for(BufferedOutputStream stream_out : merge_files)
		{
			stream_out.close();
		}
		
		super.is_closed = true;
		
	}
	
	
	public boolean isReassemblyFinished()
	{
		for(List list : packet_lists)
		{
			if(list==null)
				continue;
			
			if(list.getCountFlag(Flag.WRITTEN)!=list.length())
				return false;
		}
		
		return true;
	}
	
	public String getWorkDirectory()
	{
		return work_dir;
	}
	
	public long totalLength()
	{
		return total_length;
	}
	
	public long length()
	{
		long count = 0;
		for(List list : packet_lists)
		{
			if(list==null)
				continue;
			
			count+=list.getCountNotNull();
		}
		
		return count;
	}
	
	public String getFilename(int type_id) throws IncompleteContentException
	{
		List list = getList(type_id);
		System.out.println("FILENAME LIST : list==null?"+(list==null)+"  | number written="+list.getCountFlag(Flag.WRITTEN)+"  | list length="+list.length());
		if(list!=null  && (list.getCountFlag(Flag.WRITTEN)!=list.length()))
			throw new IncompleteContentException();
		
		return getMergeFilename(work_dir, type_id);
	}
	
	
	public String getFilename(PacketType type) throws IncompleteContentException
	{
		return getFilename(type.getId());
	}
	
	public PacketContent getContent(int type_id) throws IncompleteContentException
	{
		String filename = getFilename(type_id);
		
		return new PacketContent(filename);
	}
	
	
	
	public Iterator<Range> getMissingPacket(int type_id)
	{
		return (new MissingPacketIterator(type_id).iterator());
	}
	
	
	
	private class MissingPacketIterator implements Iterable<Range>
	{
		
		private boolean has_next;
		private Range previous;
		private int index;
		private List list;
		
		public MissingPacketIterator(int type_id)
		{
			this.list = getList(type_id);
			if(list==null)
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
			while(index<list.length() && list.get(index)!=null)
			{
				index++;
			}
			
			if(index==list.length())
				return null;
			
			int start = index;
			while(index<list.length() && list.get(index)==null)
			{
				index++;
			}
			int end = index-1;
			
			return new Range(start, end , list.getType());
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



/*
 * 
 * 
 * 
 * 
 * 
 * 
 *
*/

class BufferedPacketList
{
	private static class Container
	{
		public final long index;
		public final PacketType type;
		public final byte[] buffer;
		
		public Container(long index, PacketType type, byte[] buffer)
		{
			this.index = index;
			this.type = type;
			this.buffer = buffer;
		}
	}
	
	private Container[] list;
	private LinkedList<Integer> free_pos;
	
	public BufferedPacketList(int number_packet)
	{
		this.list = new Container[number_packet];
		
		this.free_pos = new LinkedList<Integer>();
		
		for(int i=0; i<number_packet; i++)
		{
			free_pos.add(i);
		}
	}
	
	public Long add(PacketHeader header, byte[] buffer, int offset_data)
	{
		if(free_pos.isEmpty())
			return null;
		
		
		int pos = free_pos.pollFirst();
		
		byte[] data = Arrays.copyOfRange(buffer, offset_data, offset_data+header.getLength());
		Container container = new Container(header.getIndex(), header.getType(), data);
		
		list[pos] = container;
		
		return new Long(pos);
	}
	
	private void setItemRemoved(int pos)
	{
		free_pos.add(pos);
		list[pos] = null;
	}
	
	public long getPacketIndex(int pos)
	{
		if(pos>=list.length)
			throw new IllegalArgumentException();
		
		return list[pos].index;
	}
	
	public PacketType getPacketType(int pos)
	{
		if(pos>=list.length)
			throw new IllegalArgumentException();
		
		return list[pos].type;
	}
	
	public int getPacketLength(int pos)
	{
		if(pos>=list.length)
			throw new IllegalArgumentException();
		
		return list[pos].buffer.length;
	}
	
	public byte[] getPacketData(int pos)
	{
		if(pos>=list.length)
			throw new IllegalArgumentException();
		
		return list[pos].buffer;
	}
	
	public void remove(int pos)
	{
		if(pos>=list.length)
			throw new IllegalArgumentException();
		
		setItemRemoved(pos);
	}
	
	public int getFullSpaceLength()
	{
		return list.length - free_pos.size();
	}
	
	public boolean isEmpty()
	{
		return getFullSpaceLength()==0;
	}
	
	public boolean isFull()
	{
		return free_pos.size()==0;
	}
	
	
	public Iterator<Integer> itemPosIterator()
	{
		return (new ItemPosIterator(this)).iterator();
	}
	/* -------------------------------------------
	 * 
	 * 
	 * -------------------------------------------
	 */
	
	private class ItemPosIterator implements Iterable<Integer>
	{
		
		private boolean has_next;
		private Integer previous;
		private int index;
		private Integer prev_remove;
		private BufferedPacketList list_pointer;
		
		public ItemPosIterator(BufferedPacketList list_pointer)
		{
			this.list_pointer = list_pointer;
			index = 0;
			previous = getNext();
			if(previous==null)
				has_next = false;
			else
				has_next = true;
		}
		
		public Integer getNext()
		{
			while(index<list.length && list[index]==null)
			{
				index++;
			}
			
			if(index==list.length)
				return null;
			
			Integer tmp = new Integer(index);
			index++;
			
			return tmp;
		}
		
		@Override
	    public Iterator<Integer> iterator() {
	        Iterator<Integer> it = new Iterator<Integer>() {
	            @Override
	            public boolean hasNext() 
	            {
	                return has_next;
	            }

	            @Override
	            public Integer next()
	            {
	            	prev_remove = previous;
	            	Integer tmp = previous;
	            	previous = getNext();
	            	if(previous==null)
	            		has_next = false;
	            	
	            	return tmp;
	            }

	            @Override
	            public void remove() 
	            {
	            	if(prev_remove==null)
	            		return;
	            	
	            	list_pointer.remove(prev_remove);
	            }
	        };
	        return it;
	    }
	}
	
}




abstract class List
{
	public abstract Pos get(long index);
	public abstract void setFile(long index, long value);
	public abstract void setMemory(long index, long value);
	public abstract void setDefault(long index);
	public abstract long getCountNotNull();
	public abstract long length();
	public abstract void close(boolean save_objects) throws IOException;
	public void close() throws IOException
	{
		close(true);
	}
	public abstract PacketType getType();
	
	public static List getInstance(String work_dir, Manifest manifest, PacketType type, long block_length, Flag[] flags, String block_count_parms, String complete_parms) throws IOException
	{
		return ListFile.fromFile(work_dir, manifest, type, block_length, flags, block_count_parms, complete_parms);
	}
	
	public static List getInstance(String work_dir, Manifest manifest, int type_id, long block_length, Flag[] flags, String block_count_parms, String complete_parms) throws IOException
	{
		return getInstance(work_dir, manifest, PacketType.getType(type_id), block_length, flags, block_count_parms, complete_parms);
	}
	
	public abstract long getIndex();
	public abstract long nextIndex();
	public abstract long getBlockCount(long index);
	public abstract long getBlockLength();
	public abstract boolean isInBlockIndex(long index);
	public abstract boolean isIndex(long index);
	public abstract Pos getIndexItem();
	
	public abstract void setFlag(long index, Flag flag);
	public abstract long getCountNull();
	public abstract long getCountMemory();
	public abstract long getCountFile();
	public abstract long getCountFlag(int flag_id);
	public long getCountFlag(Flag flag)
	{
		if(flag==null)
			return 0;
		return getCountFlag(flag.id);
	}
	
}



class ListFile
{
	private static final String FILENAME_PREFIXE = "list_";
	
	public static final double CEIL_NOT_LINKED = 0.6;
	
	
	public static String getFilename(String work_dir, PacketType type)
	{
		return work_dir+File.separator+FILENAME_PREFIXE+type.getId();
	}
	
	public static File getFile(String work_dir, PacketType type)
	{
		return new File(getFilename(work_dir, type));
	}
	 
	public static void toFile(String work_dir, ListArray list) throws IOException
	{
		File tmp = getFile(work_dir, list.getType());
		if(tmp.exists())
			tmp.delete();
		
		RandomAccessFile file = new RandomAccessFile(tmp, "rw");
		
		FileOutputStream fis_out = new FileOutputStream(file.getFD());
		DataOutputStream buffered_file_out = new DataOutputStream(new BufferedOutputStream(fis_out, Parms.instance().getBufferFileSize()));
		
		boolean linked;
		if(list.getCountNotNull()>=CEIL_NOT_LINKED*list.length())
			linked = false;
		else
			linked = true;
		
		ListFileInfo info = new ListFileInfo(file, list.getType(), list.length(), linked, 
				list.getIndex(), list.getFlags(), list.getBlockCountParms(), list.getBlockLength(), list.getCompleteParms());
		info.write();
		
		if(!ListArray.isCompleted(list))
		{
		
			Pos pos;
			int type;
			long value;
			for(long i=0; i<list.length(); i++)
			{
				pos = list.get(i);
				
				if(linked)
				{
					if(pos==null)
						continue;
					
					buffered_file_out.writeLong(i);
					buffered_file_out.writeInt(pos.type);
					buffered_file_out.writeLong(pos.value);
				}else
				{
					if(pos==null)
					{
						type = Pos.NULL_TYPE;
						value = 0;
						
					}else
					{
						type = pos.type;
						value = pos.value;
					}
					
					buffered_file_out.writeInt(type);
					buffered_file_out.writeLong(value);
				}
			}
		}
		
		buffered_file_out.flush();
		buffered_file_out.close();
		file.close();
	}
	
	public static ListArray fromFile(String work_dir, Manifest manifest, PacketType type, long block_length, Flag[] flags, String block_count_parms, String complete_parms) throws IOException
	{
		File tmp = getFile(work_dir, type);
		if(!tmp.exists())
		{
			return new ListArray(work_dir, manifest, type, block_length, flags, block_count_parms, complete_parms);
		}
		
		RandomAccessFile file = new RandomAccessFile(tmp, "r");
		
		FileInputStream fis_in = new FileInputStream(file.getFD());
		DataInputStream buffered_file_in = new DataInputStream(new BufferedInputStream(fis_in, Parms.instance().getBufferFileSize()));
		
		ListFileInfo info = new ListFileInfo(file);
		ListArray list;
		
		if(info.length>=Integer.MAX_VALUE)
		{
			try
			{
				buffered_file_in.close();
				file.close();
			}catch(IOException e){}
			throw new IndexOutOfBoundsException();
		}
		
		list = new ListArray(work_dir, manifest, info);
		System.out.println("**********count blokc from parms count="+info.blockCountParms);
		
		if(info.linked)
		{	
			long i;
			int item_type;
			long value;
			while(true)
			{
			   try
			   {
				   i = buffered_file_in.readLong();
				   item_type = buffered_file_in.readInt();
				   value = buffered_file_in.readLong();
				   
				   if(i>=Integer.MAX_VALUE)
						throw new IndexOutOfBoundsException();
				   

				   switch(item_type)
				   {
				   		case Pos.FILE_TYPE:
				   			list.setFile(i, value);
				   			break;
				   		case Pos.MEMORY_TYPE:
				   			list.setMemory(i, value);
				   			break;
				   		case Pos.FLAG_TYPE:
				   			list.setFlag(i, (int)value);
				   			break;
				   		default:
				   			throw new IllegalArgumentException();
				   }
				   
			   } catch(EOFException e)
			   {
			      break;
			   }
			}
			
		}else
		{
			long i = 0;
			int item_type;
			long value;
			while(true)
			{
			   try
			   {
				   item_type = buffered_file_in.readInt();
				   value = buffered_file_in.readLong();
				   
				   switch(item_type)
				   {
				   		case Pos.NULL_TYPE:
				   			list.setDefault(i);
				   			break;
				   		case Pos.FILE_TYPE:
				   			list.setFile(i, value);
				   			break;
				   		case Pos.MEMORY_TYPE:
				   			list.setMemory(i, value);
				   			break;
				   		case Pos.FLAG_TYPE:
				   			list.setFlag(i, (int)value);
				   			break;
				   		default:
				   			throw new IllegalArgumentException();
				   }
				   
				   i++;
				   
			   } catch(EOFException e)
			   {
			      break;
			   }
			}
		}
		
		buffered_file_in.close();
		file.close();
		
		return list;
	}
}

class ListFileInfo
{
	public final PacketType type;
	public final long length;
	public final boolean linked;
	public final long index;
	public final String blockCountParms;
	public final long nbPacketBlock;
	public final Flag[] flags;
	public final String completeParms;
	private RandomAccessFile file;
	
	private static final String STRING_FORMAT = "UTF-8";
	private static final long POS_TYPE = 0;
	private static final long POS_LENGTH = POS_TYPE + Integer.BYTES;
	private static final long POS_LINKED = POS_LENGTH + Long.BYTES;
	private static final long POS_INDEX = POS_LINKED + Integer.BYTES;
	private static final long POS_COUNT_NOT_NULL = POS_INDEX + Long.BYTES;
	private static final long SIZE = POS_COUNT_NOT_NULL + Long.BYTES;
	

	public ListFileInfo(RandomAccessFile file, PacketType type, 
			long length, boolean linked, long index, Flag[] flags, String block_count_parms, long nb_packet_block, String compete_parms) throws IOException
	{
		this.type = type;
		this.length = length;
		this.linked = linked;
		this.index = index;
		this.flags = flags;
		this.blockCountParms = block_count_parms;
		this.nbPacketBlock = nb_packet_block;
		this.completeParms = compete_parms;
		
		this.file = file;
	}
	
	public ListFileInfo(RandomAccessFile file) throws IOException
	{
		try
		{
			file.seek(0);
			this.file = file;
			
			this.type = PacketType.getType(file.readInt());
			this.length = file.readLong();
			
			this.linked = file.readBoolean();
			
			this.index = file.readLong();
			
			int size = file.readInt();
			if(size!=0)
			{
				byte[] tmp = new byte[size];
				file.read(tmp);
				this.blockCountParms = new String(tmp, STRING_FORMAT);
			}else
			{
				this.blockCountParms = null;
			}
			
			size = file.readInt();
			if(size!=0)
			{
				this.flags = new Flag[size];
				for(int i=0; i<size; i++)
				{
					this.flags[i] = Flag.getFlag(file.readInt());
				}
				
			}else
			{
				this.flags = null;
			}
			
			this.nbPacketBlock = file.readLong();
			
			
			size = file.readInt();
			if(size!=0)
			{
				byte[] tmp = new byte[size];
				file.read(tmp);
				this.completeParms = new String(tmp, STRING_FORMAT);
			}else
			{
				this.completeParms = null;
			}
			
		}catch(EOFException e)
		{
			throw new IllegalArgumentException();
		}
	}
	
	public RandomAccessFile getFile()
	{
		return file;
	}
	
	public void write() throws IOException
	{
		file.seek(0);
		
		file.writeInt(type.getId());
		
		file.writeLong(length);
		
		file.writeBoolean(linked);
		
		file.writeLong(index);
		
		if(blockCountParms!=null)
		{
			byte[] tmp = blockCountParms.getBytes(STRING_FORMAT);
			
			file.writeInt(tmp.length);
			file.write(tmp);
		}else
		{
			file.writeInt(0);
		}
		
		if(flags!=null)
		{
			file.writeInt(flags.length);
			for(Flag flag : flags)
			{
				file.writeInt(flag.id);
			}
		}else
		{
			file.writeInt(0);
		}
		
		file.writeLong(nbPacketBlock);
		
		
		
		if(completeParms!=null)
		{
			byte[] tmp = completeParms.getBytes(STRING_FORMAT);
			
			file.writeInt(tmp.length);
			file.write(tmp);
		}else
		{
			file.writeInt(0);
		}
	}
	
	public static long size()
	{
		return SIZE; 
	}
}

class Pos
{
	public final long value;
	public final int type;
	
	public static final int NULL_TYPE = 0;
	public static final int MEMORY_TYPE = 1;
	public static final int FILE_TYPE = 2;
	public static final int FLAG_TYPE = 3;
	
	public Pos(long value, int type)
	{
		this.value = value;
		this.type = type;
	}
	
	public boolean isMemoryValue()
	{
		return type==MEMORY_TYPE;
	}
	
	public boolean isFileValue()
	{
		return type==FILE_TYPE;
	}
	
	public boolean isFlagValue()
	{
		return type==FLAG_TYPE;
	}
	
	public boolean isFlagValue(int flag_id)
	{
		return isFlagValue() && (flag_id==value) ;
	}
	
	public boolean isFlagValue(Flag flag)
	{
		return (flag!=null && isFlagValue(flag.id));
	}
	
	public String toString()
	{
		String type_str;
		switch(type)
		{
			case MEMORY_TYPE:
				type_str="Memory";
				break;
			case FILE_TYPE:
				type_str="File";
				break;
			case FLAG_TYPE:
				type_str="Flag";
				break;
			default:
				type_str=""+type;
				break;
		}
		return "Pos(type="+type_str+", value="+value+")";
	}
}

class Block
{
	private int[] int_array;
	private byte[] byte_array;
	private int type;
	private long block_length;
	private long min_value;
	private long max_value;
	
	public Block(long total_length, long block_length)
	{
		if(block_length<0)
			throw new IllegalArgumentException();
		
		this.block_length = block_length;
		
		if(block_length>getRange(Byte.MIN_VALUE, Byte.MAX_VALUE))
		{
			type = 1;
			this.min_value = Integer.MIN_VALUE;
			this.max_value = min_value + block_length;
			//this.block_length = getRange(min_value, max_value);
		}else
		{
			type = 0;
			
			this.min_value = Byte.MIN_VALUE;
			this.max_value = min_value + block_length;
			//this.block_length = getRange(min_value, max_value);
		}
		
		
		long length = (long)Math.ceil((double)total_length/this.block_length);
		if(length>Integer.MAX_VALUE)
			new IllegalArgumentException();
		
		if(type==1)
		{
			int_array = new int[(int)length];
			Arrays.fill(int_array, Integer.MIN_VALUE);
			
			long tmp = length*block_length-1;
			int index = (int)getBlock(tmp);
			int_array[index]+= (tmp-total_length+1);
			
		}else
		{
			byte_array = new byte[(int)length];
			Arrays.fill(byte_array, Byte.MIN_VALUE);
			
			long tmp = length*block_length-1;
			int index = (int)getBlock(tmp);
			byte_array[index]+= (tmp-total_length+1);
		}
	}
	
	private long getRange(long min_val, long max_val)
	{
		return max_val - min_val;//(max_val-(min_val+1)+1);
	}
	
	private long getBlock(long index)
	{
		return (long)((double)(index)/block_length);
	}
	
	private long get(long index)
	{
		index = getBlock(index);
		
		if(type==1)
		{
			return int_array[(int)index];
		}else
		{
			return byte_array[(int)index];
		}
	}
	
	public void up(long index)
	{
		index = getBlock(index);
		
		if(type==1)
		{
			int_array[(int)index]++;
		}else
		{
			byte_array[(int)index]++;
		}
	}
	
	public void low(long index)
	{
		index = getBlock(index);
		
		if(type==1)
		{
			int_array[(int)index]--;
		}else
		{
			byte_array[(int)index]--;
		}
	}
	
	public long length()
	{
		if(type==1)
		{
			return int_array.length;
		}else
		{
			return byte_array.length;
		}
	}
	
	public long getBlockCount(long index)
	{
		if(type==1)
		{
			return Math.abs(int_array[(int)getBlock(index)]-min_value);
		}else
		{
			return Math.abs(byte_array[(int)getBlock(index)]-min_value);
		}
	}
	
	public long getRange()
	{
		return block_length;
	}
}

class Flag
{
	public final int id;
	
	public static final Flag WRITTEN = new Flag(1);
	
	private Flag(int id)
	{
		this.id = id;
	}
	
	public boolean equals(Object o)
	{
		if(o==null)
			return false;
		if(o==this)
			return true;
		if(o.getClass()!=getClass())
			return false;
		
		Flag f = (Flag)o;
		
		return f.id ==id;
	}
	
	public static Flag getFlag(int id)
	{
		if(id==WRITTEN.id)
			return WRITTEN;
		
		return null;
	}
}

class ListArray extends List
{
	private long index;
	private long[] list;
	private Block block_count_list;
	private PacketType type;
	private String work_dir;
	private Map<Integer, Integer> flags;
	private long[] count_flag_elems;
	private final int shift_memory_value;
	private boolean count_memory;
	private boolean count_file;
	private boolean count_null;
	private Set<Integer> count_flags;
	private long count_memory_elems;
	private long count_file_elems;
	private long count_null_elems;
	private Flag[] flag_list;
	private final String block_count_parms;
	private final String complete_parms;
	
	public ListArray(String work_dir, PacketType type, long index, Flag[] flags, String block_count, String complete_parms)
	{
		this.work_dir = work_dir;
		this.type = type;
		this.index = index;
		this.count_memory_elems = 0;
		this.count_file_elems = 0;
		this.count_null_elems = 0;
		this.flag_list = flags;
		this.block_count_parms = block_count;
		this.complete_parms = complete_parms;
		
		if(complete_parms==null)
			throw new IllegalArgumentException();
		
		int size = 0;
		if(flags!=null)
		{
			HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
			for(int i=0; i<flags.length; i++)
			{
				if(map.get(flags[i].id)!=null)
					throw new IllegalArgumentException();
				
				map.put(flags[i].id, i);
				
				this.flags = Collections.unmodifiableMap(map);
				this.count_flag_elems = new long[flags.length];
			}
			size = map.size();
		}
		this.shift_memory_value = size + 1;
		
		if(block_count!=null)
		{
			HashSet<Integer> hashSet = new HashSet<Integer>(); 
			
			block_count = block_count.replaceAll(" ", "");
			
			String[] elems = block_count.split(";");
			for(String e : elems)
			{
				if(e.equals("f"))
				{
					this.count_file = true;
					
				}else if(e.equals("m"))
				{
					this.count_memory = true;
				}else if(e.equals("n"))
				{
					this.count_null = true;
					
				}else
				{
					try
					{
						int flag_id = Integer.parseInt(e);
						if(this.flags==null || this.flags.get(flag_id)==null)
						{
							throw new IllegalArgumentException("Illegal count string <<"+e+">> is not an accepted flag");
						}
						if(hashSet.contains(flag_id))
							continue;
						
						hashSet.add(flag_id);
					}catch(NumberFormatException ex)
					{
						throw new IllegalArgumentException("Illegal count string <<"+e+">> not reconized as a flag");
					}
				}
			}
			this.count_flags = Collections.unmodifiableSet(hashSet);
		}

	}
	
	public ListArray(String work_dir, PacketType type, Flag[] flags, String block_count, String complete_parms)
	{
		this(work_dir, type, 0, flags, block_count, complete_parms);
	}
	
	public ListArray(String work_dir, PacketType type, long index, String block_count, String complete_parms)
	{
		this(work_dir, type, index, null, block_count, complete_parms);
	}
	
	private ListArray(String work_dir, PacketType type, String block_count, String complete_parms)
	{
		this(work_dir, type, 0, block_count, complete_parms);
	}
	
	private void setArrays(long nb_packet, long nb_packet_block)
	{
		this.list = new long[(int)nb_packet];
		this.block_count_list = new Block(nb_packet, nb_packet_block);
		this.count_null_elems = list.length;
		if(count_null)
		{
			for(int i=0; i<list.length; i++)
			{
				block_count_list.up(i);
			}
		}
	}
	
	private void setArrays(Manifest manifest, PacketType type, long nb_packet_block)
	{
		long nb_packet = Environment.getNumberPackets(manifest, type);
		if(nb_packet>=Integer.MAX_VALUE)
			throw new IndexOutOfBoundsException();
		
		setArrays(nb_packet, nb_packet_block);
	}

	public ListArray(String work_dir, Manifest manifest, PacketType type, long nb_packet_block, Flag[] flags, String block_count, String complete_parms)
	{
		this(work_dir, type, flags, block_count, complete_parms);
		
		setArrays(manifest, type, nb_packet_block);
	}
	
	public ListArray(String work_dir, Manifest manifest, ListFileInfo file_info)
	{
		this(work_dir, file_info.type, file_info.index, file_info.flags, file_info.blockCountParms, file_info.completeParms);
		
		setArrays(manifest, type, file_info.nbPacketBlock);
	}
	
	public Pos get(long index)
	{
		if(index>=list.length)
			throw new IndexOutOfBoundsException();

		long value = list[(int)index];
		if(isFileValue(value))
			return new Pos(value-1, Pos.FILE_TYPE);
		else if(isMemoryValue(value))
			return new Pos(-value-shift_memory_value, Pos.MEMORY_TYPE);
		else if(isFlagValue(value))
			return new Pos(getFlag(value).id, Pos.FLAG_TYPE);
		else
			return null;
	}
	
	private void update(long index, long value)
	{
		if(isMemoryValue(value))
	    {
			count_memory_elems++;
			if(count_memory)
				block_count_list.up(index);
	    }else if(isFileValue(value))
	    {
	    	if(count_file)
	    		block_count_list.up(index);
	    	count_file_elems++;
	    }else if(isFlagValue(value))
	    {
		   int fi = getFlagIndex(value);
		   
		   if(count_flags!=null && count_flags.contains(flag_list[fi].id))
		   {
			   block_count_list.up(index);
		   }
		   count_flag_elems[fi]++;
	    }else if(value==0)
	    {
	    	count_null_elems++;
	    	if(count_null)
	    	{
	    		block_count_list.up(index);
	    	}
	    }
	}
	
	private void set(long index, long value)
	{
		if(index>=list.length)
			throw new IndexOutOfBoundsException();
	   
	   if(list[(int)index]==0)
	   {
		   count_null_elems--;
		   if(value!=0 && count_null)
			   block_count_list.low(index);
		   
		   update(index, value);
		   
	   }else if(isMemoryValue(list[(int)index]) && !isMemoryValue(value))
	   {
		   if(count_memory)
			   block_count_list.low(index);
		   
		   count_memory_elems--;
		   update(index, value);
		   
	   }else if(isFileValue(list[(int)index]) && !isFileValue(value))
	   {
		   if(count_file)
			   block_count_list.low(index);
		   
		   count_file_elems--;
		   update(index, value);
		   
	   }else if(isFlagValue(list[(int)index]))
	   {
		   int fi = getFlagIndex(list[(int)index]);
		   if(count_flags!=null && count_flags.contains(flag_list[fi].id))
		   {
			   block_count_list.low(index);
		   }
		   count_flag_elems[fi]--;
		   
		   update(index, value);
	   }
	   
	   list[(int)index] = value;
	}
	
	public void setFile(long index, long value)
	{
		if(value<0)
			throw new IllegalArgumentException();
		
		value++;
		set(index, value);
	}
	
	private boolean isFileValue(long value)
	{
		return value>0;
	}
	
	
	public void setMemory(long index, long value)
	{
		if(value<0)
			throw new IllegalArgumentException();
		
		value = -value -shift_memory_value;//allow 0 for both in file and in memory index
		set(index, value);
	}
	
	private boolean isMemoryValue(long value)
	{
		return value<=-shift_memory_value;
	}
	
	public void setFlag(long index, int flag_id)
	{
		if(flags.get(flag_id)==null)
			throw new IllegalArgumentException();
		
		int flag_index = flags.get(flag_id);
		flag_index = -flag_index-1;
		set(index, flag_index);
	}
	
	public void setFlag(long index, Flag flag)
	{
		setFlag(index, flag.id);
	}
	
	private boolean isFlagValue(long value)
	{
		return value<0 && value>=(-shift_memory_value+1);
	}
	
	private int getFlagIndex(long value)
	{
		return -(int)value-1;
	}
	
	private Flag getFlag(long value)
	{
		int fi = getFlagIndex(value);
		return flag_list[fi];
	}
	
	public void setDefault(long index)
	{
		set(index, 0);
	}
	
	
	public long getCountNull()
	{
		return count_null_elems;
	}
	
	public long getCountMemory()
	{
		return count_memory_elems;
	}
	
	public long getCountFile()
	{
		return count_file_elems;
	}
	
	public long getCountFlag(int flag_id)
	{
		if(flags==null || flags.get(flag_id)==null)
			return 0;
		
		int fi = flags.get(flag_id);
		return count_flag_elems[fi];
	}
	
	public long getCountNotNull()
	{
		return list.length-count_null_elems;
	}
	
	public PacketType getType()
	{
		return type;
	}
	
	public long length()
	{
		return list.length;
	}
	
	public Flag[] getFlags()
	{
		return flag_list;
	}
	
	public boolean isAcceptedFlag(int flag_id)
	{
		return (flags.get(flag_id)!=null);
	}
	
	public boolean isAcceptedFlag(Flag flag)
	{
		return flag!=null && isAcceptedFlag(flag.id);
	}
	
	
	public String getBlockCountParms()
	{
		return block_count_parms;
	}
	
	public String getCompleteParms()
	{
		return complete_parms;
	}
	
	public long getBlockCount(long index)
	{
		return block_count_list.getBlockCount(index);
	}
	
	public boolean isInBlockIndex(long index)
	{
		return getBlockCount(this.index)==getBlockCount(index);
	}
	
	public void close(boolean save_objects) throws IOException
	{
		if(save_objects)
			ListFile.toFile(work_dir, this);
	}
	
	public long getIndex()
	{
		return index;
	}
	
	public long nextIndex()
	{
		index++;
		return index;
	}
	
	public Pos getIndexItem()
	{
		return get(index);
	}
	
	public boolean isIndex(long index)
	{
		return this.index==index;
	}
	
	public long getBlockLength()
	{
		return block_count_list.getRange();
	}
	
	public static boolean isCompleted(ListArray list)
	{
		if(list==null)
			throw new IllegalArgumentException();
		
		String complete_parms = list.getCompleteParms();
		if(complete_parms==null)
			throw new IllegalArgumentException();
		
		if(complete_parms.equals("f"))
			return list.getCountFile()==list.length();
		else if(complete_parms.equals("m"))
			return list.getCountMemory()==list.length();
		else
		{
			try
			{
				int flag_id = Integer.parseInt(complete_parms);
				if(!list.isAcceptedFlag(flag_id))
					throw new IllegalArgumentException();
				
				return list.getCountFlag(flag_id)==list.length();
				
			}catch(NumberFormatException e)
			{
				throw new IllegalArgumentException();
			}
		}
	}
}
