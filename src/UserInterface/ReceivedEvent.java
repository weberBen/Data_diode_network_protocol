package UserInterface;

import PacketConstructor.Manifest;

public class ReceivedEvent extends ReceptionEvent
{
	public final Manifest manifest;
	public final String info;
	public final Exception exception;
	
	private ReceivedEvent(Manifest manifest, String info, Exception e)
	{
		this.manifest = manifest;
		this.info = info;
		this.exception = e;
	}
	
	public ReceivedEvent(Manifest manifest, String info)
	{
		this(manifest, info, null);
	}
	
	public ReceivedEvent(Manifest manifest, Exception e)
	{
		this(manifest, null, e);
	}
	
	public ReceivedEvent(Exception e)
	{
		this(null, e);
	}
}
