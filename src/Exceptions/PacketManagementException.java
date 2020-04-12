package Exceptions;

public class PacketManagementException extends Exception
{
	public PacketManagementException(String errorMessage, Throwable err) 
	{
        super(errorMessage, err);
    }
	
	public PacketManagementException(Throwable err) 
	{
        this("PacketManagmentException", err);
    }
	
	public PacketManagementException() {}
}
