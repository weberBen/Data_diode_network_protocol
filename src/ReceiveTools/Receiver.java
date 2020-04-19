package ReceiveTools;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import EnvVariables.Environment;
import EnvVariables.Parms;
import Exceptions.EndOfPacketReassemblingException;
import Exceptions.IncompleteContentException;
import Exceptions.InvalidPacketStreamException;
import Exceptions.ReassemblingException;
import Metadata.ContentActionInterface;
import Metadata.DataType;
import PacketConstructor.Manifest;
import PacketConstructor.PacketBufferInfo;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketReader;
import PacketConstructor.PacketType;
import PacketManager.InFilePacketManager;
import PacketManager.PacketContent;
import PacketManager.PacketManager;
import PacketTools.Checksum;
import PacketTools.Range;
import UserInterface.CancellationEvent;
import UserInterface.LockEvent;
import UserInterface.ManifestEvent;
import UserInterface.ReceivedEvent;
import UserInterface.ReceiverListener;
import UserInterface.ReceptionEvent;
import UserInterface.UnlockEvent;
import generalTools.NetworkAddress;
import generalTools.Tools;

public class Receiver 
{
	private static final String STRING_CODEX = "UTF-8";
	
	private HashMap<Long, PacketManager> mapIdList;
	private ConcurrentLinkedQueue<ReceiverListener> received_request;
	private SwingWorker<Void, ReceptionEvent> swingWorkder;
	private AtomicBoolean stop;
	
	public Receiver()
	{
		this.mapIdList = new HashMap<Long, PacketManager>();
		this.received_request = new ConcurrentLinkedQueue<ReceiverListener>();
		this.swingWorkder = null;
		this.stop = new AtomicBoolean(false);
	}

	public void interupt() throws IOException
	{
		interupt(true);
	}
	
	public void interupt(boolean on) throws IOException
	{
		stop.set(on);
		
		if(swingWorkder!=null)
		{
			swingWorkder.cancel(on);
		}
	}
	
	public void close() throws Exception
	{
		Exception ex = null;
		try 
		{
			interupt();
		} catch (IOException e) 
		{
			ex = e;
		}
		
		for (Map.Entry<Long, PacketManager>entry : mapIdList.entrySet()) 
		{
			Long manifest_id = entry.getKey();
			PacketManager manager = entry.getValue();
		    
			try 
			{
				manager.close();
			} catch (IOException e) 
			{
				ex = e;
			}
		}
		
		if(ex!=null)
			throw ex;
	}
	
	
	private Manifest getManifest(UdpSocketList socket_list) throws InvalidPacketStreamException, OutOfMemoryError, InterruptedException, UnsupportedEncodingException
	{
		Manifest manifest = null;
		byte[] packet;
		
		do
		{
			packet = socket_list.getPacket();
			
		}while(packet==null && !stop.get());
		if(stop.get())
			throw new CancellationException();
		
		PacketReader packetReader = new PacketReader(Manifest.BYTES);
		if(!packetReader.depack(packet))
		{
			System.out.println("manifest checksum false");
			throw new InvalidPacketStreamException();
		}
		
		if(!packetReader.getHeader().getType().isManifest())
		{
			throw new InvalidPacketStreamException();
		}
		
		manifest = Manifest.readExternal(packet, packetReader.getOffsetData());
		if(manifest==null)
		{
			throw new InvalidPacketStreamException();
		}
		
		return manifest;
	}
	
