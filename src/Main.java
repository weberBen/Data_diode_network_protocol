import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.SwingWorker;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUnderflowException;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

import EnvVariables.Environment;
import EnvVariables.Parms;
import EnvVariables.SenderParms;
import Exceptions.InvalidPacketStreamException;
import Exceptions.MissingPacketsException;
import Exceptions.ReadingPacketException;
import GeneralTools.BufferedRandomAccessFile;
import GeneralTools.FilePreallocator;
import GeneralTools.NetworkAddress;
import GeneralTools.Serialization;
import GeneralTools.Tools;
import Metadata.ClipboardMetadata;
import Metadata.DataType;
import Metadata.FileMetadata;
import Metadata.Metadata;
import PacketConstructor.Manifest;
import PacketConstructor.Packet;
import PacketConstructor.PacketBufferInfo;
import PacketConstructor.PacketData;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketReader;
import PacketConstructor.PacketType;
import PacketManager.InFilePacketManager;
import PacketManager.InMemoryPacketManager;
import PacketManager.PacketContent;
import PacketManager.PacketManager;
import PacketTools.Checksum;
import PacketTools.MissingPackets;
import PacketTools.Range;
import ReceiveTools.Receiver;
import SendTools.PacketSender;
import SendTools.Sender;
import UserInterface.Menu;
import UserInterface.MenuItem;
import UserInterface.ReceiverInterface;
import UserInterface.UserMenu;

import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import javax.imageio.ImageIO;

public class Main 
{
	
	private static volatile Manifest manifest;
	private static final int nb_packet_to_hold = 255;
	private static final int nb_packet = nb_packet_to_hold*100;
	private static final int block_size = 1200;
	private static final PacketBufferInfo info = new PacketBufferInfo(block_size);
	private static final String work_directory =  "/home/benjamin/eclipse-workspace/Data_diode_network_protocol/test"; //System.getProperty("user.dir") + File.separator + "test";
	private static final byte[] init_data = new byte[nb_packet*block_size+3];
	
	public static ArrayList blockShuffle(ArrayList list, int block)
	{
		int previous = 0;
		int next=0;
		
		while(previous<list.size())
		{
			next = block; //(int)(Math.random()*block);
			int end = Math.min(previous+next, list.size());
			int start = previous;
			System.out.println("from="+start+"  to "+end);
			for(int i=start; i<end; i++)
			{
				int pos = (int)(Math.random()*(end-start) + start);
				Object o = list.get(i);
				list.set(i, list.get(pos));
				list.set(pos, o);
				
			}
			
			previous += next;
			if(previous>255)
				break;
		}
		
		return list;
	}
	
	private static class Container
	{
		public final PacketHeader header;
		public final byte[] buffer;
		private AtomicInteger pos;
		
		public Container(PacketHeader header, byte[] buffer)
		{
			this.header = header;
			this.buffer = buffer;
			this.pos = new AtomicInteger(0);
		}
	}
	
	private static class RRR implements Runnable
	{
		private StringBuffer string_buffer;
		private volatile boolean stop;
		
		public RRR(StringBuffer string_buffer)
		{
			this.string_buffer = string_buffer;
		}
		
		public void stop()
		{
			stop = false;
		}
		
		public void run()
		{
			while(!stop)
			{
				
			}
		}
	}
	
	private static class Reciver implements Runnable
	{
		private volatile boolean stop;
		private DatagramSocket socket;
		
		public Reciver() throws SocketException, UnknownHostException
		{
			stop = false;
			socket = new DatagramSocket(1652, InetAddress.getByName("169.254.8.186"));
			
		}
		
		public void stop()
		{
			stop = true;
			socket.close();
		}
		
