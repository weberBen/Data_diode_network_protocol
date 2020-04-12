package EnvVariables;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import generalTools.NetworkAddress;

@XmlRootElement
public class Parms 
{
	private int buffer_file_size;
	private NetworkAddress network_config;
	private ReceiverParms receiver;
	private SenderParms sender;
	
	@XmlTransient
	private static Parms instance;
	private static final String filename = "/home/benjamin/eclipse-workspace/UDP/test/parms.xml";
	
	protected Parms() {}
	
	public static Parms instance()
	{
		return instance;
	}
	
	public static void load() throws JAXBException
	{
		if(instance!=null)
			return ;
		
		File file = new File(filename);
		if((!file.exists()) || (file.exists() && !file.isFile()))
		{
			instance = new Parms();
		}else
		{
	        JAXBContext jaxbContext = JAXBContext.newInstance(Parms.class);
	        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
	        instance = (Parms)jaxbUnmarshaller.unmarshal(file);
		}
	}
	
	public static void save() throws JAXBException
	{
		if(instance==null)
			return;
		
		File file = new File(filename);
	    JAXBContext jaxbContext = JAXBContext.newInstance(Parms.class);

	    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

	    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	    jaxbMarshaller.marshal(instance, file);
	}
	
	@XmlElement(name = "BufferFileSize")
	public void setBufferFileSize(int length)
	{
		this.buffer_file_size = length;
	}
	
	public int getBufferFileSize()
	{
		return buffer_file_size;
	}
	
	@XmlElement(name = "NetworkConfig")
	public void setNetworkConfig(NetworkAddress address)
	{
		this.network_config = address;
	}
	
	public NetworkAddress getNetworkConfig()
	{
		if(network_config==null)
			return new NetworkAddress();
		
		return network_config;
	}
	
	@XmlElement(name = "Receiver")
	public void setReceiver(ReceiverParms receiver)
	{
		this.receiver = receiver;
	}
	
	public ReceiverParms getReceiver()
	{
		if(receiver==null)
			return new ReceiverParms();
		
		return receiver;
	}
	
	public ReceiverParms receiver()
	{
		return getReceiver();
	}
	
	@XmlElement(name = "Sender")
	public void setSender(SenderParms sender)
	{
		this.sender = sender;
	}
	
	public SenderParms getSender()
	{
		if(sender==null)
			return new SenderParms();
		
		return sender;
	}
	
	public SenderParms sender()
	{
		return getSender();
	}
}
