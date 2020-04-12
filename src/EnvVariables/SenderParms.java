package EnvVariables;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SenderParms
{
	private int mmu_packet_length;
	private String in_path;
	
	protected SenderParms() {}
	
	@XmlElement(name = "MMUPacketLength")
	public void setMMUPacketLength(int length)
	{
		this.mmu_packet_length = length;
	}
	
	public int getMMUPacketLength()
	{
		return mmu_packet_length;
	}
	
	@XmlElement(name = "InPath")
	public void setInPath(String path)
	{
		this.in_path = path;
	}
	
	public String getInPath()
	{
		return in_path;
	}
}
