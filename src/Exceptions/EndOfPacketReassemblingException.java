package Exceptions;

public class EndOfPacketReassemblingException extends Exception
{

	private static final long serialVersionUID = -8399194467687498993L;

	public EndOfPacketReassemblingException() {}
	public EndOfPacketReassemblingException(String msg) 
	{
		super(msg);
	}
	
	public EndOfPacketReassemblingException(String errorMessage, Throwable err) 
	{
        super(errorMessage, err);
    }
	
	public EndOfPacketReassemblingException(Throwable err) 
	{
        super(err);
    }
}
