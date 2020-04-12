package Exceptions;

public class InvalidPacketStreamException extends Exception
{
	private static final long serialVersionUID = -8399298467687498993L;

	public InvalidPacketStreamException() {}
	public InvalidPacketStreamException(String msg) 
	{
		super(msg);
	}
	
	public InvalidPacketStreamException(String errorMessage, Throwable err) 
	{
        super(errorMessage, err);
    }
	
	public InvalidPacketStreamException(Throwable err) 
	{
        super(err);
    }
}
