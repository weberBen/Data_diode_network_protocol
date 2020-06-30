package GeneralTools;

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
	private int port;
	
	public NetworkAddress() {}
	
	public NetworkAddress(InetAddress ip, int port)
	{
		if(port<0 || port>65535)
			throw new IllegalArgumentException();
		
		this.intAdrress = ip;
		this.port = port;
	}
	
	public NetworkAddress(String ip, int port) throws UnknownHostException
	{
		this(InetAddress.getByName(ip), port);
	}
	
	public NetworkAddress(byte[] ip, int port) throws UnknownHostException
	{
		this(InetAddress.getByAddress(ip), port);
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
	public void setPort(int port)
	{
		if(port<0 || port>65535)
			throw new IllegalArgumentException();
		
		this.port = port;
	}
	
	public int getPort()
	{
		return port;
	}
	
	
	@Override
	public String toString()
	{
		return "NetworkConfig(ip="+intAdrress.getHostAddress()+", port="+port+")";
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
		
		return intAdrress.equals(adrs.intAdrress) && port==adrs.port;
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