package SendTools;
import java.io.BufferedInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import EnvVariables.Environment;
import EnvVariables.Parms;
import Exceptions.InvalidPacketStreamException;
import Metadata.FileMetadata;
import Metadata.Metadata;
import PacketConstructor.PacketBufferInfo;
import Metadata.DataType;
import PacketTools.MissingPackets;
import generalTools.NetworkAddress;
import generalTools.Tools;

public class Sender 
{
	private DatagramSocket socket;
	private int block_size;
	
	public Sender() throws SocketException, InvalidPacketStreamException
	{
		this.socket = new DatagramSocket();
		
		this.block_size = Math.min(Parms.instance().sender().getMMUPacketLength(), Parms.instance().sender().getMMUPacketLength()-PacketBufferInfo.getSize(0)-Environment.BYTES_UDP_HEADER);
		if(this.block_size<=0)
			throw new InvalidPacketStreamException();
	}
	
	
	
	private void send(InputStream input_stream, long data_length, DataType type, Metadata metadata, NetworkAddress address, long rate) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		PacketSender stream = new PacketSender(input_stream, data_length, block_size, type, metadata);
		
		sendPackets(stream, address, rate);
	}
	
	private void send(InputStream input_stream, long data_length, DataType type, Metadata metadata) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		send(input_stream, data_length, type, metadata, Parms.instance().getNetworkConfig(), Parms.instance().sender().getSendRate());
	}
	
	
	
	
	private void sendMissing(long id, RandomAccessByteArrayInputStream input_stream, Metadata metadata, MissingPackets missing_packets, NetworkAddress address, long rate) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		PacketRecovery stream = new PacketRecovery(id, input_stream, block_size, metadata, missing_packets);
		
		sendPackets(stream, address, rate);
	}
	
	private void sendMissing(long id, RandomAccessByteArrayInputStream input_stream, Metadata metadata, MissingPackets missing_packets) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		sendMissing(id, input_stream, metadata, missing_packets, Parms.instance().getNetworkConfig(), Parms.instance().sender().getSendRate());
	}
	
	
	
	
	private void sendPackets(DatagramSender stream, NetworkAddress address, long rate) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		DatagramPacket dp ;
		byte[] data;
		int port = address.getPort();
		InetAddress ip = address.getIp();
		
		
		data = stream.getPacket();
		dp = new DatagramPacket(data, data.length, ip, port);
		socket.send(dp);//send manifest
		
		Thread.sleep(6000);
		
		
		long count = 0;
		
		int index = 0;
		data = stream.getPacket();
		while(data!=null)
		{
			//System.out.println("loop...");
			
			if(count%1000==0)
				System.out.println("packet "+count+" send");
			
			dp = new DatagramPacket(data, data.length, ip, port);
			index++;
			socket.send(dp);
				
			count++;
			
			Tools.sleep(rate);  
			
			data = stream.getPacket();
		}
	}
	
	public void sendMissing(long id, String msg, MissingPackets missing_packets, NetworkAddress address, long rate) throws InterruptedException, IOException, InvalidPacketStreamException
	{
		System.out.println("send missing string");
		
		RandomAccessByteArrayInputStream input_stream = new RandomAccessByteArrayInputStream(msg.getBytes(Environment.STRING_CODEX));
		sendMissing(id, input_stream, null, missing_packets);
		
		input_stream.close();
	}
	
	public void sendMissing(long id, String msg, MissingPackets missing_packet) throws InterruptedException, IOException, InvalidPacketStreamException
	{
		sendMissing(id, msg, missing_packet, Parms.instance().getNetworkConfig(), Parms.instance().sender().getSendRate());
	}
	
	public void send(String msg, NetworkAddress address, long rate) throws InterruptedException, IOException, InvalidPacketStreamException
	{
		System.out.println("send string");
		
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes(Environment.STRING_CODEX));
		send(input_stream, msg.length(), DataType.PlainTxt, null);
		
		input_stream.close();
	}
	
	public void send(String msg) throws InterruptedException, IOException, InvalidPacketStreamException
	{
		send(msg, Parms.instance().getNetworkConfig(), Parms.instance().sender().getSendRate());
	}
	
	public void send(File file, NetworkAddress address, long rate) throws IOException, InterruptedException, InvalidPacketStreamException
	{	
		System.out.println("send file");
		FileMetadata metadata = new FileMetadata(file);
		
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream data_stream = new BufferedInputStream(fis);
		
		//send data
		send(data_stream, file.length(), DataType.File, metadata);
		data_stream.close();
	}
	
	public void send(File file) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		send(file, Parms.instance().getNetworkConfig(), Parms.instance().sender().getSendRate());
	}
	
	public void close()
	{
		socket.close();
	}

}
