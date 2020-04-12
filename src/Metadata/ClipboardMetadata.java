package Metadata;

import java.awt.datatransfer.DataFlavor;

public class ClipboardMetadata extends OnMemoryMetadata
{
	public final DataFlavor flavor;
	
	public ClipboardMetadata(DataFlavor flavor)
	{
		if(flavor==null)
			throw new IllegalArgumentException();
		
		this.flavor = flavor;
	}
}
