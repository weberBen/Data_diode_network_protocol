package Exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import PacketConstructor.PacketType;

public class IncompleteContentException extends Exception
{
	private static final long serialVersionUID = -8399298478587498993L;
	private final List<PacketType> incomplete_content_type;
	
	public IncompleteContentException() 
	{
		this.incomplete_content_type = null;
	}
	
	public IncompleteContentException(List<PacketType> incomplete_content_type) 
	{
		this.incomplete_content_type = incomplete_content_type;
	}
	
	public IncompleteContentException(String msg, ArrayList<PacketType> incomplete_content_type) 
	{
		super(msg);
		this.incomplete_content_type = Collections.unmodifiableList(incomplete_content_type);
	}
	
	public IncompleteContentException(String msg)
	{
		super(msg);
		this.incomplete_content_type = null;
	}
	
	public IncompleteContentException(String errorMessage, Throwable err, ArrayList<PacketType> incomplete_content_type) 
	{
        super(errorMessage, err);
        this.incomplete_content_type = Collections.unmodifiableList(incomplete_content_type);
    }
	
	public IncompleteContentException(String errorMessage, Throwable err)
	{
		super(errorMessage, err);
        this.incomplete_content_type = null;
	}
	
	public IncompleteContentException(Throwable err, ArrayList<PacketType> incomplete_content_type) 
	{
        super(err);
        this.incomplete_content_type = Collections.unmodifiableList(incomplete_content_type);
    }
	
	public IncompleteContentException(Throwable err)
	{
		super(err);
        this.incomplete_content_type = null;
	}
	
	public List<PacketType> getIncompletePacketTypes()
	{
		return incomplete_content_type;
	}
	
	
}