	private void reassemble(PacketManager manager, UdpSocketList socket_list, long timeout_ns) throws OutOfMemoryError, InterruptedException, IOException, EndOfPacketReassemblingException, ReassemblingException, TimeoutException
	{
		PacketReader packetReader = new PacketReader(manager.getManifest().blockSize);
		byte[] packet;
		
		long start = System.nanoTime();
		long end;
		while(!manager.isComplete() && !stop.get())
		{	
			//System.out.println("\tadd type="+container.header.getType()+", index="+container.header.getIndex());
			
			end = System.nanoTime();
			if(end-start>timeout_ns)
				throw new TimeoutException();
			
			packet = socket_list.getPacket();
			if(packet==null)
				continue;
			
			start = System.nanoTime();
			
			if(!packetReader.depack(packet))
				continue;
			
			manager.add(packetReader.getHeader(), packetReader.getBuffer(), packetReader.getOffsetData());
		}
		if(stop.get())
			throw new CancellationException();
		
		if(manager.isComplete())
		{
			List<PacketType> types = manager.update();
			if(types!=null)
			{
				System.out.println("INCOMPLETE TYPES=");
				for(PacketType type : types)
				{
					System.out.println("\t"+type);
				}
			}
			
			System.out.println("MISSING PACKET=");
			Iterator<Range> missings = manager.getMissingPacket(PacketType.TYPE_DATA);
			while(missings.hasNext())
			{
				System.out.println("\t"+missings.next());
			}
			
			
			if(!manager.isReassemblyFinished())
			{
				throw new ReassemblingException();
			}
			
			throw new EndOfPacketReassemblingException();
		}
	}
	
	
	public void receive(ReceiverListener receiver_listener)
	{	
		System.out.println("start receiver listener");
		this.swingWorkder = new SwingWorker<Void, ReceptionEvent>()  
        { 
			private Instant start;
			private Instant finish;
            @Override
            protected Void doInBackground()
            { 
            	if(this.isCancelled() || stop.get())
            		return null;
            	
            	UdpSocketList socket_list = null;
            	Manifest manifest = null;
        		PacketManager manager = null;
        		
            	try
            	{
	            	socket_list = new UdpSocketList(receiver_listener.addressList);
	            	socket_list.open();
	        		
	        		System.out.println("Waiting for manifest");
	        		try
	        		{
	        			manifest = getManifest(socket_list);
	        			
	        			ReceptionEvent event = new ManifestEvent(manifest);
	    				publish(event);
	    				
	        		}catch(InvalidPacketStreamException e)
	        		{
	        			System.out.println("manifest reception fail");
	        			
	        			ReceptionEvent event = new ManifestEvent(e);
	    				publish(event);
	    				
	    				return null;
	        		}catch(CancellationException e)
	        		{
	        			ReceptionEvent event = new ManifestEvent(e);
	    				publish(event);
	    				
	    				return null;
	        		}
	    			
	        		
	        		try
	        		{
	        			manager = mapIdList.get(manifest.id);
	        			if(manager==null)
	        			{System.out.println("oki1");
	        				PacketBufferInfo info = new PacketBufferInfo(manifest.blockSize);
	        				manager = PacketManager.getManager(Parms.instance().receiver().getWorkspace(), manifest, info, 
	        						receiver_listener.nbPacketHold, receiver_listener.nbPacketBlock, receiver_listener.bufferSizeFile, 
	        						receiver_listener.timeoutNanos);
	        				mapIdList.put(manifest.id, manager);
	        			}
	        			
	        			publish(new LockEvent());
	        			System.out.println("start reassembly");
	        			start = Instant.now();
	        			
	        			
	        			reassemble(manager, socket_list, receiver_listener.timeoutNanos);
	        			
	        			
	        			
	
	        		}catch(EndOfPacketReassemblingException e)
	        		{
	        			System.out.println("END OF STREAM");
	        			try 
	        			{
	        				String info = finalizeReceivedOperation(manager);
	        				System.out.println("INFO="+info);
	        				mapIdList.remove(manifest.id);
	        				
	        				finish = Instant.now();
	        				long timeElapsed = Duration.between(start, finish).toMillis();
	    					System.out.println("time="+timeElapsed);
	        				ReceptionEvent event = new ReceivedEvent(manifest, info);
	        				publish(event);
	        				
	        				
	        			} catch (Exception ex) 
	        			{
	        				System.out.println("oki 1 expeitojn="+ex);
	        				ReceptionEvent event = new ReceivedEvent(manifest, ex);
	        				publish(event);
	        				
	        				return null;
	        			}
	        			
	        		}
	        		catch(Exception e)
	        		{System.out.println("oki 2 exception="+e);
	        		
	        			ReceptionEvent event = new ReceivedEvent(manifest, e);
	    				publish(event);
	    				
	    				return null;
	        		}
            	}catch(Exception e)
            	{
            		ReceptionEvent event = new ReceivedEvent(manifest, e);
    				publish(event);
            	}
        		finally
        		{
        			try 
        			{
						socket_list.close();
					} catch (Exception e) 
        			{
						ReceptionEvent event = new ReceivedEvent(manifest, e);
	    				publish(event);
					}
        		}
        		
        		return null;
            } 
  
            @Override
            protected void process(List<ReceptionEvent> events) 
            { 
                // define what the event dispatch thread  
                // will do with the intermediate results received 
                // while the thread is executing 
            	for(ReceptionEvent event : events)
            	{
            		receiver_listener.listener.stateChanged(new ChangeEvent(event));
            	}
            } 
  
            @Override
            protected void done()  
            { 
                // this method is called when the background  
                // thread finishes execution 
                try 
                { 
                	get(); 
                	System.out.println("oki end");	
                }  
                catch (InterruptedException e)  
                { 
                    e.printStackTrace(); System.out.println("oki end error 1");
                }  
                catch (ExecutionException e)  
                { 
                    e.printStackTrace(); System.out.println("oki end error 2");
                }catch(CancellationException e)
                {System.out.println("oki end error 3");
                	e.printStackTrace();
                }
            	
            	receiver_listener.listener.stateChanged(new ChangeEvent(new UnlockEvent()));
            	
            	ReceiverListener rl = received_request.poll();
            	if(rl!=null)
            	{
            		System.out.println("start new receiver");	
            		receive(rl);
            	}
            } 
        }; 
        
        swingWorkder.execute();
        
				
		/*catch(ReassemblingException e)
		{
			System.out.println("END OF STREAM BUT CANNOT REASSEMBLE ALL THE CONTENT ERROR");
			throw new ReassemblingException(e);
		}catch(TimeoutException e)
		{
			System.out.println("TIMEOUT");
			throw new TimeoutException();
		}catch(InvalidPacketStreamException e)
		{
			throw new InvalidPacketStreamException(e);
		}catch(Exception e)
		{
			//e.printStackTrace();
			throw new Exception(e);
			
		}*/
		
	}
	
