package Exceptions;

public class SaveObjectException extends Exception
{
	private static final long serialVersionUID = -7697928467086137837L;

	public SaveObjectException() {}
	
	public SaveObjectException(String msg) 
	{
		super(msg);
	}
	
	public SaveObjectException(String errorMessage, Throwable err) 
	{
        super(errorMessage, err);
    }
	
	public SaveObjectException(Throwable err) 
	{
        super("", err);
    }
}
