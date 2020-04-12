package generalTools;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement
public class NetworkAddress
{
	@XmlElement(name = "IntAdrress")
	@XmlJavaTypeAdapter(InetAddressAdapter.class)
	private InetAddress intAdrress;
	private int[] portList;
	
	public NetworkAddress() {}
	
	public NetworkAddress(InetAddress ip, int[] port_list)
	{
		for(int i=0; i<port_list.length; i++)
		{
			for(int j=0; j<port_list.length; j++)
			{
				if(j==i)
					continue;
				if(port_list[i]==port_list[j])
					throw new IllegalArgumentException();
			}
		}
		
		this.intAdrress = ip;
		this.portList = port_list;
	}
	
	public NetworkAddress(String ip, int[] port_list) throws UnknownHostException
	{
		this(InetAddress.getByName(ip), port_list);
	}
	
	public NetworkAddress(byte[] ip, int[] port_list) throws UnknownHostException
	{
		this(InetAddress.getByAddress(ip), port_list);
	}
	
	@XmlTransient
	public void setIp(String ip) throws UnknownHostException
	{
		this.intAdrress = InetAddress.getByName(ip);
	}
	
	@XmlTransient
	public void setIp(byte[] ip) throws UnknownHostException
	{
		this.intAdrress = InetAddress.getByAddress(ip);
	}
	
	@XmlTransient
	public void setIp(InetAddress ip)
	{
		this.intAdrress = ip;
	}
	
	public InetAddress getIp()
	{
		return intAdrress;
	}
	
	@XmlElement(name = "Ports")
	public void setPorts(int[] ports)
	{
		this.portList = ports;
	}
	
	public int[] getPorts()
	{
		return portList;
	}
	
	
	@Override
	public String toString()
	{
		String ports = "[";
		if(portList.length!=0)
		{
			for(int i=0; i<portList.length-1; i++)
			{
				ports+=portList[i] + ",";
			}
			ports+=portList[portList.length-1];
		}else
		{
			ports+=" ";
		}
		ports+="]";
		
		return "NetworkConfig(ip="+intAdrress.getHostAddress()+", ports="+ports+")";
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o==null)
			return false;
		if(o==this)
			return true;
		if(o.getClass()!=getClass())
			return false;
		
		NetworkAddress adrs = (NetworkAddress)o;
		
		return intAdrress.equals(adrs.intAdrress) && Arrays.equals(portList, adrs.portList);
	}
	
	private static class InetAddressAdapter extends XmlAdapter<String, InetAddress>
	{

	    @Override
	    public InetAddress unmarshal(String v) throws Exception 
	    {
	        return InetAddress.getByName(v);
	    }

	    @Override
	    public String marshal(InetAddress v) throws Exception 
	    {
	       return v.getHostAddress();
	    }

	}
}