package UserInterface;

import PacketConstructor.Manifest;

public class ManifestEvent extends ReceptionEvent
{
	public final Manifest manifest;
	public final Exception exception; 
	
	
	private ManifestEvent(Manifest manifest, Exception e)
	{
		this.manifest = manifest;
		this.exception = e;
	}
	
	public ManifestEvent(Manifest manifest)
	{
		this(manifest, null);
	}
	
	public ManifestEvent(Exception e)
	{
		this(null, e);
	}
}
