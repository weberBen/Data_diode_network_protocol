package Metadata;

import java.io.IOException;

import GeneralTools.Tools;

public interface ContentActionInterface 
{
	public Class metadataClass();
	
	public boolean isInMemoryContent();
	
	public String actionOn(byte[] metadata, Object data) throws Exception;
	
	public default String actionOn(String metadata_filename, Object data) throws Exception
	{
		return actionOn(Tools.getBytesFromFile(metadata_filename), data);
	}
}
