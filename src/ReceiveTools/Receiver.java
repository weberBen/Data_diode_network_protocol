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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

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
import PacketTools.Checksum;
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
	
	private StreamInfo streamInfo;
	private HashMap<Long, ConcurrentPacketList> mapIdList;
	private PacketReceiver[] receive_threads;
	private NetworkAddress address;
	private ConcurrentPacketList pool;
	private ConcurrentLinkedQueue<ReceiverListener> received_request;
	private SwingWorker<Void, ReceptionEvent> swingWorkder;
	private AtomicBoolean is_running;
	
	public Receiver()
	{
		this.mapIdList = new HashMap<Long, ConcurrentPacketList>();
		this.received_request = new ConcurrentLinkedQueue<ReceiverListener>();
		this.swingWorkder = null;
		this.is_running = new AtomicBoolean(false);
	}
	
	private PacketReceiver[] setReceiveThread(NetworkAddress address, StreamInfo streamInfo, int buffer_size) throws SocketException
	{
		int[] ports = address.getPorts();
		PacketReceiver[] list = new PacketReceiver[ports.length];
		for(int i=0; i<list.length; i++)
		{
			list[i] = new PacketReceiver(ports[i], address.getIp(), streamInfo, buffer_size);
		}
		
		return list;
	}
	
	private void startThreads(Runnable[] list)
	{
		Thread t;
		for(Runnable r : list)
		{
			t = new Thread(r);
	        t.setDaemon(true);//stop at the end of the JVM
	        t.start(); 
		}
	}
	
	private void endThreads(PacketReceiver[] list)
	{
		for(PacketReceiver r : list)
		{
			r.close();
		}
	}
	

	public void close() throws IOException
	{
		if(swingWorkder!=null)
			swingWorkder.cancel(true);
			
		endThreads(receive_threads);
		
		for (Map.Entry<Long, ConcurrentPacketList>entry : mapIdList.entrySet()) 
		{
			Long manifest_id = entry.getKey();
		    ConcurrentPacketList list = entry.getValue();
		    
		    list.close();
		}
		
		if(pool!=null)
		{
			pool.close();
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
            	if(this.isCancelled())
            		return null;
            	
				
				
            	streamInfo = new StreamInfo();
        		if(address==null || !address.equals(receiver_listener.address))
        		{
        			try
        			{
        				receive_threads = setReceiveThread(receiver_listener.address, streamInfo, Environment.MAXIMUN_PACKET_SIZE);
        				
        			}catch(SocketException e)
        			{
        				ReceptionEvent event = new ReceivedEvent(new SocketException(e.getMessage()));
        				publish(event);
        				
        				return null;
        			}catch(CancellationException e)
        			{
        				publish(new CancellationEvent());
        				return null;
        			}
        		}
        		
        		startThreads(receive_threads);
        		
        		System.out.println("Waiting for manifest");
        		Manifest manifest = null;
        		try
        		{
        			manifest = streamInfo.getManifest();
        		}catch(InvalidPacketStreamException e)
        		{
        			System.out.println("manifest reception fail");
        			
        			ReceptionEvent event = new ManifestEvent(new InvalidPacketStreamException(e));
    				publish(event);
    				
    				endThreads(receive_threads);
    				
    				return null;
    				
        		}
        		System.out.println("manifest get");		
        		
        		
        		try
        		{
        			pool = mapIdList.get(manifest.id);
        			if(pool==null)
        			{System.out.println("oki1");
        				PacketBufferInfo info = new PacketBufferInfo(manifest.blockSize);
        				pool = new ConcurrentPacketList(Parms.instance().receiver().getWorkspace(), manifest, info, 
        						receiver_listener.nbPacketHold, receiver_listener.nbPacketBlock, receiver_listener.bufferSizeFile, 
        						receiver_listener.timeoutNanos);
        				mapIdList.put(manifest.id, pool);
        			}
        			streamInfo.setPacketPool(pool);
        			publish(new LockEvent());
        			System.out.println("start reassembly");
        			start = Instant.now();
        			pool.reassemble();

        		}catch(EndOfPacketReassemblingException e)
        		{
        			System.out.println("END OF STREAM");
        			try 
        			{
        				String info = finalizeReceivedOperation(pool);
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
        				ReceptionEvent event = new ReceivedEvent(manifest, new Exception(ex));
        				publish(event);
        				
        				return null;
        			}
        			
        		}
        		catch(Exception e)
        		{System.out.println("oki 2 exception="+e);
        		
        			ReceptionEvent event = new ReceivedEvent(manifest, new Exception(e));
    				publish(event);
    				
    				return null;
        		}
        		finally
        		{
        			if(pool!=null)
        			try 
        			{
        				pool.flush();
        			} catch (IOException e) 
        			{
        				ReceptionEvent event = new ReceivedEvent(manifest, new Exception(e));
        				publish(event);
        			}
        			endThreads(receive_threads);
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
                	endThreads(receive_threads);
            		
                	streamInfo.stop();
                	
            		if(pool!=null)
            		{
            			pool.interputReassembly();
            			try
            			{
            				pool.clear();
            			}catch(IOException ex) {}
            			pool = null;
            		}
                }
            	
            	receiver_listener.listener.stateChanged(new ChangeEvent(new UnlockEvent()));
            	
            	ReceiverListener receiver_listener = received_request.poll();
            	if(receiver_listener!=null)
            	{
            		System.out.println("start new receiver");	
            		receive(receiver_listener);
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
	
	
	public void interupt() throws IOException
	{
		if(swingWorkder!=null)
		{
			swingWorkder.cancel(true);
		}
		else
		{
			endThreads(receive_threads);
    		
        	streamInfo.stop();
        	
    		if(pool!=null)
    		{
    			pool.interputReassembly();
    			try
    			{
    				pool.clear();
    			}catch(IOException ex) {}
    			pool = null;
    		}
		}
	}
	
	public void dropReceivedObject(long id) throws IOException
	{
		ConcurrentPacketList pool = mapIdList.remove(id);
		if(pool==null)
			return;
		
		pool.clear();
	}
	
	public void SaveOnDiskReceivedObject(long id) throws IOException
	{
		ConcurrentPacketList pool = mapIdList.remove(id);
		if(pool==null)
			return;
		
		pool.close();
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
	
	
	private byte[] getReceivedMetadata(ConcurrentPacketList pool) throws IOException, IncompleteContentException
	{
		if(pool==null)
			return null;
		
		PacketContent content = pool.getContent(PacketType.TYPE_METADATA);
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
		ConcurrentPacketList pool = mapIdList.get(id);
		if(pool==null)
		{
			throw new IllegalArgumentException();
		}
		
		return getReceivedMetadata(pool);
		
	}
	
	private Object getReceivedData(ConcurrentPacketList pool) throws IncompleteContentException
	{
		if(pool==null)
		{
			System.out.println("POOL IS NULL get content data");
			return null;
		}
		
		return pool.getContent(PacketType.TYPE_DATA).content;
	}
		
	public Object getReceivedData(long id) throws IncompleteContentException
	{
		ConcurrentPacketList pool = mapIdList.get(id);
		if(pool==null)
		{
			throw new IllegalArgumentException();
		}
		
		return getReceivedData(pool);
	}
	
	private String finalizeReceivedOperation(ConcurrentPacketList pool) throws Exception
	{
		if(pool==null)
		{
			throw new IllegalArgumentException();
		}
		Manifest manifest = pool.getManifest();
		
		
		pool.flush();
		
		ContentActionInterface itf = manifest.type.getAction();
		if(itf==null)
			return null;
		
		return itf.actionOn(getReceivedMetadata(pool), getReceivedData(pool));
	}
		
	public String finalizeReceivedOperation(long id) throws Exception
	{
		ConcurrentPacketList pool = mapIdList.get(id);
		if(pool==null)
		{
			throw new IllegalArgumentException();
		}
		
		return finalizeReceivedOperation(pool);
	} 
	
	private static class StreamInfo
	{
		private AtomicBoolean is_waiting;
		private PacketReader packetReader;
		private volatile ConcurrentPacketList packets_pool;
		private AtomicBoolean buffer_set;
		private AtomicBoolean stop;
		private volatile byte[] buffer;
		
		public StreamInfo()
		{
			this.packetReader = new PacketReader(Manifest.BYTES);
			this.is_waiting = new AtomicBoolean(true);
			this.buffer_set = new AtomicBoolean(false);
			this.stop = new AtomicBoolean(false);
		}
		
		private void stop()
		{
			this.stop.set(true);
		}
		
		public void setManifest(byte[] buffer)
		{
			if(buffer==null)
				return;
			
			while(!buffer_set.compareAndSet(false, true))
			{
				
			}
			//System.out.println("buffer set null=?"+(buffer==null));
			this.buffer = buffer.clone();
			//System.out.println("\t buffer null=?"+(this.buffer==null)+"  - euals="+Arrays.equals(this.buffer, buffer));
		}
		
		public boolean isWaitingManifest()
		{
			return is_waiting.get();
		}
		
		public Manifest getManifest() throws InvalidPacketStreamException
		{
			is_waiting.set(true);
			
			Manifest manifest = null;
			while((buffer==null || !buffer_set.get()) && !stop.get())
			{
				
			}
			if(buffer==null)
			{
				System.out.println("buffer is null");
			}
			if(stop.compareAndSet(true, false))
			{
				throw new CancellationException();
			}
			
			if(!packetReader.depack(buffer))
			{
				System.out.println("manifest checksum false");
				buffer_set.set(false);
				throw new InvalidPacketStreamException();
			}
			
			if(!packetReader.getHeader().getType().isManifest())
			{
				buffer_set.set(false);
				throw new InvalidPacketStreamException();
			}
			
			manifest = Manifest.readExternal(buffer, packetReader.getOffsetData());
			if(manifest==null)
			{
				buffer_set.set(false);
				throw new InvalidPacketStreamException();
			}
			buffer_set.set(false);
			
			is_waiting.set(false);
			
			return manifest;
		}
		
		public ConcurrentPacketList getPacketPool()
		{
			return packets_pool;
		}
		
		public void setPacketPool(ConcurrentPacketList pool)
		{
			this.packets_pool = pool;
		}
	}
	
	
	private static class PacketReceiver implements Runnable
	{
		private static int count = 0;
		public final int ID;
		private volatile boolean stop;
		private volatile DatagramSocket socket;
		private StreamInfo streamInfo;
		private int buffer_size;
		
		public PacketReceiver(int port, InetAddress ip, StreamInfo streamInfo, int buffer_size) throws SocketException
		{
			this.ID = count++;
			this.stop = false;
			this.socket = new DatagramSocket(port, ip);
			this.streamInfo = streamInfo;
			this.buffer_size = buffer_size;
		}
		
		
		public void close()
		{
			stop = true;
			socket.close();
		}
		
		public void run()
		{	
			byte[] buffer;
			DatagramPacket packet;
			ConcurrentPacketList packets_pool;
			
			while(!stop)
			{
		        try 
		        {
		        	buffer = new byte[buffer_size];
		        	packet = new DatagramPacket(buffer, buffer.length);
		        	
					socket.receive(packet);
		        	
		        	packet.setData(buffer);
		        	packet.setLength(buffer.length);
					
					if(streamInfo.isWaitingManifest())
					{System.out.println("thread "+ID+"  get manifest");
						streamInfo.setManifest(buffer);
						System.out.println("thread "+ID+" manifest end");
					}
					else
					{//System.out.println("thread "+ID+"  get packet");
						packets_pool = streamInfo.getPacketPool();
						if(packets_pool!=null)
							packets_pool.add(packet.getData());
					}
					
				} catch (IOException e) 
		        {
					e.printStackTrace();
					break;
				}
			}
		}
	}
}
