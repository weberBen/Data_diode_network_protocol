package ReceiveTools;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpSocket 
{
	private final String hostname;
	private final int port;
	private final int max_buffer_size;
	private Pointer ptr;
	private AtomicBoolean is_opened;
	
	static { System.load( (new File(System.getProperty("java.class.path"))).getParent() + File.separator + "jni"  + File.separator + "libmaLib.so" ); }
	
	public UdpSocket(String hostname, int port, int max_buffer_size)
	{
		if(hostname==null || port<0 || max_buffer_size<0)
			throw new IllegalArgumentException();
		
		this.ptr = null;
		this.is_opened = new AtomicBoolean(false);
		this.hostname = hostname;
		this.port = port;
		this.max_buffer_size = max_buffer_size;
		
		Thread shutdownThread = new Thread(() -> shutdownHandler(this));
		Runtime.getRuntime().addShutdownHook(shutdownThread);
	}
	
	private static void shutdownHandler(UdpSocket instance) 
	{
		try 
		{
			instance.close();
			
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	
	public void open() throws InterruptedException,OutOfMemoryError
	{
		if(!is_opened.compareAndSet(false, true))
		{
			return;
		}
		
		this.ptr = openUdpSocket(hostname, port, max_buffer_size);
	}
	
	public void close() throws InterruptedException,OutOfMemoryError
	{
		System.out.println("closing...");
		if(ptr==null)
			return;
		closeUdpSocket(ptr);
	}
	
	public byte[] getReceivedPacket() throws InterruptedException, OutOfMemoryError
	{
		if(ptr==null)
			return null;
		
		return getUdpReceivedPacket(ptr);
	}
	
	public void dropAllReceivedPackets() throws InterruptedException, OutOfMemoryError
	{
		if(ptr==null)
			return ;
		
		dropAllUdpReceivedPackets(ptr);
	}
	
	
	private native Pointer openUdpSocket(String hostname, int port, int max_buffer_size) throws InterruptedException;
	private native byte[] getUdpReceivedPacket(Pointer ptr) throws InterruptedException, OutOfMemoryError;
	private native void closeUdpSocket(Pointer ptr) throws InterruptedException, OutOfMemoryError;
	private native void dropAllUdpReceivedPackets(Pointer ptr)  throws InterruptedException, OutOfMemoryError;
	
}

class Pointer 
{
	private long address;
	
	public Pointer(){}
}
