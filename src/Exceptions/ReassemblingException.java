package Exceptions;

public class ReassemblingException extends Exception
{
	private static final long serialVersionUID = -8399475446768798993L;

	public ReassemblingException() {}
	public ReassemblingException(String msg) 
	{
		super(msg);
	}
	
	public ReassemblingException(String errorMessage, Throwable err) 
	{
        super(errorMessage, err);
    }
	
	public ReassemblingException(Throwable err) 
	{
        super(err);
    }
}
