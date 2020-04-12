package EnvVariables;
import java.io.File;


import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

import PacketConstructor.Manifest;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketType;
import PacketTools.Checksum;
import SendTools.Sender;
import generalTools.Tools;

public class Environment 
{
	public static final String MANIFEST_FILENAME = "manifest.config";
	public static final String BUILD_PACKET_FILENAME = "build.config";
	public static final String METADATA_FILENAME = "metadata.config";
	public static final String MERGE_DATA_FILENAME = "mergeRawData.file";
	
	
	public static final String PREFIXE_SEPARATOR = "_";
	public static final String PACKET_PREFIXE = "packet"+PREFIXE_SEPARATOR;
	public static final String SUBPACKET_PREFIXE = "subpacket"+PREFIXE_SEPARATOR;
	
	public static final String PACKET_HEADER_PREFIXE = "header"+PREFIXE_SEPARATOR;
	public static final String PACKET_DATA_PREFIXE = "data"+PREFIXE_SEPARATOR;
	public static final String FOLDER_PACKET_HEADER = "Headers";
	public static final String FOLDER_PACKET_DATA = "Data";
	public static final String FOLDER_PACKET_CHECKSUM = "Checksum";
	public static final String FOLDER_FILE_PREFIXE = "file_";
	
	public static final String RAW_DATA_PREFIXE = "data_";
	public static final String RAW_METADATA_PREFIXE = "meta_";
	public static final String RECEIVED_PACKET_PREFIXE = "received_";
	
	public static final String PACKET_POOL_FILENAME = "PacketPool";
	
	public static final long PACKET_START_INDEX = 0;
	
	public static final int MAXIMUN_PACKET_SIZE = Math.max(Parms.instance().sender().getMMUPacketLength(), Manifest.BYTES);
	
	public static final String STRING_CODEX = "UTF-8";
	
	//public static int BUFFER_FILE_SIZE=8192;
	

	public enum PacketEnumType 
	{
	    DATA(0), METADATA(1);

	    private final int value;

	    PacketEnumType(int value) 
	    {
	        this.value = value;
	    }

	    public static Optional<PacketEnumType> valueOf(int value) 
	    {
	        return Arrays.stream(values())
	            .filter(var -> var.value == value)
	            .findFirst();
	    }
	    
	    public int getValue()
	    {
	    	return value;
	    }
	}
	
	
	
	public static String getPacketName(long index)
	{
		return PACKET_PREFIXE + index;
	}
	
	public static String getSubPacketFilename(long index)
	{
		return SUBPACKET_PREFIXE + index;
	}
	
	public static String getPacketReceivedFilename(String work_directory, long index)
	{
		return Tools.combinePath(work_directory, Environment.RECEIVED_PACKET_PREFIXE+index);
	}
	
	
	public static String getRawDataFilename(String work_directory, long index)
	{
		return Tools.combinePath(work_directory, Environment.RAW_DATA_PREFIXE+index);
	}
	
	public static String getRawMetadataFilename(String work_directory, long index)
	{
		return Tools.combinePath(work_directory, Environment.RAW_METADATA_PREFIXE+index);
	}
	
	public static interface RawPacketFilenameFromIndex 
	{
	    String getFilename(String work_directory, long packet_index);
	}
	
	public static String getManifestFilename(String work_directory)
	{
		return Tools.combinePath(work_directory, Environment.MANIFEST_FILENAME);
	}
	
	public static String getMetadataFilename(String work_directory)
	{
		return Tools.combinePath(work_directory, Environment.METADATA_FILENAME);
	}
	
	public static String getMergeDataFilename(String work_directory)
	{
		return Tools.combinePath(work_directory, Environment.MERGE_DATA_FILENAME);
	} 
	
	public static String getFolderFile(String work_directory, long id)
	{
		return Tools.combinePath(work_directory, Environment.FOLDER_FILE_PREFIXE+id);
	}
	
	public static long getNumberPackets(Manifest manifest, int type_id)
	{
		double length = getLength(manifest, type_id);
		
		return (long)Math.ceil(length/manifest.blockSize);
	}
	
	public static long getNumberPackets(Manifest manifest, PacketType type)
	{
		return getNumberPackets(manifest, type.getId());
	}
	
	public static long getLength(Manifest manifest, int type_id)
	{
		switch(type_id)
		{
			case PacketType.TYPE_DATA:
				return manifest.dataLength;
			case PacketType.TYPE_METADATA:
				return manifest.metaLength;
			default :
				return 0;
		}
	}
	
	public static long getLength(Manifest manifest, PacketType type)
	{
		return getLength(manifest, type.getId());
	}
	
}
