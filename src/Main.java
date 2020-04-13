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
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

import EnvVariables.Environment;
import EnvVariables.Parms;
import EnvVariables.SenderParms;
import Exceptions.InvalidPacketStreamException;
import Exceptions.MissingPacketsException;
import Exceptions.ReadingPacketException;
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
import ReceiveTools.ConcurrentPacketList;
import ReceiveTools.Receiver;
import SendTools.PacketSender;
import SendTools.Sender;
import UserInterface.Menu;
import UserInterface.MenuItem;
import UserInterface.ReceiverInterface;
import UserInterface.UserMenu;
import generalTools.NetworkAddress;
import generalTools.Serialization;
import generalTools.Tools;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import javax.imageio.ImageIO;

public class Main 
{
	
	private static ConcurrentPacketList stream;
	private static volatile Manifest manifest;
	private static final int nb_packet_to_hold = 255;
	private static final int nb_packet = nb_packet_to_hold*100;
	private static final int block_size = 1200;
	private static final PacketBufferInfo info = new PacketBufferInfo(block_size);
	private static final String work_directory = System.getProperty("user.dir") + File.separator + "test";
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
	
	public static void main(String[] agrs) throws Exception
	{
		System.out.println("sork_dir="+work_directory);
		Parms.load();
		Parms.instance().getNetworkConfig().setIp("192.168.0.132");
		Parms.instance().getNetworkConfig().setPorts(new int[] {1652, 1653});
		
		Parms.instance().sender().setSendRate(10000);
		Parms.instance().sender().setMMUPacketLength(1500);
		Parms.instance().receiver().setWorkspace(work_directory);
		Parms.instance().receiver().setOutPath(work_directory);
		
		Parms.save();
		
		int nb_packet_block = nb_packet_to_hold;
		int buffer_file_size = 8192;
		long timeout = 10000000000L;//10s
		
		ReceiverInterface ritf = new ReceiverInterface(Parms.instance().getNetworkConfig(), nb_packet_to_hold, nb_packet_block,
				buffer_file_size, timeout);
		ritf.start();
	}
	
	public static void main2(String[] args) throws Exception
	{
		System.out.println("sork_dir="+work_directory);
		Parms.load();
		Parms.instance().getNetworkConfig().setIp("192.168.0.132");
		Parms.instance().getNetworkConfig().setPorts(new int[] {1652,1653});
		
		Parms.instance().sender().setSendRate(10000);
		Parms.instance().sender().setMMUPacketLength(1500);
		Parms.instance().receiver().setWorkspace(work_directory);
		Parms.instance().receiver().setOutPath(work_directory);
		
		Parms.save();
		
		
		Sender sender = new Sender();
		
		sender.send("test");
		
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
		
		int[] ports = new int[number_threads];
		for(int i=0; i<number_threads; i++)
		{
			ports[i] = i;
		}
		Parms.instance().getNetworkConfig().setPorts(ports);
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
		
		ReceiverInterface ritf = new ReceiverInterface(Parms.instance().getNetworkConfig(), nb_packet_to_hold, nb_packet_block, buffer_file_size,
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
		
		ReceiverInterface ritf = new ReceiverInterface(Parms.instance().getNetworkConfig(), nb_packet_to_hold, nb_packet_block, 
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
