package PacketManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import EnvVariables.Environment;
import EnvVariables.Parms;
import PacketConstructor.Manifest;
import PacketConstructor.PacketBufferInfo;
import PacketConstructor.PacketHeader;
import PacketConstructor.PacketType;

public class PacketPoolFile 
{
	private long length;
	private RandomAccessFile raf_in;
	private DataOutputStream stream_out;
	private DataInputStream stream_in;
	private final byte[] buffer_in;
	
	public PacketPoolFile(String work_dir, Manifest manifest, int buffer_size) throws IOException
	{
		File file = new File(work_dir+File.separator+Environment.PACKET_POOL_FILENAME);
		BufferedOutputStream tmp = new BufferedOutputStream(new FileOutputStream(file, true)/*append*/, buffer_size);
		this.stream_out = new DataOutputStream(tmp);
		
		this.raf_in =  new RandomAccessFile(file, "r");
		
		int size_in = Integer.BYTES + Long.BYTES + Integer.BYTES + manifest.blockSize;
		this.buffer_in =  new byte[size_in];
		ByteArrayInputStream byte_stream_in = new ByteArrayInputStream(buffer_in);
		this.stream_in = new DataInputStream(byte_stream_in);
		this.stream_in.mark(0);
		
		this.length = file.length();
	
	}
	
	public PacketPoolFile(String work_dir, Manifest manifest) throws IOException
	{
		this(work_dir, manifest, Parms.instance().getBufferFileSize());
	}
	
	public long write(PacketHeader header, byte[] buffer, int offset_data) throws IOException
	{
		return write(header.getType(), header.getIndex(), buffer, offset_data, header.getLength());
	}
	
	public long write(PacketType type, long index, byte[] buffer, int offset, int len) throws IOException
	{
		
		long pos = length;
		stream_out.writeInt(type.getId());
		stream_out.writeLong(index);
		stream_out.writeInt(len);
		stream_out.write(buffer, offset, len);
		
		length+= Integer.BYTES + Long.BYTES + Integer.BYTES + len;
		
		return pos;
	}
	
	public long write(PacketType type, long index, byte[] buffer) throws IOException
	{
		return write(type, index, buffer, 0, buffer.length);
	}
	
	public byte[] read(long pos) throws IOException
	{
		raf_in.seek(pos);
		raf_in.read(buffer_in);
		stream_in.reset();
		
		int type_id = stream_in.readInt();
		long index = stream_in.readLong();
		int length = stream_in.readInt();
		
		byte[] buffer = new byte[length];
		stream_in.read(buffer);
		
		return buffer;
	}
	
	public long length()
	{
		return length;
	}
	
	public void close(boolean save_objects) throws IOException
	{
		if(save_objects)
			flush();
		
		stream_in.close();
		stream_out.close();
		raf_in.close();
	}
	
	public void close() throws IOException
	{
		close(true);
	}
	
	public void flush() throws IOException
	{
		stream_out.flush();
	}
}
