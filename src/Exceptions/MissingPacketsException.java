package Exceptions;

import PacketTools.MissingPackets;

public class MissingPacketsException extends Exception
{
	private static final long serialVersionUID = -4655128733518718760L;
	
	private MissingPackets missing_packets;
	
	public MissingPacketsException(MissingPackets missing_packets) 
	{
		this.missing_packets = missing_packets;
	}
	
	public MissingPacketsException(String msg) 
	{
		super(msg);
	}
	
	public MissingPacketsException(String errorMessage, Throwable err) 
	{
        super(errorMessage, err);
    }
	
	public MissingPacketsException(Exception e, MissingPackets missing_packets) 
	{
        super(e.getMessage(), e.getCause());
        this.missing_packets = missing_packets;
    }
	
	public MissingPackets getPackets()
	{
		return missing_packets;
	}
}