		public void run()
		{
			byte[] data = new byte[1500];
			
			DatagramPacket dp = new DatagramPacket(data, data.length); 
			
			System.out.println("waiting manifest");
			try {
				socket.receive(dp);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return ;
			}
			
			PacketReader packerReader = new PacketReader(Manifest.BYTES);
			if(!packerReader.depack(dp.getData()))
			{
				System.out.println("manifest false");
				return;
			}
			
			//CircularBufferRing ring = new CircularBufferRing(10, 1500);
			
			System.out.println("manifest ok");
			
			Manifest manifest = PacketReader.getManifest(data);
			packerReader = new PacketReader(manifest.blockSize);
			
			byte[] buffer = new byte[1500*2];
			long count = 0;
			while(!stop)
			{
				dp.setLength(data.length);
				//dp.setData(ring.buffer, ring.getNewPos(), 1500);
				try {
					socket.receive(dp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
				count++;
				/*if(!packerReader.depack(dp.getData()))
				{
					System.out.println("packet false");
					break;
				}
				
				for(int i=0; i<packerReader.getHeader().getLength(); i++)
				{
					buffer[i] = data[i+packerReader.getOffsetData()];
				}*/
				
				if(count%10000==0)
					System.out.println("count="+count);
			}
			
			System.out.println("count="+count);
			
			socket.close();
		}
	}
	public static void main1245(String[] agrs) throws Exception
	{
		Reciver rr = new Reciver();
		
		Thread t = new Thread(rr);
        t.setDaemon(true);//stop at the end of the JVM
        t.start(); 
        
        System.in.read();
        
        rr.stop();
        t.join();
		
		System.out.println("end");
	}
	
	
	public static class UnsignedIntegerFifo
	{
		private final int size;
		private AtomicInteger count;
		private ArrayBlockingQueue<Integer> queue;
		private IntUnaryOperator dec_unary_op;
		private IntUnaryOperator inc_unary_op;
		
		public UnsignedIntegerFifo(int size)
		{
			this.count = new AtomicInteger(0);
			this.size = size;
			this.queue = new ArrayBlockingQueue<Integer>(size);
			
			this.dec_unary_op = new IntUnaryOperator()
			{
				@Override
				public int applyAsInt(int a) 
				{
					return Math.min(0, a-1);
				}
		
			};
			
			this.inc_unary_op = new IntUnaryOperator()
			{
				@Override
				public int applyAsInt(int a) 
				{
					return Math.min(size, a+1);
				}
		
			};
		}
		
		public boolean add(int val)
		{
			if(val<0)
				return false;
			
			int current_size = count.getAndUpdate(inc_unary_op);
			if(current_size>=size)
				return false;
			
			boolean added = queue.add(val);
			if(!added)
			{
				count.getAndUpdate(dec_unary_op);
			}
			
			return added;
		}
		
		public int remove()
		{
			try
			{
				int current_size = count.getAndUpdate(dec_unary_op);
				if(current_size==0)
					return -1;
				
				return (int)queue.remove();
				
			}catch(NoSuchElementException  e)
			{
				return -1;
			}
			
		}
		
		public Iterator<Integer> iterator()
		{
			return new Iterator<Integer>(){

		            private Iterator iterator = queue.iterator();

		            @Override
		            public boolean hasNext() {
		                return iterator.hasNext();
		            }

		            @Override
		            public Integer next() {
		                return (int)iterator.next();
		            }

		            @Override
		            public void remove() {
		                throw new UnsupportedOperationException();
		            }
		        };
		}
		
	}
	
	private static class ConcurrentBuffer
	{
		public final int itemLength;
		private ArrayBlockingQueue<DatagramPacket> process_pos;
		private ArrayBlockingQueue<DatagramPacket> free_pos;
		public final int size;
		
		public ConcurrentBuffer(int nb_items, int item_size)
		{
			this.itemLength = item_size;
			this.process_pos = new ArrayBlockingQueue<DatagramPacket>(nb_items);
			this.free_pos = new ArrayBlockingQueue<DatagramPacket>(nb_items);
			
			for(int i=0; i<nb_items; i++)
			{
				byte[] buffer = new byte[item_size];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				free_pos.add(packet);
			}
			
			this.size = nb_items;
		}
		
		public DatagramPacket producerLock()
		{
			return free_pos.poll();
		}
		
		public boolean producerUnlock(DatagramPacket packet)
		{	
			//PRODUCER_VAL+="producer unlock index="+lock+"\n";
			return process_pos.add(packet);
		}
		
		
		public DatagramPacket consumerLock()
		{
			return process_pos.poll();
		}
		
		public boolean consumerUnlock(DatagramPacket packet)
		{
			return free_pos.add(packet);
		}
		
		
		public Iterator<DatagramPacket> iterator()
		{
			return new Iterator<DatagramPacket>(){
				
				private DatagramPacket _next = process_pos.poll();
				
	            @Override
	            public boolean hasNext() {
	                return (_next!=null);
	            }

	            @Override
	            public DatagramPacket next() {
	            	DatagramPacket val = _next;
	            	_next = process_pos.poll();
	                
	            	return val;
	            }

	            @Override
	            public void remove() {
	                throw new UnsupportedOperationException();
	            }
	        };
		}
		
	}
	
	private static AtomicInteger processed_count = new AtomicInteger(0);
	private static AtomicInteger received_count = new AtomicInteger(0);
	private static volatile boolean stop_rcp = false;
	
	private static class Runn implements Runnable
	{
		private DatagramSocket socket;
		private ConcurrentBuffer ring;
		private PacketReader packet_reader;
		
		public Runn(String hostname, int port, ConcurrentBuffer ring, PacketBufferInfo info) throws SocketException, UnknownHostException
		{
			this.socket = new DatagramSocket(port, InetAddress.getByName(hostname));
			this.ring = ring;
			this.packet_reader = new PacketReader(info);
		}
		
		public void stop()
		{
			socket.close();
		}
		
		public void run()
		{
			
			long count = 0;
			
			while(true)
			{
				//buffer = list_array.poll();
				DatagramPacket packet = ring.producerLock();
				if(packet==null)
				{
					//System.out.println("receiver dropped");
					continue;
				}
				
				try
				{
					socket.receive(packet);
					
					ring.producerUnlock(packet);
					//received_count.getAndIncrement();
					count++;
					if(count%10000==0)
						System.out.println("receive count="+count);
					if(stop_rcp)
						break;
				}catch(IOException e)
				{
					e.printStackTrace();
					break;
				}
			}
			System.out.println("receiver count="+count);
		}
		
	}
	
	private static class Runnp implements Runnable
	{
		private static int count = 0;
		private final int id;
		private ConcurrentBuffer ring;
		private volatile boolean stop;
		private PacketReader packet_reader;
		
		public Runnp(ConcurrentBuffer ring, PacketBufferInfo info)
		{
			this.id = this.count;
			this.count++;
			
			this.ring = ring;
			this.stop = false;
			
			this.packet_reader = new PacketReader(info);
		}
		
		public void stop()
		{
			this.stop = true;
		}
		
		public void run()
		{
			System.out.println("start process thread "+id);
			BufferedOutputStream stream = null;
			try {
				File file = new File(work_directory+ File.separator +"t_"+id);
				FileOutputStream fos = new FileOutputStream(file);
				stream = new BufferedOutputStream(fos, ring.itemLength*255);
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return ;
			}
			
			
			long count = 0;
			byte[] buffer;
			byte[] copy = new byte[1500];
			int len;
			int offset;
			while(!stop)
			{
				DatagramPacket packet = ring.consumerLock();
				if(packet==null)
					continue;
				
				buffer = packet.getData();
				len = packet.getLength();
				offset = packet.getOffset();
				
				if(!packet_reader.depack(buffer, offset, len))
				{
					System.out.println("\t packet fail id="+id);
					stop_rcp = true;
					ring.consumerUnlock(packet);
					break;
					//continue;
				}
				
				for(int i=0, j=offset; i<len; i++, j++)
				{
					copy[i] = buffer[j];
				}
				//int val = processed_count.getAndIncrement();
				count++;
				if(count%10000==0)
					System.out.println("process count="+count);
				try {
					stream.write(buffer, offset, len);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
				
				ring.consumerUnlock(packet);
			}
			
			System.out.println("process count="+count);
			
			try {
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static Manifest receiveManifest(String hostname, int port) throws IOException
	{
		DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(hostname));
		
		byte[] buffer = new byte[1500];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		System.out.println("waiting manifest...");
		packet.setData(buffer, 0, buffer.length);
		
		socket.receive(packet);
		
		Manifest manifest = PacketReader.getManifest(buffer);
		if(manifest==null)
		{
			System.out.println("manifest fail");
			throw new IllegalArgumentException();
		}
		
		System.out.println("manifest received");
		
		socket.close();
		
		return manifest;
	}
	
	public static void main(String[] agrs) throws Exception
	{
		String string = "salut fonctionne";
		
		String path = "/home/benjamin/tmp.txt";
		
		File file = new File(path);
		
		FilePreallocator preallocator = new FilePreallocator();
		preallocator.preallocate(file, 10*1000000000L);
		
		RandomAccessFile rd = new RandomAccessFile(file, "rw");
		
		rd.seek(5*1000000000L);
		rd.write(2);
		
		rd.seek(0);
		rd.write(250);
		
		rd.seek(2);
		rd.write(45);
		
		rd.seek(1);
		rd.write(41);
		
		
		rd.close();
		
		/*
		File tmp = new File(path);
		BufferedRandomAccessFile rd_file = new BufferedRandomAccessFile(tmp, "rw", 2);
		
		byte[] array = string.getBytes();
		int len = array.length/2;
		
		rd_file.seek(len);
		rd_file.write(array, len, len);
		//rd_file.flush();
		
		rd_file.seek(0);
		rd_file.write(array, 0, len);
		
		rd_file.close();*/
		
		System.out.println("end");
		
	}
	
	
	public static void main123(String[] agrs) throws Exception
	{
		System.out.println("start");
		
		int nb_process_thread = 3;
		String hostname = "169.254.8.186";
		int port = 1652;
		int nb_packet = 1000;
		
		
        Runnp[] runnnp_array = new Runnp[nb_process_thread];
        Thread[] thread_array = new Thread[nb_process_thread+1];
        
        Manifest manifest = receiveManifest(hostname, port);
        PacketBufferInfo info = new PacketBufferInfo(manifest.blockSize);
        System.out.println("\ttotal length="+info.totalLength);
        
		ConcurrentBuffer ring = new ConcurrentBuffer(nb_packet, info.totalLength);
		Runn rr =  new Runn(hostname, port, ring, info);
		Thread t = new Thread(rr);
        t.setDaemon(true);//stop at the end of the JVM
        t.start(); 
        thread_array[0] = t;
        
        
        
        for(int i=0; i<runnnp_array.length; i++)
        {
        	runnnp_array[i] = new Runnp(ring, info);
        	t = new Thread(runnnp_array[i]);
            t.setDaemon(true);//stop at the end of the JVM
            t.start(); 
            thread_array[i+1] = t;
        }
        
        //Buffer buf = new CircularFifoBuffer(4);
        
        System.in.read();
        
        rr.stop();
        for(int i=0; i<runnnp_array.length; i++)
        {
        	runnnp_array[i].stop();
        }
        
        for(int i=0; i<thread_array.length; i++)
        {
        	thread_array[i].join();
        }
        
        
        Iterator<DatagramPacket> iterator = ring.iterator();
        while(iterator.hasNext())
        {
        	iterator.next();
        	processed_count.getAndIncrement();
        }
		
       // System.out.println("received count="+received_count.get());
        //System.out.println("processed count="+processed_count.get());
		System.out.println("end");
	}
	
	public static void main125(String[] agrs) throws Exception
	{
		System.out.println("sork_dir="+work_directory);
		Parms.load();
		Parms.instance().getNetworkConfig().setIp("169.254.8.186");
		Parms.instance().getNetworkConfig().setPort(1652);
		long count=0;
		
		DatagramSocket socket = new DatagramSocket(1652, Parms.instance().getNetworkConfig().getIp());
		
		File file = new File(work_directory+File.separator+"tt");
		file.createNewFile();
		FileOutputStream fos = new FileOutputStream(file, false);
		BufferedOutputStream  stream = new BufferedOutputStream(fos, 8192);
		
		byte[] buffer = new byte[1500];
		
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		/*socket.receive(packet);
		
		System.out.println("wait manifest");
		Manifest manifest = PacketReader.getManifest(packet.getData().clone());
		System.out.println("checmusm="+Checksum.LENGTH);
		System.out.println("header="+PacketHeader.BYTES);
		
		System.out.println("manifest="+manifest);
		
		socket.setSoTimeout(10000);
		PacketBufferInfo info = new PacketBufferInfo(manifest.blockSize);*/
		
		/*for(int i=0; i<10000; i++)
		{
			list_array.add(new byte[1400]);
		}*/
		byte b;
		while(true)
		{
			//buffer = list_array.poll();
			packet.setData(buffer);
			packet.setLength(buffer.length);
			
			try
			{
				socket.receive(packet);
				/*for(int i=0; i<buffer.length; i++)
				{
					b = buffer[i];
				}*/
				//byte[] buff = Arrays.copyOfRange(buffer, packet.getOffset(), packet.getLength());
				//System.out.println("offset="+packet.getOffset()+"  - length="+packet.getLength());
				/*PacketReader packetReader =  new PacketReader(info);
				if(!packetReader.depack(buffer))
				{System.out.println("total packet="+count);
					r.stop();
					throw new InvalidPacketStreamException();
				}
				
				Container container = new Container(packetReader.getHeader(), packetReader.getBuffer());*/
				//stream.write(packet.getData().clone(), 0, packet.getLength());
				count++;
				/*if(count%1000==0)
					System.out.println("packet "+count+" received"+ "  | size="+packet.getLength());*/
			}catch(IOException e)
			{
				e.printStackTrace();
				break;
			}
		}
		stream.flush();
		
		socket.close();
		stream.close();
		
		int size=PacketBufferInfo.getSize(manifest.blockSize);
		System.out.println("packet size="+size);
		//System.out.println("is equals="+(236000*size==file.length()));
		System.out.println("total packet="+count);
		System.out.println("file size="+file.length());
		System.out.println("end");
	}
	
	public static void main11 (String[] agrs) throws Exception
	{
		System.out.println("sork_dir="+work_directory);
		Parms.load();
		Parms.instance().getNetworkConfig().setIp("169.254.8.186");
		Parms.instance().getNetworkConfig().setPort(1652);
		
		Parms.instance().sender().setSendRate(10000);
		Parms.instance().sender().setMMUPacketLength(1500);
		Parms.instance().receiver().setWorkspace(work_directory);
		Parms.instance().receiver().setOutPath(work_directory);
		
		Parms.save();
		
		int nb_packet_block = nb_packet_to_hold;
		int buffer_file_size = 8192;
		long timeout = 10000000000L;//10s
		
		ReceiverInterface ritf = new ReceiverInterface(new NetworkAddress[] {Parms.instance().getNetworkConfig()}, nb_packet_to_hold, nb_packet_block,
				buffer_file_size, timeout);
		ritf.start();
	}
	
	public static void main111(String[] args) throws Exception
	{
		System.out.println("sork_dir="+work_directory);
		Parms.load();
		Parms.instance().getNetworkConfig().setIp("169.254.9.13");
		Parms.instance().getNetworkConfig().setPort(1652);
		
		Parms.instance().sender().setSendRate(100000);
		Parms.instance().sender().setMMUPacketLength(1500);
		Parms.instance().receiver().setWorkspace(work_directory);
		Parms.instance().receiver().setOutPath(work_directory);
		
		Parms.save();
		
		
		Sender sender = new Sender();
		
		sender.send(new File("/home/benjamin/Téléchargements/Ex.Machina.2015.MULTi.TRUEFRENCH.1080p.BluRay.x264.mkv"));
		
		sender.close();
	}
	
	public static void main4785(String[] args) throws Exception
	{
		Parms.load();
		int nb_packet_block = nb_packet_to_hold;
		int buffer_file_size = 8192;
		long timeout = 10000000000L;//10s
		
		InputStream input_stream = new ByteArrayInputStream(init_data);
		FileMetadata m = new FileMetadata("test.txt");
		
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.File, m);
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		
		byte[] data;
		byte[] b_manifest;
		b_manifest = sender.getPacket();//manifest
		manifest = PacketReader.getManifest(b_manifest);
		
		//ArrayList<byte []> list = new ArrayList<byte []>();
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		
		System.out.println("packet send");
		
		System.out.println("manifest="+manifest);
		
		//Collections.shuffle(list);
		list = (ArrayList<byte[]>)blockShuffle(list, nb_packet_block);
		
		
		
		int number_threads = 5;
		
		int port = 2;
		Parms.instance().getNetworkConfig().setPort(port);
		Parms.instance().receiver().setOutPath(work_directory);
		Parms.instance().receiver().setWorkspace(work_directory);
		
		int h = list.size()/number_threads;
		int total_size = list.size();
		ArrayList<ConcurrentLinkedQueue<byte[]>> sub_lists = new ArrayList<ConcurrentLinkedQueue<byte[]>>();
		
		System.out.println("list lenght="+list.size());
		System.out.println("list shuffled");
		
		/*for(int i=0; i<number_threads; i++)
		{
			ConcurrentLinkedQueue<byte[]> tmp = new ConcurrentLinkedQueue<byte[]>();
			if(i==0)
			{
				tmp.add(b_manifest);
			}
			
			for(int j=i*h; j<Math.min((i+1)*h, total_size); j++)
			{
				tmp.add(list.get(j));
			}
			sub_lists.add(tmp);
			System.out.println("size="+sub_lists.get(i).size());
		}
		for(int j=number_threads*h; j<Math.min((number_threads+1)*h, total_size); j++)
		{
			sub_lists.get(number_threads-1).add(list.get(j));
		}
		list.clear();
		
		
		long count = 0;
		
		for(int i=0; i<number_threads; i++)
		{
			count+=sub_lists.get(i).size();
		}*/
		
		//sub_lists.get(0).add(list.get(0));
		//sub_lists.get(0).add(list.get(h));
		//System.out.println("total size="+total_size+"  | currnt size="+(count-1));
		
		for(int i=0; i<number_threads; i++)
		{
			ConcurrentLinkedQueue<byte[]> tmp = new ConcurrentLinkedQueue<byte[]>();
			sub_lists.add(tmp);
		}
		
		sub_lists.get(0).add(b_manifest);
		
		for(int i=0; i<total_size; i++)
		{
			for(int k=0; k<number_threads; k++)
			{
				if(i%number_threads==k)
				{
					sub_lists.get(k).add(list.get(i));
				}
			}
		}
		list.clear();
	

		System.out.println("start");
		
		ReceiverInterface ritf = new ReceiverInterface(new NetworkAddress[] {Parms.instance().getNetworkConfig()}, nb_packet_to_hold, nb_packet_block, buffer_file_size,
				timeout);
		ritf.start();
		
		
		/*PacketManager manager = new InFilePacketManager(work_directory, manifest, nb_packet_block, nb_packet_block, buffer_file_size);
		PacketReader packetReader = new PacketReader(block_size);
		Instant start = Instant.now();
		BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(work_directory+File.separator+"ttt")));
		for(byte[] buffer : list)
		{
			if(!packetReader.depack(buffer))
			{
				continue;
			}
			//stream.write(packetReader.getBuffer(), packetReader.getOffsetData(), packetReader.getHeader().getLength());
			manager.add(packetReader.getHeader(), packetReader.getBuffer(), packetReader.getOffsetData());
		}
		//stream.flush();
		manager.flush();
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		System.out.println("time="+timeElapsed);
		
		manager.close();*/
	}
	
	public static void main211(String[] agrs) throws Exception
	{
		Parms.load();
		
		for(int i=0; i<init_data.length; i++)
		{
			init_data[i] = (byte)i;
		}
		
		InputStream input_stream = new ByteArrayInputStream(init_data);
		FileMetadata m = new FileMetadata("test.txt");
		
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.File, m);
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		
		byte[] data;
		byte[] b_manifest;
		b_manifest = sender.getPacket();//manifest
		manifest = PacketReader.getManifest(b_manifest);
		
		//ArrayList<byte []> list = new ArrayList<byte []>();
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		
		System.out.println("packet send");
		
		System.out.println("manifest="+manifest);
		
		//Collections.shuffle(list);
		//list = (ArrayList<byte[]>)blockShuffle(list, 300);
		
		InFilePacketManager manager = new InFilePacketManager(work_directory, manifest);
		PacketReader packet = new PacketReader(block_size);
		
		HashSet<Integer> set = new HashSet<Integer>();
		set.add(10);//packet data 9
		set.add(11);//packet data 10
		set.add(14);//packet data 11
		set.add(16);//packet data 12
		set.add(17);
		set.add(18);
		set.add(19);
		set.add(20);
		set.add(21);
		set.add(22);
		//index start at 15
		int index_data=-1;
		
		//ordre : 15 - 16 - 17 - 18 - 9 - 10 - 11 - 12 - 44 - 13 - 14
		if(false)
		{
			for(int i=0; i<list.size(); i++)
			{
				if(set.contains(i))
					continue;
				if(!packet.depack(list.get(i)))
				{
					System.out.println("corruped !");
					return ;
				}
				if(packet.getHeader().getType().isData() && index_data==-1)
				{
					index_data=i;
				}
				manager.add(packet.getHeader(), packet.getBuffer(), packet.getOffsetData());
			}
		}else
		{
			/*for(Integer i : set)
			{
				if(!packet.depack(list.get(i)))
				{
					System.out.println("corruped !");
					return ;
				}
				manager.add(packet.getHeader(), packet.getBuffer(), packet.getOffsetData());
			}*/
			
			manager.update();
		}
		//manager.test();
		//manager.update();
		manager.flush();
		
		manager.close();
		
		System.out.println("start data index at ="+index_data);
		System.out.println("manifest="+manifest);
		byte[] new_init_data = Tools.getBytesFromFile(work_directory+File.separator+"merge_2");
		System.out.println("euqlas="+Arrays.equals(init_data, new_init_data));
		System.out.println("get writtent count="+manager.isReassemblyFinished());
		System.out.println("complete="+manager.isComplete());
	}
	
	public static void main14(String[] args) throws Exception
	{
		Parms.load();
		int nb_packet_to_hold = 10;
		int nb_packet_block = nb_packet_to_hold;
		int buffer_file_size = 8192;
		long timeout = 10000000000L;//10s
		
		InputStream input_stream = new ByteArrayInputStream(init_data);
		FileMetadata m = new FileMetadata("test.txt");
		
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.File, m);
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		
		byte[] data;
		byte[] b_manifest;
		b_manifest = sender.getPacket();//manifest
		manifest = PacketReader.getManifest(b_manifest);
		
		//ArrayList<byte []> list = new ArrayList<byte []>();
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		
		System.out.println("packet send");
		
		System.out.println("manifest="+manifest);
		
		//Collections.shuffle(list);
		list = (ArrayList<byte[]>)blockShuffle(list, 255);
		
		
		
		int number_threads = 2;
		int h = list.size()/number_threads;
		int total_size = list.size();
		ArrayList<ConcurrentLinkedQueue<byte[]>> sub_lists = new ArrayList<ConcurrentLinkedQueue<byte[]>>();
		
		System.out.println("list lenght="+list.size());
		System.out.println("list shuffled");
		
		for(int i=0; i<number_threads; i++)
		{
			ConcurrentLinkedQueue<byte[]> tmp = new ConcurrentLinkedQueue<byte[]>();
			if(i==0)
			{
				tmp.add(b_manifest);
			}
			
			for(int j=i*h; j<Math.min((i+1)*h, total_size); j++)
			{
				tmp.add(list.get(j));
			}
			sub_lists.add(tmp);
			System.out.println("size="+sub_lists.get(i).size());
		}
		list.clear();
	

		System.out.println("start");
		
		ReceiverInterface ritf = new ReceiverInterface(new NetworkAddress[] {Parms.instance().getNetworkConfig()}, nb_packet_to_hold, nb_packet_block, 
				buffer_file_size, timeout);
		ritf.start();
	}
	
	/*
	public static void main55(String[] args) throws Exception
	{
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable content = clipboard.getContents(null);
		byte[] data = null;
		DataFlavor flavor = null;
		if(content.isDataFlavorSupported(DataFlavor.imageFlavor))
		{
			System.out.println("clipboard image");
			
			flavor = DataFlavor.imageFlavor;
			BufferedImage bufferedImage = (BufferedImage)content.getTransferData(DataFlavor.imageFlavor);
			
			File img_file = File.createTempFile("temp", null);
			img_file.deleteOnExit();
			ImageIO.write(bufferedImage, "png", img_file);
			
			data = Tools.getBytesFromFile(img_file);
			
		}else if(content.isDataFlavorSupported(DataFlavor.stringFlavor))
		{
			System.out.println("clipboard string");
			flavor = DataFlavor.stringFlavor;
			String txt = (String) content.getTransferData(DataFlavor.stringFlavor);
			data = txt.getBytes("UTF-8");
		}else if(content.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
		{
			System.out.println("list files");
		}
		//javaJVMLocalObjectMimeType javaRemoteObjectMimeType javaSerializedObjectMimeType plainTextFlavor selectionHtmlFlavor fragmentHtmlFlavor javaFileListFlavor allHtmlFlavor
		System.out.println("wait :");
		System.in.read();
		
		
		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transferable = null;
		
		if(flavor.equals(DataFlavor.imageFlavor))
		{
			System.out.println("\t"+"Image");
			
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			BufferedImage bufferedImage = (BufferedImage)ImageIO.read(bais);
			
			transferable = new ImageTransferable(bufferedImage);
			
		}else if(flavor.equals(DataFlavor.stringFlavor))
		{
			System.out.println("\t"+"String");
			String string = new String(data);
			
			transferable = new StringSelection(string);
		}
		
		
		clipboard.setContents(transferable, null);
		
		System.out.println("wait :");
		System.in.read();
			
	}
	
	public static void main7(String[] args) throws Exception
	{
		
		String msg = (String) Toolkit.getDefaultToolkit()
                .getSystemClipboard().getData(DataFlavor.stringFlavor);
		//String msg = "salut copie coller";
		byte[] b_data = msg.getBytes("UTF-8");
		
		String txt = new String(b_data);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection stringSelection = new StringSelection(txt);
		clipboard.setContents(stringSelection, null);
		Image image = null;
		BufferedImage bufferedImage = Tools.getBufferedImage(image);
		byte[] imageBytes = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
		System.in.read();
	}
	
	
	public static void main000(String[] args) throws Exception
	{
		Environment.setBuildDir(work_directory);
		
		
		InputStream input_stream = new ByteArrayInputStream(init_data);
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.File, m);
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		
		byte[] data;
		byte[] b_manifest;
		b_manifest = sender.getPacket();//manifest
		manifest = PacketReader.getManifest(b_manifest);
		
		//ArrayList<byte []> list = new ArrayList<byte []>();
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		
		System.out.println("packet send");
		
		System.out.println("manifest="+manifest);
		
		//Collections.shuffle(list);
		list = (ArrayList<byte[]>)blockShuffle(list, 255);
		
		
		
		int number_threads = 2;
		int h = list.size()/number_threads;
		int total_size = list.size();
		ArrayList<ConcurrentLinkedQueue<byte[]>> sub_lists = new ArrayList<ConcurrentLinkedQueue<byte[]>>();
		
		System.out.println("list lenght="+list.size());
		System.out.println("list shuffled");
		
		for(int i=0; i<number_threads; i++)
		{
			ConcurrentLinkedQueue<byte[]> tmp = new ConcurrentLinkedQueue<byte[]>();
			if(i==0)
			{
				tmp.add(b_manifest);
			}
			for(int j=i*h; j<Math.min((i+1)*h, total_size); j++)
			{
				tmp.add(list.get(j));
			}
			sub_lists.add(tmp);
			System.out.println("size="+sub_lists.get(i).size());
		}
		list.clear();
		
		int[] ports = new int[number_threads];
		for(int i=0; i<ports.length; i++)
		{
			ports[i] = i;
		}
		NetworkAddress address = new NetworkAddress(InetAddress.getByName("127.0.0.1"), ports);
		Receiver receiver =  new Receiver(address, sub_lists);

		System.out.println("start");
		try
		{
			receiver.receive();
			
		}catch(TimeoutException e)
		{
			e.printStackTrace();
		}catch(InvalidPacketStreamException e)
		{
			e.printStackTrace();
		}catch(EOFException e)
		{
			System.out.println("oki");
		}catch(IOException e)
		{
			e.printStackTrace();
		}
		
		receiver.close();
	}
	
	public static void main00(String[] args) throws IOException
	{
		 Thread thread = new Thread()
		 {
		    public void run()
		    {
		    	try {
		    		ren();
		    	}catch(Exception e)
		    	{
		    		System.out.println("*************************Exception ! = ");
		    		e.printStackTrace();
		    	}
		    }
		};
		
		thread.setDaemon(true);
		thread.start();
			  
		while(manifest==null)
		{
			
		}
		stream = new ConcurrentPacketList(work_directory, manifest, info);
		System.out.println("created");
		
		try
		{
			stream.startAsynchronousReassembly();
		}catch(EOFException e)
		{
			System.out.println("END OF STREAM");
		}catch(TimeoutException e)
		{
			System.out.println("TIMEOUT");
		}
		stream.flush();
		
		PacketContent content = stream.getContent(PacketType.TYPE_DATA);
		System.out.println("--- DATA TYPE ---");
		if(content==null)
		{
			System.out.println("content null");
		}
		else if(content.inMemory)
		{
			System.out.println("memory content");
			byte[] tmp = content.getInMemoryContent();
			System.out.println("equals="+Arrays.equals(init_data, tmp));
		}else
		{
			System.out.println("file content");
			String filename = content.getOnDiskContent();
			byte[] tmp = Tools.getBytesFromFile(filename);
			System.out.println("filanme="+filename);
			System.out.println("equals="+Arrays.equals(init_data, tmp));
		}
		
		
		content = stream.getContent(PacketType.TYPE_METADATA);
		System.out.println("--- METADATA TYPE ---");
		if(content==null)
		{
			System.out.println("content null");
		}
		else if(content.inMemory)
		{
			System.out.println("memory content");
		}else
		{
			System.out.println("file content");
			String filename = content.getOnDiskContent();
			System.out.println("filanme="+filename);
			Metadata metadata = Metadata.getMetadata(filename, manifest.type);
			System.out.println("Metadata="+((FileMetadata)metadata).absolutePath());
		}
			
		stream.close();
		System.out.println("----------END----------");
	}
	
	public static void ren() throws Exception
	{
		for(int i=0; i<init_data.length; i++)
		{
			init_data[i] = (byte)i;
		}
		
		InputStream input_stream = new ByteArrayInputStream(init_data);
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.File, m);
		
		byte[] data;
		data = sender.getPacket();//manifest
		manifest = PacketReader.getManifest(data);
		
		
		//ArrayList<byte []> list = new ArrayList<byte []>();
		ArrayList<PacketReader> list = new ArrayList<PacketReader>();
		data = sender.getPacket();
		while(data!=null)
		{
			PacketReader packet = new PacketReader(info);
			if(!packet.depack(data))
			{
				System.out.println("CORRUPTION !");
				return ;
			}
			list.add(packet);
			data = sender.getPacket();
		}
		
		System.out.println("packet send");
		
		System.out.println("manifest="+manifest);
		
		//Collections.shuffle(list);
		list = (ArrayList<PacketReader>)blockShuffle(list, 255);
		
		System.out.println("list lenght="+list.size());
		System.out.println("list shuffled");

		
		System.out.println("start");
		//43 ->65
		//41-55 | 55-64
		PacketReader packet;
		Thread.sleep(2000);
		System.out.println("adding");
		//Instant start = Instant.now();
		for(int i=0; i<list.size(); i++)
		{
			//System.out.println("i="+i);
			packet = list.get(i);
			//if(i==0 || i==2 || i==4|| i==5)
				//continue;
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet.getBuffer()))
				System.out.println("NOT ADDED");
			
		}
		
		//Instant finish = Instant.now();
		//long timeElapsed = Duration.between(start, finish).toMillis();
		
	}
	
	
	public static void main0(String[] args) throws Exception
	{
		//initialization
			String work_directory = "/home/benjamin/eclipse-workspace/UDP/test
			
			
			int nb_packet = 10;
			int block_size = 1200;
			PacketBufferInfo info = new PacketBufferInfo(block_size);
			
			
			byte[] init_data = new byte[nb_packet*block_size+3];
			
			for(int i=0; i<init_data.length; i++)
			{
				init_data[i] = (byte)i;
			}
			
			InputStream input_stream = new ByteArrayInputStream(init_data);
			FileMetadata m = new FileMetadata("test oui");
			PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.Clipboard, null);
			
			byte[] data;
			data = sender.getPacket();//manifest
			Manifest manifest = PacketReader.getManifest(data);
			
			
			//ArrayList<byte []> list = new ArrayList<byte []>();
			ArrayList<PacketReader> list = new ArrayList<PacketReader>();
			data = sender.getPacket();
			while(data!=null)
			{
				PacketReader packet = new PacketReader(info);
				if(!packet.depack(data))
				{
					System.out.println("CORRUPTION !");
					return ;
				}
				list.add(packet);
				data = sender.getPacket();
			}
			
			System.out.println("packet send");
			
			System.out.println("manifest="+manifest);
			
			//Collections.shuffle(list);
			list = (ArrayList<PacketReader>)blockShuffle(list, 10);
			
			System.out.println("list lenght="+list.size());
			System.out.println("list shuffled");

			
			ConcurrentPacketList stream = new ConcurrentPacketList(work_directory, manifest, info);
			System.out.println("start");
			//43 ->65
			//41-55 | 55-64
			PacketReader packet;
			
			Instant start = Instant.now();
			for(int i=0; i<list.size(); i++)
			{
				
				packet = list.get(i);
				/*if(i==0 || i==2 || i==4|| i==5)
					//continue;
				//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
					//System.out.println("NOT ADDED");
				
				if(!stream.add(packet.getBuffer()))
					System.out.println("NOT ADDED");
				
			}
			
			Instant finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			
			InMemoryPacketManager manager = (InMemoryPacketManager)stream.getPacketManager();
			Iterator<Range> it = manager.getMissingPacket(PacketType.TYPE_DATA);
			while(it.hasNext())
			{
				System.out.println(it.next());
			}
			
			
			System.out.println("time="+timeElapsed);
			
			System.out.println("res="+Arrays.equals(init_data, manager.getBuffer()));
			
			stream.close();
	}
	
	public static void main2(String[] args) throws Exception 
	{
		//initialization
		String work_directory = "/home/benjamin/eclipse-workspace/UDP/test";
		
		
		int nb_packet = 100000;
		int block_size = 1200;
		PacketBufferInfo info = new PacketBufferInfo(block_size);
		
		
		byte[] init_data = new byte[nb_packet*block_size+3];
		
		for(int i=0; i<init_data.length; i++)
		{
			init_data[i] = (byte)i;
		}
		
		InputStream input_stream = new ByteArrayInputStream(init_data);
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.Clipboard, null);
		
		byte[] data;
		data = sender.getPacket();//manifest
		Manifest manifest = PacketReader.getManifest(data);
		
		
		//ArrayList<byte []> list = new ArrayList<byte []>();
		ArrayList<PacketReader> list = new ArrayList<PacketReader>();
		data = sender.getPacket();
		while(data!=null)
		{
			PacketReader packet = new PacketReader(info);
			if(!packet.depack(data))
			{
				System.out.println("CORRUPTION !");
				return ;
			}
			list.add(packet);
			data = sender.getPacket();
		}
		
		System.out.println("packet send");
		
		System.out.println("manifest="+manifest);
		
		//Collections.shuffle(list);
		list = (ArrayList<PacketReader>)blockShuffle(list, 255);
		
		System.out.println("list lenght="+list.size());
		System.out.println("list shuffled");

		for(int i=0; i<255*2; i++)
		{
			System.out.println(list.get(i).getHeader().getIndex());
			if(i%255==0)
				System.out.println("-----");
		}
		
		ConcurrentPacketList stream = new ConcurrentPacketList(work_directory, manifest, info);
		System.out.println("start");
		//43 ->65
		//41-55 | 55-64
		PacketReader packet;
		
		Instant start = Instant.now();
		for(int i=0; i<list.size(); i++)
		{
			
			packet = list.get(i);
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet.getBuffer()))
				System.out.println("NOT ADDED");
			
		}
		
		
		/*for(int i=255; i<list.size(); i++)
		{
			
			packet = list.get(i);
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet.getBuffer()))
				System.out.println("NOT ADDED");
			
		}
		
		
		for(int i=100; i<105; i++)
		{
			packet = list.get(i);
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet.getBuffer()))
				System.out.println("NOT ADDED");
			
		}*/
		
		/*for(int i=0; i<100; i++)
		{
			packet = list.get(i);
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet.getBuffer()))
				System.out.println("NOT ADDED");
		}
		
		for(int i=105; i<255; i++)
		{
			packet = list.get(i);
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet.getBuffer()))
				System.out.println("NOT ADDED");
		}*/
		
		/*int[] pos_list = new int[] {0,2,3,4,5, 1, 11, 12, 13,14,15,16,17, 18, 19};
		for(int i : pos_list)
		{
			packet = list.get(i);
			if(!stream.add(packet.getBuffer()))
				System.out.println("NOT ADDED");
		}
		
		stream.flush();
		//stream.test();
		
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		
		PacketManager manager = stream.getPacketManager();
		Iterator<Range> it = manager.getMissingPacket(PacketType.TYPE_DATA);
		while(it.hasNext())
		{
			System.out.println(it.next());
		}
		
		stream.close();
		System.out.println("time="+timeElapsed);

		
		/*byte[] file_buffer = Files.readAllBytes(Paths.get(work_directory+File.separator+"merge_2"));
		System.out.println("equals="+Arrays.equals(file_buffer, init_data));*/
		/*
		OutputPacketList stream = new OutputPacketList(work_directory, manifest, info, 255);
		byte[] packet;
		Instant start = Instant.now();
		for(int i=0; i<list.size(); i++)
		{
			
			packet = list.get(i);
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet))
				System.out.println("NOT ADDED");
			
		}
		stream.flush();
		//stream.test();
		
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		
		//System.out.println("equals="+Arrays.equals(init_data, stream.test2()));
		stream.close();
		System.out.println("time="+timeElapsed);
		*/
		
		/*
		int block_size = 1200;
		ArrayList<byte []> list = new ArrayList<byte []>();
		PacketBufferInfo info = new PacketBufferInfo(block_size);

		byte[] init_data = new byte[100000*block_size];
		
		
		InputStream input_stream = new ByteArrayInputStream(init_data);
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.File, m);
		
		byte[] data;
		data = sender.getPacket();//manifest
		Manifest manifest = PacketReader.getManifest(data);
		System.out.println("manifest="+manifest);
		
		data = sender.getPacket();
		while(data!=null)
		{
			/*PacketReader packet = new PacketReader(info);
			if(!packet.depack(data))
			{
				System.out.println("CORRUPTION !");
				return ;
			}*//*
			list.add(data);
			data = sender.getPacket();
		}
		
		System.out.println("packet send");
		Collections.shuffle(list);
		System.out.println("list shuffled");
		
		OutputPacketList stream = new OutputPacketList(work_directory, manifest, info);
		System.out.println("start");
		//43 ->65
		//41-55 | 55-64
		byte[] packet;
		
		DataOutputStream streama = new DataOutputStream(new FileOutputStream(new File(work_directory+"/test")));
		Instant start = Instant.now();
		for(int i=0; i<list.size(); i++)
		{
			
			packet = list.get(i);
			//if(!stream.add(packet.getHeader(), packet.getBuffer(), true))
				//System.out.println("NOT ADDED");
			
			if(!stream.add(packet))
				System.out.println("NOT ADDED");
			
		}
		stream.flush();
		//stream.test();
		
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		
		System.out.println("equals="+Arrays.equals(init_data, stream.test2()));
		stream.close();
		System.out.println("time="+timeElapsed);
		
		
		
		
		/*
		byte[] buffer = new byte[1200];
		
		
		Instant start = Instant.now();
		
		for(long i=0; i<10; i++)
		{
			buffer[0] = 2;
		}
		
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		
		System.out.println("time="+timeElapsed);*/
		

		/*ArrayList<byte[]> list = new ArrayList<byte[]>();
		int block_size = 3;
		PacketBufferInfo info = new PacketBufferInfo(block_size);
		String msg= "salut toi comment est ce que tu va ? moi c'est ok franchement mais ";//je ne sais pas si toi ca va aller avce tout ce que tu m'a dit et tout ce qui cse passe ici !!! je ne sis plus quoi faire. Il me faut un long texte vraiment long tres long en fait!";
		
		byte[] init_data = msg.getBytes("UTF-8");
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, init_data.length, block_size, DataType.File, m);
		
		byte[] data;
		data = sender.getPacket();//manifest
		Manifest manifest = PacketReader.getManifest(data);
		System.out.println("manifest="+manifest);
		
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		//Collections.shuffle(list);
		
		ArrayList<PacketReader> plist = new ArrayList<PacketReader>();
		PacketReader p;
		for(byte [] buffer : list)
		{
			p = new PacketReader(block_size);
			if(!p.depack(buffer))
			{
				System.out.println("ERROR CORRUPTION");
				break;
			}
			
			plist.add(p);
		}
		
		
		byte[] ma = manifest.writeExternal();
		FileOutputStream output = new FileOutputStream(Environment.getManifestFilename(work_directory));
		output.write(ma);
		output.close();
		
		OutputStreamPacket stream = new OutputStreamPacket(work_directory, manifest, info);
		
		for(int i=0; i<plist.size()-2; i++)
		{
			PacketReader ptm = plist.get(i);
			System.out.println("(index="+ptm.getHeader().getIndex()+", type="+ptm.getHeader().getType()+")");
			if((ptm.getHeader().getIndex()==40 || ptm.getHeader().getIndex()==25) && ptm.getHeader().getType().isMetadata())
			{
				System.out.println("\tpassed");
				continue;
			}
			if((ptm.getHeader().getIndex()==6 || ptm.getHeader().getIndex()==25) && ptm.getHeader().getType().isData())
			{
				System.out.println("\tpassed");
				continue;
			}
			stream.write(ptm.getHeader(), ptm.getBuffer(), true);
		}
		stream.flush();*/
		/*PacketManager manager = stream.manager();
		manager.waitingShow();
		manager.missingShow();
		stream.update(true);
		System.out.println("**update**");
		manager.waitingShow();
		manager.missingShow();*/
		//stream.completeMissing();
		
		/*System.out.println("******MISSIING*******");
		Range r;
		Iterator<Range> it = stream.missingIterator();
		while(it.hasNext())
		{
			r = it.next();
			System.out.println("type="+r.type+", start="+r.start+", end="+r.end);
		}
		System.out.println("**********************");
		
		stream.close();
		System.out.println("end");
		
		FileInputStream in = new FileInputStream(Environment.getMergeDataFilename(work_directory));
		byte[] end_data = new byte[init_data.length];
		in.read(end_data);
		in.close();
		String msg2 = new String(end_data);
		System.out.println("msg="+msg2);
		*/
	
		
		
		
		/*ArrayList<byte[]> list = new ArrayList<byte[]>();
		int block_size = 1200;
		
		String msg= "salut toi comment est ce que tu va ? moi c'est ok franchement mais ";//je ne sais pas si toi ca va aller avce tout ce que tu m'a dit et tout ce qui cse passe ici !!! je ne sis plus quoi faire. Il me faut un long texte vraiment long tres long en fait!";
		
		byte[] data = msg.getBytes("UTF-8");
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, data.length, block_size, DataType.File, m);
		
		data = sender.getPacket();//manifest
		Manifest manifest = PacketReader.getManifest(data);
		System.out.println("manifest="+manifest);
		System.out.println("total size="+(Checksum.LENGTH+PacketHeader.BYTES));
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		Collections.shuffle(list);
		
		
		PacketManager bd = new PacketManager();
		bd.open(work_directory);
		
		PacketReader p = new PacketReader(block_size);
		Instant start = Instant.now();
		
		for(byte[] buffer : list)
		{
			if(!p.depack(buffer))
			{
				System.out.println("ERROR CORRUPTION");
				break;
			}
			
			bd.addWaitingPacket(p.getHeader(), buffer, p.getOffsetData());
		}
		bd.flush();
		
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		
		Iterator<PacketData> it = bd.waitingIteartor();
		PacketData pData;
		String msg2 = "";
		while(it.hasNext())
		{
			pData = it.next();
			System.out.println("(index="+pData.header.getIndex()+", type="+pData.header.getType()+", length="+pData.header.getLength()+")");
			if(pData.header.getType().isData())
				msg2+= new String(pData.data, 0, pData.data.length);
			
			//bd.removeWaitingPacket(pData.header.getType(), pData.header.getIndex());
			//bd.sync();
		}
		
		System.out.println("msg="+msg2);
		System.out.println("length="+msg2.length());
		System.out.println("duration="+timeElapsed);
				System.out.println("count="+bd.getNumberMissingPackets());
		bd.addMissingPacket(PacketType.Data(), 110, 256);
		System.out.println("count="+bd.getNumberMissingPackets());
		bd.close();*/
		
		
		
		/*PacketManager bd = new PacketManager();
		bd.open(work_directory);

		PacketType type = PacketType.Metadata();
		bd.addMissingPacket(type, 16);
		
		bd.addMissingPacket(type, 8, 10);
		
		type = PacketType.Data();
		bd.addMissingPacket(type, 0, 5);
		
		bd.addMissingPacket(type, 7);
		
		bd.addMissingPacket(type, 12);
		bd.addMissingPacket(type, 13);
		bd.addMissingPacket(type, 14);
		
		bd.addMissingPacket(type, 17, 20);
		bd.flush();
		
		Iterator<Range> it = bd.missingIterator();
		Range r;
		while(it.hasNext())
		{
			r = it.next();
			System.out.println(r);
		}
		
		System.out.println("-------------");
		bd.removeMissingPacket(type, 13);
		bd.flush();
		
		it = bd.missingIterator();
		while(it.hasNext())
		{
			r = it.next();
			System.out.println(r);
		}
		
		//System.out.println("---------------");
		//bd.test();
		
		bd.close();*/
		
		
		/*
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		int block_size = 3;
		
		String msg= "salut toi !";
		byte[] data = msg.getBytes("UTF-8");
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, data.length, block_size, DataType.File, m);
		
		data = sender.getPacket();//manifest
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		
		Collections.shuffle(list);
		
		
		PacketManager bd = new PacketManager();
		bd.open("");
		
		PacketReader p = new PacketReader(block_size);
		for(byte[] buffer : list)
		{
			if(!p.depack(buffer))
			{
				System.out.println("ERROR CORRUPTION");
				break;
			}
			
			bd.addWaitingPacket(p.getHeader(), buffer, p.getOffsetData());
		}
		
		PacketData pData = bd.getNextWaitingPacket();
		String msg2 = "";
		while(pData!=null)
		{
			System.out.println("(index="+pData.header.getIndex()+", type="+pData.header.getType()+", length="+pData.header.getLength()+")");
			if(pData.header.getType().isData())
				msg2+= new String(pData.data, 0, pData.data.length);
			
			pData = bd.getNextWaitingPacket();
		}
		
		System.out.println("msg="+msg2);
		bd.close();
		*/
		
		/*
		ConcurrentLinkedQueue<byte[]> list_concur = new ConcurrentLinkedQueue<byte[]>();
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		int block_size = 3;
		
		String msg= "salut toi !";
		byte[] data = msg.getBytes("UTF-8");
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, data.length, block_size, DataType.File, m);
		
		data = sender.getPacket();//manifest
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		
		Collections.shuffle(list);
		
		for(byte[] buffer : list)
		{
			list_concur.add(buffer);
		}
		
		PacketBufferInfo info = new PacketBufferInfo(block_size);
		PacketHub hub = new PacketHub(2, info, list_concur);
		hub.run();
		*/
		
		
		/*
		int block_size = 3;
		PacketBufferInfo info = new PacketBufferInfo(block_size);
		PacketsList stream = new PacketsList(info);
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		
		
		
		String msg= "salut toi !";
		byte[] data = msg.getBytes("UTF-8");
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, data.length, block_size, DataType.File, m);
		
		data = sender.getPacket();//manifest
		data = sender.getPacket();
		while(data!=null)
		{
			list.add(data);
			data = sender.getPacket();
		}
		
		
		
		System.out.println("previous order="+order(list, block_size));
		
		Collections.shuffle(list);
		
		System.out.println("new order="+order(list, block_size));
		
		
		int i = 0;
		for(byte[] buffer : list)
		{
			if(!stream.add(buffer))
			{
				System.out.println("STREAM corruption packet number : "+i);
				return ;
			}
			i++;
		}
		
		
		String msg2 = "";
		for(Container container : stream)
		{
			
			System.out.println("\ttype="+container.getHeader().getType()+"  |  index="+container.getHeader().getIndex()+"  | lenght="+container.getHeader().getLength());
			
			if(container.getHeader().getType().isData())
				msg2+= new String(container.getBuffer(), info.offsetData, container.getHeader().getLength());
		}
		
		System.out.println("msg="+msg2);
		*/
		
		
		/*
		String msg= "salut toi !";
		byte[] data = msg.getBytes("UTF-8");
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
		int block_size = 3;

		FileMetadata m = new FileMetadata("test oui");
		PacketSender sender  = new PacketSender(input_stream, data.length, block_size, DataType.Clipboard, null);
		
		data = sender.getPacket();//manifest
		data = sender.getPacket();
		
		String msg2 = "";
		PacketReader p = new PacketReader(block_size);
		int i = 0;
		while(data!=null)
		{
			System.out.println("i="+i);
			if(!p.depack(data))
			{
				System.out.println("Corrupted packet "+i);
				return;
			}
			
			System.out.println("len="+p.getLenData());
			System.out.println("header index="+p.getHeader().getIndex());
			System.out.println("header type="+p.getHeader().getType());
			msg2+= new String(p.getBuffer(), p.getOffsetData(), p.getLenData());
			
			i++;
			data = sender.getPacket();
		}
		
		System.out.println("msg="+msg2);
		
		/*int bloc_size = 5;
		int offset_buffer = 7;
		int size = offset_buffer + Checksum.LENGTH + PacketHeader.BYTES + bloc_size;
		byte[] buffer = new byte[size];
		Packet p = new Packet(buffer, offset_buffer, bloc_size);
		byte[] checksum = new byte[Checksum.LENGTH];
		for(int i=0; i<checksum.length; i++)
		{
			checksum[i] = (byte)i;
		}
		
		PacketHeader pp = new PacketHeader(12, 5, PacketType.Data());
		byte[] header = pp.writeExternal();
		
		byte[] data = new byte[bloc_size*2];
		for(int i=0; i<data.length; i++)
		{
			data[i] = (byte)i;
		}
		int offset_data = bloc_size;
		
		
		p.writeHeader(header);
		p.writeCheksum(checksum);
		p.writeData(data, offset_data);
		
		byte[] tmp = p.getBuffer();
		
		
		for(int i=0; i<p.getLenChecksum(); i++)
		{
			if(tmp[i+p.getOffsetChecksum()]!=checksum[i])
			{
				System.out.println("checksum FALSE ! i="+i+" | len="+checksum.length);
				return;
			}
		}
		
		for(int i=0; i<header.length; i++)
		{
			if(tmp[i+p.getOffsetHeader()]!=header[i])
			{
				System.out.println("header FALSE ! i="+i+" | len="+header.length);
				return;
			}
		}
		
		
		for(int i=0; i<data.length-offset_data; i++)
		{
			if(tmp[i+p.getOffsetData()]!=data[i+offset_data])
			{
				System.out.println("data FALSE ! i="+i+" | len="+data.length);
				return;
			}
		}
		
		
		byte[] check = Checksum.compute(data, offset_data, data.length-offset_data);
		byte[] check2 = new byte[check.length*3];
		int offset_checksum = 2*check.length;
		
		for(int i=offset_checksum; i<offset_checksum+check.length; i++)
		{
			check2[i] = check[i-offset_checksum];
		}
		
		System.out.println(Checksum.dataMatchChecksum(check2, offset_checksum, data, offset_data, data.length-offset_data));
		
		System.out.println("all tests passed !");*/
		
		
		/*FileInputStream fis = new FileInputStream("/home/benjamin/Téléchargements/ddddd.avi");
		BufferedInputStream data_stream = new BufferedInputStream(fis);
		
		
		int block_size =  576;
		PacketBuilder packetBuilder = new PacketBuilder(data_stream, block_size, 1, PacketType.Data());
		byte[] buffer = packetBuilder.getNextPacket();
		long count=0;

		while(buffer!=null)
		{
			
			buffer = packetBuilder.getNextPacket();
			if(count%10000==0)
				System.out.println("count="+count+"   | length="+(count*576)+"  | data="+(new java.util.Date()));
			count++;
		}
		
		data_stream.close();
		System.out.println("end");*/
		
		
		//mainSendMissing();
		
	    //test("/home/benjamin/in/file_4540678890081");
		
		//mainReceive();
		//mainSend();
		/*PacketHeader p = new PacketHeader(12,2,PacketType.Data());
		System.out.println("bytes="+p.BYTES+"  |  actual bytes="+p.writeExternal().length);*/
		
		/*
		if(args[0].equals("s"))
			mainSend();
		else
			mainReceive();
		*/
		
		/*String msg="hellobsssssssdcfvd";
		InputStream input_stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
		
		PacketBuilder packetBuilder = new PacketBuilder(input_stream, 5, 1, PacketType.Data());
		byte[] output = packetBuilder.getNextPacket();
		
		String checksum_filename = "/home/benjamin/in/checksum/received_1";
		String header_filename = "/home/benjamin/in/header/received_1";
		String data_filename = "/home/benjamin/in/data/received_1";
		
		
		
		PacketBuilder.writeDeletePacket(checksum_filename, header_filename, data_filename, output, 5);
		
		PacketHeader header = PacketBuilder.checkPacket(checksum_filename, header_filename, data_filename, 5);
		System.out.println(header);
		
	}
	
	private static void mainSendMissing() throws InterruptedException, IOException, InvalidPacketStreamException
	{
		System.out.println("send...");
		
		InetAddress ip = InetAddress.getByName("192.168.0.132");
		int port = 2345;
		Sender stream = new Sender(ip, port, 500);
		String msg = "salut toi comment tu va et moi ?";
		
		ArrayList<Range> missing_meta = new ArrayList<Range>();
		missing_meta.add(new Range(6));
		missing_meta.add(new Range(12));
		
		ArrayList<Range> missing_data = null;/*new ArrayList<Range>();
		missing_data.add(new Range(2,3));
		missing_data.add(new Range(5,7));
		
		stream.sendMissing(87828912387177L, msg, new MissingPackets(missing_meta, missing_data));
		
		System.out.println("--------------- END ---------------");
	}
	
	private static void mainSend() throws InterruptedException, IOException, InvalidPacketStreamException
	{
		System.out.println("send...");
		
		//InetAddress ip = InetAddress.getByName("192.168.0.132");
		InetAddress ip = InetAddress.getByName("192.168.0.109");
		int port = 2345;
		Sender stream = new Sender(ip, port, 1);
		/*
		String msg = "salut toi comment tu va et moi ?";
		stream.send(msg, 5);
		
		//File file = new File("/home/benjamin/Téléchargements/11_IntroSockets.pdf");
		File file = new File("/home/benjamin/Téléchargements/ddddd.avi");
		stream.send(file);
		
		System.out.println("--------------- END ---------------");
	}
	
	
	private static void mainReceive() throws IOException, InvalidPacketStreamException
	{
		System.out.println("receive...");
		
		InetAddress ip = InetAddress.getByName("192.168.0.132");
		//int port = 9600;
		int port = 2345;
		Receiver stream = new Receiver(ip, port);
		String work_directory = stream.receive(true);
		System.out.println("work_directory=\n\t"+work_directory);
		
		//test(work_directory);
		
		System.out.println("--------------- END ---------------");
	}
	
	public static void test(String work_directory) throws IOException, InvalidPacketStreamException
	{
		StreamReader reader = new StreamReader(work_directory, null, true);
		try {
			
			RawDataFile data_file = reader.read();
			if(data_file.getType().equals(DataType.File))
			{
				System.out.println("TYPE file");
				
				System.out.println(((FileMetadata)data_file.getMetadata()).absolutePath());
				
			}else if(data_file.getType().equals(DataType.Clipboard))
			{
				System.out.println("TYPE clipboard");
				byte[] data = Tools.getBytesFromFile(data_file.getFilename());
				System.out.println(new String(data, "UTF-8"));
				//System.out.println(((FileMetadata)data_file.getMetadata()).absolutePath());
				
			}else
			{
				System.out.println("TYPE unknown");
			}
			
			
		} catch (MissingPacketsException e) 
		{
			MissingPackets missing_packets = e.getPackets();
			
			System.out.println("-------- METADATA MISSING PACKETS ---------------");
			if(missing_packets.getMetadataPackets()==null)
			{
				System.out.println("null");
			}else
			{
				for(Range range:missing_packets.getMetadataPackets())
				{
					System.out.println(range);
				}
			}
			
			System.out.println("-------- DATA MISSING PACKETS ---------------");
			if(missing_packets.getDataPackets()==null)
			{
				System.out.println("null");
			}else
			{
				for(Range range:missing_packets.getDataPackets())
				{
					System.out.println(range);
				}
			}
			
		}
	}*/
	
}
