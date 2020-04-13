package UserInterface;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import EnvVariables.Parms;
import ReceiveTools.Receiver;
import generalTools.NetworkAddress;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ReceiverInterface 
{
	private Receiver receiver;
	private Scanner scanner;
	private NetworkAddress networkConf;
	private int nb_packet_to_hold;
	private int nb_packet_block;
	private int buffer_file_size;
	private long timeout;
	private static int THREAD_COUNT = 0;
	
	public ReceiverInterface(NetworkAddress networkConf, int nb_packet_to_hold, int nb_packet_block, int buffer_file_size, long timeout)
	{
		this.receiver = new Receiver();
		this.scanner = new Scanner(System.in);
		this.networkConf = networkConf;
		this.nb_packet_to_hold = nb_packet_to_hold;
		this.nb_packet_block = nb_packet_block;
		this.buffer_file_size = buffer_file_size;
		this.timeout = timeout;
	}
	
	private int getNewThreadId()
	{
		return THREAD_COUNT++;
	}
	
	private void display(String txt)
	{
		//System.console().writer().println(">>"+txt);
		System.out.println(">>"+txt);
	}
	
	private String getResponse()
	{
		return scanner.nextLine();
	}
	
	private static Instant start;
	private static Instant finish;
	public void start() throws IOException
	{
		display("start");
		
		
		ChangeListener changeListner = new ChangeListener() 
		{
			private int id = getNewThreadId();
			
			@Override
			public void stateChanged(ChangeEvent event) {
			
				
				ReceptionEvent rep_event = (ReceptionEvent)event.getSource();
				
				if(rep_event instanceof ManifestEvent)
				{
					ManifestEvent evt = (ManifestEvent)rep_event;
					if(evt.manifest!=null)
					{
						System.out.println("manifest received ="+evt.manifest);
					}else
					{
						System.out.println("manifest exeption="+evt.exception);
						evt.exception.printStackTrace();
						try {
							receiver.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}else if(rep_event instanceof LockEvent)
				{start = Instant.now();
					System.out.println("interface thread id="+id+" has lock");
				}else if(rep_event instanceof UnlockEvent)
				{
					System.out.println("interface thread id="+id+" has looses lock");
				}else if(rep_event instanceof ReceivedEvent)
				{
					finish = Instant.now();
					if(start!=null)
					{	long timeElapsed = Duration.between(start, finish).toMillis();
						System.out.println("time="+timeElapsed);
					}
					
					ReceivedEvent evt = (ReceivedEvent)rep_event;
					if(evt.exception!=null)
					{
						System.out.println("received object from stream id="+evt.manifest.id+" equals="+evt.exception);
						evt.exception.printStackTrace();
					}else
					{
						
						if(evt.manifest==null)
						{
							System.out.println("received object from stream , exception="+evt.exception);
							evt.exception.printStackTrace();
						}else
						{
							System.out.println("received object from stream id="+evt.manifest.id+" info="+evt.info);
						}
					}
					
					try {
						receiver.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			 
			}
		};
		
		System.out.println("adresse ports="+networkConf);
		ReceiverListener receiver_listener = new ReceiverListener(networkConf, nb_packet_to_hold, nb_packet_block, buffer_file_size, timeout, changeListner);
		
		receiver.addReceivedRequest(receiver_listener);
		
		
		String line = scanner.nextLine();
		
		
	}
}
