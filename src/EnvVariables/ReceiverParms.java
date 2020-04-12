package EnvVariables;

import java.io.OutputStream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class ReceiverParms
{
	private int number_packet_to_hold;
	private int number_packet_reassimbling_block;
	private String out_path;
	private String workspace;
	private long timeout_ns;
	private OutputStream defautl_out_stream;
	
	protected ReceiverParms() {}
	
	@XmlElement(name = "NumberPacketToHold")
	public void setNumberPacketToHold(int nb)
	{
		this.number_packet_to_hold = nb;
	}
	
	public int getNumberPacketToHold()
	{
		return number_packet_to_hold;
	}
	
	
	@XmlElement(name = "NumberPacketBlock")
	public void setNumberPacketBlock(int nb)
	{
		this.number_packet_reassimbling_block = nb;
	}
	
	public int getNumberPacketBlock()
	{
		return number_packet_reassimbling_block;
	}
	
	@XmlElement(name = "OutPath")
	public void setOutPath(String path)
	{
		this.out_path = path;
	}
	
	public String getOutPath()
	{
		return out_path;
	}
	
	
	@XmlElement(name = "WorkspacePath")
	public void setWorkspace(String path)
	{
		this.workspace = path;
	}
	
	public String getWorkspace()
	{
		return workspace;
	}
	
	@XmlElement(name = "Timeout")
	public void setTimeoutNs(long time_nanosecond)
	{
		this.timeout_ns = time_nanosecond;
	}
	
	public long getTimeoutNs()
	{
		return timeout_ns;
	}
	
	@XmlTransient
	public void setDefaultOutStream(OutputStream stream)
	{
		this.defautl_out_stream = stream;
	}
	
	public OutputStream getDefaultOutStream()
	{
		return defautl_out_stream;
	}
}
