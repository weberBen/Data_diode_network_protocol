package Exceptions;

import java.util.ArrayList;

public class ReadingPacketException extends Exception
{
	private static final long serialVersionUID = -7697928469086137837L;

	public ReadingPacketException() {}
	
	public ReadingPacketException(String msg) 
	{
		super(msg);
	}
	
	public ReadingPacketException(String errorMessage, Throwable err) 
	{
        super(errorMessage, err);
    }
}
