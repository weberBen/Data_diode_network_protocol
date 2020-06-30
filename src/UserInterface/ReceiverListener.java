package UserInterface;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import GeneralTools.NetworkAddress;

public class ReceiverListener
{
	public final NetworkAddress[] addressList;
	public final int nbPacketHold;
	public final int nbPacketBlock;
	public final int bufferSizeFile;
	public final long timeoutNanos;
	public final ChangeListener listener;
	private LocalDateTime datetime;
	
	
	public ReceiverListener (NetworkAddress[] address_list, int nb_packet_hold, int nb_packet_block,  int buffer_size_file, 
			long timeout_ns, ChangeListener listener)
	{
		this.addressList = address_list;
		this.nbPacketHold = nb_packet_hold;
		this.nbPacketBlock = nb_packet_block;
		this.bufferSizeFile = buffer_size_file;
		this.timeoutNanos = timeout_ns;
		this.listener = listener;
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
