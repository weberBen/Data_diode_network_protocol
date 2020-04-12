package UserInterface;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import generalTools.NetworkAddress;

public class ReceiverListener
{
	public final NetworkAddress address;
	public final int nbPacketHold;
	public final int nbPacketBlock;
	public final int bufferSizeFile;
	public final long timeoutNanos;
	public final ChangeListener listener;
	private LocalDateTime datetime;
	
	public final ArrayList<ConcurrentLinkedQueue<byte[]>> list;
	
	
	public ReceiverListener (NetworkAddress address, int nb_packet_hold, int nb_packet_block,  int buffer_size_file, 
			long timeout_ns, ChangeListener listener, ArrayList<ConcurrentLinkedQueue<byte[]>> list)
	{
		this.address = address;
		this.nbPacketHold = nb_packet_hold;
		this.nbPacketBlock = nb_packet_block;
		this.bufferSizeFile = buffer_size_file;
		this.timeoutNanos = timeout_ns;
		this.listener = listener;
		
		this.list = list;
	}
	
	public void setDatetime()
	{
		this.datetime = LocalDateTime.now();
	}
	
	public LocalDateTime getDatetime()
	{
		return datetime;
	}
	
	
}
