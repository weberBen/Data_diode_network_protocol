package SendTools;

import java.io.IOException;
import java.io.InputStream;

public abstract class RandomAccessInputStream extends InputStream
{
	public abstract void seek(long pos) throws IOException;
	public abstract int read(byte[] buffer) throws IOException;
	public abstract int read(byte[] buffer, int offset, int len) throws IOException;
	public abstract void close() throws IOException;
}
