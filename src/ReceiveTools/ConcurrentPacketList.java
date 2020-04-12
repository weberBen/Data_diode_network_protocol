package ReceiveTools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import EnvVariables.Environment;
import EnvVariables.Parms;
import Exceptions.EndOfPacketReassemblingException;
import Exceptions.InvalidPacketStreamException;
import Exceptions.ReassemblingException;
import PacketConstructor.Manifest;
import PacketConstructor.PacketBufferInfo;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketReader;
import PacketConstructor.PacketType;
import PacketManager.InFilePacketManager;
import PacketManager.InMemoryPacketManager;
import PacketManager.PacketContent;
import PacketManager.PacketManager;
import PacketTools.Range;
import generalTools.Tools;

public class ConcurrentPacketList
{
	private PacketManager manager;
	private final PacketBufferInfo info;
	private LinkedBlockingQueue<ConcurrentPacketList.Container> queue;
	private AtomicBoolean reassembling;
	private final long timeout_nanosecond;
	private AtomicBoolean is_closed;
	private final String work_dir;
	private final Manifest manifest;
	private AtomicBoolean stop;
	
	public static final long DEFAULT_TIMEOUT_NANOSECOND = 10000000000L;//10s 
	
	public ConcurrentPacketList(String work_directory, Manifest manifest, PacketBufferInfo info, int number_packet_hold, int nb_packet_block, int buffer_size_file, long timeout_nanosecond) throws IOException
	{
		if(work_directory==null || manifest==null || info==null || number_packet_hold<=0 || nb_packet_block<=0 || buffer_size_file<=0 || timeout_nanosecond<=0)
			throw new IllegalArgumentException();

		
		if(manifest.type.isInMemory())
		{
			this.manager = new InMemoryPacketManager(manifest);
			this.work_dir = null;
		}
		else
		{
			InFilePacketManager tmp = new InFilePacketManager(work_directory, manifest, number_packet_hold, nb_packet_block);
			this.manager = tmp;
			this.work_dir = tmp.getWorkDirectory();
		}
		
		this.info = info;
		this.manifest = manifest;
		this.is_closed = new AtomicBoolean(false);
		this.timeout_nanosecond = timeout_nanosecond;
		this.reassembling = new AtomicBoolean(false);
		this.queue = new LinkedBlockingQueue<ConcurrentPacketList.Container>();
		this.stop = new AtomicBoolean(false);
	}
	
	public ConcurrentPacketList(String work_directory, Manifest manifest, PacketBufferInfo info, int number_packet_hold, int nb_packet_block, int buffer_size_file) throws IOException
	{
		this(work_directory, manifest, info, number_packet_hold, nb_packet_block, buffer_size_file, DEFAULT_TIMEOUT_NANOSECOND);
	}
	
	public ConcurrentPacketList(String work_directory, Manifest manifest, PacketBufferInfo info) throws IOException
	{
		this(work_directory, manifest, info, InFilePacketManager.DEFAULT_NUMBER_PACKET_TO_HOLD, InFilePacketManager.DEFAULT_NUMBER_PACKET_BLOCK, Parms.instance().getBufferFileSize(), DEFAULT_TIMEOUT_NANOSECOND);
	}
	
	public ConcurrentPacketList(String work_directory, Manifest manifest) throws IOException
	{
		this(work_directory, manifest, new PacketBufferInfo(manifest.blockSize), InFilePacketManager.DEFAULT_NUMBER_PACKET_TO_HOLD, InFilePacketManager.DEFAULT_NUMBER_PACKET_BLOCK, Parms.instance().getBufferFileSize(), DEFAULT_TIMEOUT_NANOSECOND);
	}
	
	
	private boolean addPacket(PacketHeader header, byte[] buffer) throws IOException
	{
		return manager.add(header, buffer, info.offsetData);
	}
	
	public void interputReassembly()
	{
		stop.set(true);
	}
	
	public void reassemble() throws IOException, EndOfPacketReassemblingException, TimeoutException, ReassemblingException, InterruptedException
	{
		if(is_closed.get())
			return;
		
		if(!reassembling.compareAndSet(false, true))
			return;
		
		Container container;
		while(!manager.isComplete() && !stop.get())
		{	
			container = queue.poll(timeout_nanosecond,  TimeUnit.NANOSECONDS);
			if(container==null)
			{//System.out.println("container null");
				throw new TimeoutException();
			}
			
			//System.out.println("\tadd type="+container.header.getType()+", index="+container.header.getIndex());
			try
			{
				addPacket(container.header, container.buffer);
				
			}catch(IllegalArgumentException e)
			{
				System.out.println("Conccurent list illegal argument exception");
			}
			//System.out.println("\t\t | missing="+manager.getNumberMissingPacket());
		}
		
		if(stop.compareAndSet(true, false))
			throw new CancellationException();
		
		if(manager.isComplete())
		{
			manager.update();
			reassembling.set(false);
			if(!manager.isReassemblyFinished())
			{
				throw new ReassemblingException();
			}
			throw new EndOfPacketReassemblingException();
		}
	}
	
	public boolean add(PacketHeader header, byte[] buffer)
	{
		if(is_closed.get())
			return false;
		//System.out.println("pool type="+header.getType()+", index="+header.getIndex());
		Container container = new Container(header, buffer);
		return queue.add(container);
	}
	
	public boolean add(byte[] buffer)
	{
		if(is_closed.get())
			return false;
		
		if(buffer==null)
			throw new IllegalArgumentException();
		
		PacketReader packet =  new PacketReader(info);
		if(!packet.depack(buffer))
		{
			//throw new InvalidPacketStreamException();
			return false;
		}
		
		if(packet.getHeader().getStreamId()!=manifest.id)
			return false;
		
		return add(packet.getHeader(), packet.getBuffer());
	}
	
	
	public long getTotalNumberPacket()
	{
		return manager.totalLength();
	}
	
	public long getCurrentNumberPacket()
	{
		return manager.length();
	}
	
	public void flush() throws IOException
	{
		if(is_closed.get())
			return;
		
		manager.flush();
	}
	
	public void close(boolean save_objects) throws IOException
	{
		if(!is_closed.compareAndSet(false, true))
			return;
		
		manager.close(save_objects);
	}
	
	public void close() throws IOException
	{
		close(true);
	}
	
	public Manifest getManifest()
	{
		return manager.getManifest();
	}
	
	public PacketContent getContent(int type_id)
	{
		return manager.getContent(type_id);
	}
	
	public void clear() throws IOException
	{
		close(false);
		
		File dir = new File(work_dir);
		if(dir.exists())
		{
			Tools.purgeDirectory(dir);
			dir.delete();
		}
	}
	
	private static class Container
	{
		public final PacketHeader header;
		public final byte[] buffer;
		
		public Container(PacketHeader header, byte[] buffer)
		{
			this.header = header;
			this.buffer = buffer;
		}
	}
	
}