	public boolean addReceivedRequest(ReceiverListener receiver_listener)
	{
		if(receiver_listener==null)
			return false;
		
		receiver_listener.setDatetime();
		
		if(swingWorkder==null || swingWorkder.isDone())
			receive(receiver_listener);
		else
			received_request.add(receiver_listener);
		
		return true;
	}
	
	public void dropReceivedObject(long id) throws IOException
	{
		PacketManager manager = mapIdList.remove(id);
		if(manager==null)
			return;
		
		manager.clear();
	}
	
	public void SaveOnDiskReceivedObject(long id) throws IOException
	{
		PacketManager manager = mapIdList.remove(id);
		if(manager==null)
			return;
		
		manager.close();
	}
	
	public void removeReceivedObjectFromMemory(long id)
	{
		mapIdList.remove(id);
	}
		
	/*public void saveInMemoryReceivedObject(long id)
	{
		ConcurrentPacketList pool = mapIdList.get(id);
		if(pool==null)
		{
			throw new IllegalArgumentException();
		}
		
		Manifest manifest = pool.getManifest();
		
		ConcurrentPacketList tmp = mapIdList.get(manifest.id);
		if(tmp!=null && tmp!=pool)
			throw new IllegalArgumentException();
		
		mapIdList.put(manifest.id, pool);
		pool = null;
	}*/
	
