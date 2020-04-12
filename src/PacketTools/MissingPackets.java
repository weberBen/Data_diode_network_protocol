package PacketTools;

import java.io.File;
import java.util.ArrayList;

import EnvVariables.Environment;

public class MissingPackets 
{
	private ArrayList<Range> metadata_packets;
	private ArrayList<Range> data_packets;
	
	
	public MissingPackets(ArrayList<Range> metadata_packets, ArrayList<Range> data_packets)
	{
		this.metadata_packets = metadata_packets;
		this.data_packets = data_packets;
	}
	
	public long getNumberPackets()
	{
		return countRanges(metadata_packets)+countRanges(data_packets);
	}
	
	public long getNumberDataPackets()
	{
		return countRanges(data_packets);
	}
	
	public long getNumberMetadataPackets()
	{
		return countRanges(metadata_packets);
	}
	
	private long countRanges(ArrayList<Range> list)
	{
		if(list==null)
			return 0;
		
		long count = 0;
		for(Range range : list)
		{
			count+= range.end-range.start+1;
		}
		
		return count;
	}
	
	
	public ArrayList<Range> getMetadataPackets()
	{
		return metadata_packets;
	}
	
	public ArrayList<Range> getDataPackets()
	{
		return data_packets;
	}
	
	
	public static ArrayList<Range> getMetadataMissingPackets(String work_directory, long start_search_index, long number_packets)
	{
		Environment.RawPacketFilenameFromIndex int_data = (work_dir, index) -> { return Environment.getRawMetadataFilename(work_dir, index); };
		return findMissingPacket(work_directory, int_data, start_search_index, number_packets);
	}
	
	public static ArrayList<Range> getDataMissingPackets(String work_directory, long start_search_index, long number_packets)
	{
		Environment.RawPacketFilenameFromIndex int_data = (work_dir, index) -> { return Environment.getRawDataFilename(work_dir, index); };
		return findMissingPacket(work_directory, int_data, start_search_index, number_packets);
	}
	
	private static ArrayList<Range> findMissingPacket(String work_directory, Environment.RawPacketFilenameFromIndex interface_packet_filename, long start_search_index, long number_packets)
	{
		if(start_search_index<1)
			return null;
		
		String filename;
		File file;
		ArrayList<Range> missing_packets = new ArrayList<Range>();
		long start_missing=-1, end_missing=-1;
		boolean has_file = false;
		boolean last_file_exists = false;
      
		/* Since the number of file can be large, getting all the file inside directory
		 * will truncate a large amount of the memory (RAM), and since the test of existence 
		 * of a file run in constant time (with hash table), it's easier to just iterate through all
		 * the packet index to find the missing one
		 */
		for (long index=start_search_index; index <= number_packets; index++) 
		{
			filename = interface_packet_filename.getFilename(work_directory, index);
			System.out.println("filename ncbcbv="+filename);
			file = new File(filename);
			
			if(!file.exists())
			{
				if(start_missing==-1)
				{
					start_missing = index;
					end_missing = index;
				}else
				{
					end_missing = index;
				}
				last_file_exists = false;
			}else
			{
				if(start_missing!=-1)
				{
					missing_packets.add(new Range(start_missing, end_missing));
				}
				start_missing = -1;
				
				has_file = true;
				last_file_exists = true;
			} 
			
		}
		
		
		if(!has_file)
		{
			missing_packets.add(new Range(start_search_index, number_packets));
			
		}else if(!last_file_exists)
		{
			if(start_missing!=-1)
				missing_packets.add(new Range(start_missing, end_missing));
		}
		
		if(missing_packets.size()==0)//constant time operation
			return null;
		
		return missing_packets;
	}
}
