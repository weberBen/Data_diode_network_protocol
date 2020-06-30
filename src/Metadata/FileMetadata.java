package Metadata;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import GeneralTools.Serialization;
import GeneralTools.StreamData;
import GeneralTools.Tools;
import SendTools.RandomAccessByteArrayInputStream;

public class FileMetadata extends OnDiskMetadata
{
	private static final long serialVersionUID = 1234L;
	
	public static final int ID = 1;
	
	public final String name;
	
	public FileMetadata(File file)
	{
		this.name = file.getName();
	}
	
	public FileMetadata(String file)
	{
		this.name = file;
	}
}
