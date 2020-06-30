package Metadata;
import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;

import EnvVariables.Environment;
import GeneralTools.Serialization;
import GeneralTools.StreamData;
import GeneralTools.Tools;
import SendTools.RandomAccessByteArrayInputStream;
import SendTools.RandomAccessInputStream;

public abstract class Metadata implements Serializable
{
	private static final long serialVersionUID = 1234L;
	
	public Metadata() {}
	
	
	public static Object getMetadata(byte[] metadata_buffer) throws IOException
	{
		return Serialization.deserialize(metadata_buffer);
	}
	
	public static Metadata getMetadata(File metadata_file) throws IOException
	{
		return getMetadata(metadata_file);
	}
	
	public static Metadata getMetadata(String metadata_filename) throws IOException
	{
		return getMetadata(new File(metadata_filename));
	}
	
	public StreamData getStream()
	{
		byte[] b_metadata = Serialization.serialize(this);
		ByteArrayInputStream metadata_stream = new ByteArrayInputStream(b_metadata);
		
		return new StreamData(metadata_stream, b_metadata.length, StreamData.CONTINUE_STREAM);
	}
	
	public StreamData getRandomAccessStream()
	{
		byte[] b_metadata = Serialization.serialize(this);
		RandomAccessByteArrayInputStream metadata_stream = new RandomAccessByteArrayInputStream(b_metadata);
		
		return new StreamData(metadata_stream, b_metadata.length, StreamData.RANDOM_ACCESS_STREAM);
	}
}


/*public byte[] writeToBytesArray()
{
	byte[] metadata = Serialization.serialize(this);
	byte[] b_size = Serialization.intToBytes(metadata.length);
	byte[] data = new byte[Integer.BYTES  + metadata.length];

	ByteBuffer buff = ByteBuffer.wrap(data);
	buff.put(b_size);
	buff.put(metadata);
	
	return buff.array();
}

public static byte[] readFromStream(InputStream stream) throws IOException
{
	byte[] b_size = new byte[Integer.BYTES];
	stream.read(b_size);
	
	int size = Serialization.bytesToInt(b_size);
	if(size<=0)
		return null;
	
	
	byte[] data = new byte[size];
	stream.read(data);
	
	return data;
}


/*public InputStream writeToStream()
{
	//metadata to bytes
	byte[] tmp = Serialization.serialize(this);
	InputStream metadata_stream = new ByteArrayInputStream(tmp);
	
	//size of metadata bytes array to byte array
	InputStream metadata_class_info_stream = new ByteArrayInputStream(Serialization.intToBytes(tmp.length));
	//merge stream (size metadata class + metadata class)
	return new SequenceInputStream(metadata_class_info_stream, metadata_stream);
}*/