	/*public double progressionRate(long id)
	{
		if(pool==null)
			return 0;
		
		return (double)pool.getTotalNumberPacket()/(double)pool.getCurrentNumberPacket();
	}*/
	
	
	private byte[] getReceivedMetadata(PacketManager manager) throws IOException, IncompleteContentException
	{
		if(manager==null)
			return null;
		
		PacketContent content = manager.getContent(PacketType.TYPE_METADATA);
		if(content==null)
			return null;
		
		if(content.inMemory)
		{
			System.out.println("***************************in memory");
			return (byte[]) content.content;
		}
		else
		{System.out.println("*************************filename="+(String)content.content);
			return Tools.getBytesFromFile((String)content.content);
		}
	}
	
	public byte[] getReceivedMetadata(long id) throws IOException, IncompleteContentException
	{
		PacketManager manager = mapIdList.get(id);
		if(manager==null)
		{
			throw new IllegalArgumentException();
		}
		
		return getReceivedMetadata(manager);
		
	}
	
	private Object getReceivedData(PacketManager manager) throws IncompleteContentException
	{
		if(manager==null)
		{
			System.out.println("POOL IS NULL get content data");
			return null;
		}
		
		return manager.getContent(PacketType.TYPE_DATA).content;
	}
		
	public Object getReceivedData(long id) throws IncompleteContentException
	{
		PacketManager manager = mapIdList.get(id);
		if(manager==null)
		{
			throw new IllegalArgumentException();
		}
		
		return getReceivedData(manager);
	}
	
	private String finalizeReceivedOperation(PacketManager manager) throws Exception
	{
		if(manager==null)
		{
			throw new IllegalArgumentException();
		}
		Manifest manifest = manager.getManifest();
		
		
		manager.flush();
		
		ContentActionInterface itf = manifest.type.getAction();
		if(itf==null)
			return null;
		
		return itf.actionOn(getReceivedMetadata(manager), getReceivedData(manager));
	}
		
	public String finalizeReceivedOperation(long id) throws Exception
	{
		PacketManager manager = mapIdList.get(id);
		if(manager==null)
		{
			throw new IllegalArgumentException();
		}
		
		return finalizeReceivedOperation(manager);
	} 
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static class UdpSocketList
	{
		private int index;
		private UdpSocket[] socket_list;
		private NetworkAddress[] address_list;
		
		public UdpSocketList(NetworkAddress[] address_list) throws OutOfMemoryError, InterruptedException
		{
			if(address_list==null)
				throw new IllegalArgumentException();
			
			this.address_list = address_list;
		}
		
		public void open() throws OutOfMemoryError, InterruptedException
		{
			this.socket_list = openSocketList(address_list);
		}
		
		public void close() throws Exception
		{
			closeSocketList(socket_list);
		}
		
		public byte[] getPacket() throws OutOfMemoryError, InterruptedException
		{
			if(socket_list==null)
				return null;
			
			byte[] packet = null;
			
			for(; index<socket_list.length; index++)
			{
				packet = socket_list[index].getReceivedPacket();
				if(packet!=null)
					break;
			}
			index++;
			
			if(index>=socket_list.length)
				index = 0;
			
			return packet;
		}
		
		
		private static UdpSocket[] openSocketList(NetworkAddress address_list[]) throws OutOfMemoryError, InterruptedException
		{
			UdpSocket[] socket_list = new UdpSocket[address_list.length];
			for(int i=0; i<socket_list.length; i++)
			{
				NetworkAddress address = address_list[i];
				UdpSocket socket = socket_list[i] = new UdpSocket(address.getIp().getHostName(), address.getPort(), Environment.MAXIMUN_PACKET_SIZE);
				socket.open();
			}
			
			return socket_list;
		}
		
		private static void closeSocketList(UdpSocket[] socket_list) throws Exception
		{
			if(socket_list==null)
				return ;
			
			Exception ex = null;
			
			for(UdpSocket socket : socket_list)
			{
				try
				{
					if(socket==null)
						continue;
					
					socket.close();
				}catch(Exception e)
				{
					ex = e; 
				}
			}
			
			if(ex!=null)
			{
				throw ex;
			}
		}
	}
	
}
