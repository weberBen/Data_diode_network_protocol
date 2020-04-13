package SendTools;
import java.io.BufferedInputStream;


import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import EnvVariables.Environment;
import Exceptions.InvalidPacketStreamException;
import Metadata.Metadata;
import PacketConstructor.Manifest;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketType;
import Metadata.DataType;
import PacketTools.Checksum;
import PacketTools.MissingPackets;
import PacketTools.Range;
import generalTools.Serialization;
import generalTools.StreamData;
import generalTools.Tools;

public class PacketRecovery extends DatagramSender
{
	
	private RandomAccessInputStream data_stream;
	private boolean manifest_send;
	private Manifest manifest;
	private RandomAccessInputStream metadata_stream;
	private RandomAccessInputStream current_stream;
	private int subpacket_size;
	private InputStreamBuilder metadata_builder;
	private InputStreamBuilder data_builder;
	private MissingPackets missing_packets;
	public List<Range> current_packets_list;
	private int index_packets_list;
	private long cursor_range;
	private PacketType current_type;
	

	
	
	public ArrayList<Range>[] packetsList_list;
	public int index_packetsList_list;
	
	//packet size must be the same as previous
	public PacketRecovery(long id, RandomAccessInputStream data_to_resend, int subpacket_size, Metadata metadata_to_resend, MissingPackets missing_packets)
	{	
		//check parameters
		List<Range> meta_list;
		List<Range> data_list;
		
		if(missing_packets.getMetadataPackets()!=null && missing_packets.getMetadataPackets().size()==0)
		{
			meta_list = null;
		}else
		{
			meta_list = missing_packets.getMetadataPackets();
		}
		
		if(missing_packets.getDataPackets()!=null && missing_packets.getDataPackets().size()==0)
		{
			data_list = null;
		}else
		{
			data_list = missing_packets.getDataPackets();
		}

		if((meta_list==null && data_list==null)
		   || (metadata_to_resend!=null && meta_list==null)
		   || (metadata_to_resend==null && meta_list!=null))
		{
			throw new IllegalArgumentException();
		}
		
		
			
		
		long length_data = missing_packets.getNumberPackets()*subpacket_size;
		
		if(metadata_to_resend!=null)
		{
			//merge metadata stream and data stream
			StreamData sd = metadata_to_resend.getRandomAccessStream();
			metadata_stream = (RandomAccessInputStream)sd.getStream();
		}else
		{
			metadata_stream = null;
		}
		
		
	    
		this.manifest = new Manifest(id, length_data, subpacket_size, DataType.PacketsRecovery, 0);
		this.data_stream = data_to_resend;
		this.manifest_send = false;
		this.subpacket_size = subpacket_size;
		
		
		this.missing_packets = missing_packets;
		
		
		this.current_stream = metadata_stream;
		this.current_packets_list = missing_packets.getMetadataPackets();
		this.index_packets_list = 0;
		current_type = PacketType.Metadata();
		if(current_stream==null || current_packets_list==null || current_packets_list.size()==0)
		{
			current_type = PacketType.Data();
			current_stream = data_stream;
			current_packets_list = missing_packets.getDataPackets();
			index_packets_list = 0;
			cursor_range = current_packets_list.get(index_packets_list).start;
		}else
		{
			this.cursor_range = current_packets_list.get(index_packets_list).start;
		}
		if(current_stream==null || current_packets_list==null || current_packets_list.size()==0)
		{
			current_packets_list = null;
		}
		
		
		if(metadata_stream==null)
		{
			this.metadata_builder = null;
		}else
		{
			
			this.metadata_builder = new InputStreamBuilder(manifest, metadata_stream, subpacket_size, Environment.PACKET_START_INDEX, PacketType.Metadata());
		}
		
		if(data_stream==null)
		{
			this.data_builder = null;
			
		}else
		{
			this.data_builder = new InputStreamBuilder(manifest, data_stream, subpacket_size, Environment.PACKET_START_INDEX, PacketType.Data());
		}
	}
	
	
	public byte[] getPacket() throws IOException, InvalidPacketStreamException
	{
		//send the manifest first
		if(!manifest_send)
		{
			//create stream
			byte[] data = Serialization.serialize(this.manifest);
			InputStream tmp = new ByteArrayInputStream(data);
			
			//create header
			InputStreamBuilder builder = new InputStreamBuilder(manifest, tmp, data.length, Environment.PACKET_START_INDEX, PacketType.Manifest());
			
			//get output
			byte[] output = builder.getNextPacket();//only one packet
			tmp.close();
			if(output==null)
				throw new InvalidPacketStreamException();
			
			manifest_send = true;
			return output;
		}
		
		
		if(current_packets_list==null)
			return null;
		
		
		if(index_packets_list>=current_packets_list.size())
		{
			if(current_packets_list == missing_packets.getDataPackets())
				return null;
			
			current_stream = data_stream;
			current_packets_list = missing_packets.getDataPackets();
			index_packets_list = 0;
			current_type = PacketType.Data();
			
			if(current_stream==null || current_packets_list==null || current_packets_list.size()==0)
			{
				return null;
			}
			cursor_range = current_packets_list.get(index_packets_list).start;
		}
		
		
		byte[] output = getPacket(current_type, cursor_range);
		
		cursor_range++;
		if(cursor_range>current_packets_list.get(index_packets_list).end)
		{
			index_packets_list++;
			if(index_packets_list<current_packets_list.size())
			{
				cursor_range = current_packets_list.get(index_packets_list).start;
			}
		}
		
		return output;
	}
	
	
	private byte[] getPacket(PacketType type, long p_index) throws IOException, InvalidPacketStreamException
	{
		System.out.println("type="+type+"   | p_index="+p_index);
		
		
		if(type.isMetadata())
		{
			metadata_stream.seek((p_index-1)*subpacket_size);
			return metadata_builder.getNextPacket(p_index);//return only one packet
			
		}else if(type.isData())
		{
			data_stream.seek((p_index-1)*subpacket_size);
			return data_builder.getNextPacket(p_index);//return only one packet
		}
		
		return null;
	}
}
