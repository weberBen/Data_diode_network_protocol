package SendTools;
import java.io.BufferedInputStream;





import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Calendar;

import EnvVariables.Environment;
import Exceptions.InvalidPacketStreamException;
import Metadata.Metadata;
import PacketConstructor.Manifest;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketType;
import Metadata.DataType;
import PacketTools.Checksum;
import generalTools.Serialization;
import generalTools.StreamData;

public class PacketSender extends DatagramSender
{	
	private InputStream data_stream;
	private long index;
	private Manifest manifest;
	private InputStream metadata_stream;
	private int subpacket_size;
	private InputStreamBuilder outputStreamBuilder;
	private long total_length;
	
	public PacketSender(InputStream stream, long length_data, int subpacket_size, DataType type, Metadata metadata)
	{	
		//check parameters
		if(type==null || stream==null)
			throw new IllegalArgumentException();
		
		if((type.hasMetadata() && metadata==null) || (!type.hasMetadata() && metadata!=null)
			|| !type.metadataClassEqual(metadata))
		{
			throw new IllegalArgumentException();
		}
		
		
		long length_metadata;
		if(metadata!=null)
		{
			//merge metadata stream and data stream
			StreamData sd = metadata.getStream();
			metadata_stream = sd.getStream();
			length_metadata = sd.getLength();
		}else
		{
			metadata_stream = null;
			length_metadata = 0;
		}
	    
		this.manifest = new Manifest(System.nanoTime(), length_data, subpacket_size, type, length_metadata);
		this.data_stream = stream;
		this.index = 0;
		this.subpacket_size = subpacket_size;
		this.outputStreamBuilder = null;
		this.total_length = length_metadata + length_data; 
	}
	
	public long getTotalLength()
	{
		return total_length;
	}

	public byte[] getPacket() throws IOException, InvalidPacketStreamException
	{
		//send the manifest first
		if(index==0)
		{
			//create stream
			StreamData sd = manifest.getStream();
			
			//create header
			InputStreamBuilder builder = new InputStreamBuilder(manifest, sd.getStream(), (int)sd.getLength(), Environment.PACKET_START_INDEX, PacketType.Manifest());
			
			//get output
			byte[] output = builder.getNextPacket();//only one packet
			sd.getStream().close();
			if(output==null)
				throw new InvalidPacketStreamException();
			index++;
			return output;
		}
		
		if(index==1)
		{
			if(metadata_stream!=null)
			{
				outputStreamBuilder = new InputStreamBuilder(manifest, metadata_stream, subpacket_size, Environment.PACKET_START_INDEX, PacketType.Metadata());
			}
			else
			{
				outputStreamBuilder = new InputStreamBuilder(manifest, data_stream, subpacket_size, Environment.PACKET_START_INDEX, PacketType.Data());
			}
		}
		

		byte[] output = outputStreamBuilder.getNextPacket();
		if(output==null && metadata_stream!=null)//end of metadata stream
		{
			metadata_stream.close();
			metadata_stream = null;
			
			if(data_stream==null)
				return null;
			
			outputStreamBuilder = new InputStreamBuilder(manifest, data_stream, subpacket_size, Environment.PACKET_START_INDEX, PacketType.Data());
			output = outputStreamBuilder.getNextPacket();
		}
		
		if(output==null)
		{
			data_stream.close();
		}
		index++;
		
		if(output!=null)
		{
			System.out.println("\tindex="+index+"   | length="+output.length);
			return output.clone();
		}else
		{
			return null;
		}
	}
	
}
