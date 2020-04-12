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

public class Sender 
{
	private InetAddress ip;
	private int port;
	private DatagramSocket socket;
	private long rate; //bytes rate
	private int block_size;
	
	public Sender(InetAddress ip, int port, long rate) throws SocketException, InvalidPacketStreamException
	{
		this.ip = ip;
		this.port = port;
		this.rate = rate;
		this.socket = new DatagramSocket();
		
		this.block_size = Math.min(Parms.instance().sender().getMMUPacketLength(), Parms.instance().sender().getMMUPacketLength()-PacketBufferInfo.getSize(0));
		if(this.block_size<=0)
			throw new InvalidPacketStreamException();
	}
	
	private void send(InputStream input_stream, long data_length, DataType type, Metadata metadata) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		PacketSender stream = new PacketSender(input_stream, data_length, block_size, type, metadata);
		
		sendPackets(stream);
	}
	
	private void sendMissing(long id, RandomAccessByteArrayInputStream input_stream, Metadata metadata, MissingPackets missing_packets) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		PacketRecovery stream = new PacketRecovery(id, input_stream, block_size, metadata, missing_packets);
		
		sendPackets(stream);
	}
	
	public void testWait(){
	    final long INTERVAL = 10000;
	    long start = System.nanoTime();
	    long end=0;
	    do{
	        end = System.nanoTime();
	    }while(start + INTERVAL >= end);
	    System.out.println(end - start);
	}
	
	
	private void sendPackets(DatagramSender stream) throws IOException, InterruptedException, InvalidPacketStreamException
	{
		DatagramPacket dp ;
		int index=1;
		byte[] data = stream.getPacket();
		dp = new DatagramPacket(data, data.length, ip, port);
		socket.send(dp);//send manifest
		Thread.sleep(6000);
		
		long count = 0;
		data = stream.getPacket();
		int total = 1;
		int start, end;
		boolean ok=true;
		
		while(data!=null)
		{
			//System.out.println("loop...");
			
			if(ok)
			{
				ok = false;
				start = 0;
				end = total/2;
			}else
			{
				ok = true;
				start = total/2;
				end = total;
			}
			
			for(int i=start; i<end; i++)
			{
				System.out.println("packet "+index+" send");index++;
				dp = new DatagramPacket(data, data.length, ip, port+i);
				socket.send(dp);
				
				data = stream.getPacket();
				if(data==null)
					break;
					
				count++;
				
			}
			
			
			//Thread.sleep(50);
			testWait();  
			
		}
	}
	
	public void sendMissing(long id, String msg, MissingPackets missing_packets) throws InterruptedException, IOException, InvalidPacketStreamException
	{
		System.out.println("send missing string");
		
		RandomAccessByteArrayInputStream input_stream = new RandomAccessByteArrayInputStream(msg.getBytes(Environment.STRING_CODEX));
		sendMissing(id, input_stream, null, missing_packets);
		
		input_stream.close();
	}
	
	public void send(String msg) throws InterruptedException, IOException, InvalidPacketStreamException
	{
		System.out.println("send string");
		
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes(Environment.STRING_CODEX));
		send(input_stream, msg.length(), DataType.Clipboard, null);
		
		input_stream.close();
	}
	
	public void send(File file) throws IOException, InterruptedException, InvalidPacketStreamException
	{	
		System.out.println("send file");
		FileMetadata metadata = new FileMetadata(file);
		
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream data_stream = new BufferedInputStream(fis);
		
		//send data
		send(data_stream, file.length(), DataType.File, metadata);
		data_stream.close();
	}

}
