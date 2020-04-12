package PacketTools;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.CRC32;

public class Checksum 
{
	public static final int LENGTH = Long.BYTES; //32; //number byte
	public static final String HASH_ALGO = "CRC32"; //"SHA-256"
	
	private CRC32 msgd;
	
	
	public Checksum()
	{
		msgd = new CRC32();
	}
	
	public byte[] compute()
	{
		return longToBytes(msgd.getValue());
	}
	
	public static byte[] compute(byte[] buffer)
	{
		return compute(buffer, 0, buffer.length);
	}
	
	public static byte[] compute(byte[] buffer, int offset, int len)
	{
		CRC32 md;
		
		md = new CRC32();
		md.update(buffer, offset, len);
		byte[] checksum = longToBytes(md.getValue());
		//System.out.println("size="+checksum.length);
		return checksum;
	}
	
	
	public static String computeString(byte[] buffer)
	{
		return computeString(buffer, 0, buffer.length);
	}
	
	public static String computeString(byte[] buffer, int offset, int len)
	{
		byte[] hash = compute(buffer, offset, len);
		return hashToString(hash);
	}
	
	public static String hashToString(byte[] hash)
	{
		StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
	}
	
	public static boolean dataMatchChecksum(byte[] checksum, byte[] data)
	{
		return Arrays.equals(checksum, compute(data));
	}
	
	public static boolean dataMatchChecksum(byte[] checksum, int offset_checksum, byte[] data, int offset_data, int len_data)
	{	
		if(checksum==null && data==null)
			return true;
		
		if(checksum==null || data==null)
			return false;
		
		if(checksum.length-offset_checksum<LENGTH)//check if there is enough space for the checksum started at that offset
			return false;
		
		byte[] digest = compute(data, offset_data, len_data);
		
		for(int i=0, j=offset_checksum; i<LENGTH; i++, j++)
		{
			if(checksum[j]!=digest[i])
			{
				return false;
			}
		}
		
		
		return true;
	}
	
	public static boolean dataMatchChecksum(byte[] checksum, byte[] data, int offset, int len)
	{	
		return Arrays.equals(checksum, compute(data, offset, len));
	}
	
	public boolean dataMatchChecksum(byte[] checksum)
	{
		return Arrays.equals(checksum, longToBytes(msgd.getValue()));
	}
	
	
	private static byte[] longToBytes(long l) 
	{
	    byte[] result = new byte[8];
	    for (int i = 7; i >= 0; i--) {
	        result[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
	    return result;
	}
}

/*
 public class Checksum 
{
	public static final int LENGTH = 16; //32; //number byte
	public static final String HASH_ALGO = "MD5"; //"SHA-256"
	
	private MessageDigest msgd;
	
	
	public Checksum()
	{
		try 
		{
			msgd = MessageDigest.getInstance(HASH_ALGO);
			
		}catch (NoSuchAlgorithmException e) 
		{
			msgd = null;
		} 
	}
	
	public void addData(String filename, long size) throws IOException
	{
		FileInputStream fis = new FileInputStream(new File(filename));
		BufferedInputStream stream = new BufferedInputStream(fis);
		
		byte b;
		int val;
		long count = 0;
		
		
		val = stream.read();
		b = (byte)val;
		while(val!=-1 && count<size)
		{
			msgd.update(b);
			
			val = stream.read();
			b = (byte)val;
			
			count++;
		}
		
		stream.close();
	}
	
	
	public void addData(byte[] buffer, int offset, int len)
	{
		msgd.update(buffer, offset, len);
	}
	
	public void addData(byte[] buffer)
	{
		addData(buffer, 0, buffer.length);
	}
	
	public byte[] compute()
	{
		return msgd.digest();
	}
	
	public static byte[] compute(byte[] buffer)
	{
		return compute(buffer, 0, buffer.length);
	}
	
	public static byte[] compute(byte[] buffer, int offset, int len)
	{
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(HASH_ALGO);
			md.update(buffer, offset, len);
			byte[] checksum = md.digest();
			//System.out.println("size="+checksum.length);
			return checksum;
			
		} catch (NoSuchAlgorithmException e) 
		{
			e.printStackTrace();
		} 
		
		return null;
	}
	
	
	public static String computeString(byte[] buffer)
	{
		return computeString(buffer, 0, buffer.length);
	}
	
	public static String computeString(byte[] buffer, int offset, int len)
	{
		byte[] hash = compute(buffer, offset, len);
		return hashToString(hash);
	}
	
	public static String hashToString(byte[] hash)
	{
		StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
	}
	
	public static boolean dataMatchChecksum(byte[] checksum, byte[] data)
	{
		return Arrays.equals(checksum, compute(data));
	}
	
	public static boolean dataMatchChecksum(byte[] checksum, int offset_checksum, byte[] data, int offset_data, int len_data)
	{	
		if(checksum==null && data==null)
			return true;
		
		if(checksum==null || data==null)
			return false;
		
		if(checksum.length-offset_checksum<LENGTH)//check if there is enough space for the checksum started at that offset
			return false;
		
		byte[] digest = compute(data, offset_data, len_data);
		
		for(int i=0, j=offset_checksum; i<LENGTH; i++, j++)
		{
			if(checksum[j]!=digest[i])
			{
				return false;
			}
		}
		
		
		return true;
	}
	
	public static boolean dataMatchChecksum(byte[] checksum, byte[] data, int offset, int len)
	{	
		return Arrays.equals(checksum, compute(data, offset, len));
	}
	
	public boolean dataMatchChecksum(byte[] checksum)
	{
		return Arrays.equals(checksum, msgd.digest());
	}
}
*/
