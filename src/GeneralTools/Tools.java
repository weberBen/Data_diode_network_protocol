package GeneralTools;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import EnvVariables.Environment;
import PacketConstructor.Manifest;
import PacketConstructor.PacketHeader;

public class Tools 
{
	public static byte[] getBytesFromFile(String filename) throws IOException 
	{
		File file = new File(filename);
		return getBytesFromFile(file);
	}
	
	public static void sleep(long interval_nano)
	{
	    long start = System.nanoTime();
	    long end=0;
	    do
	    {
	        end = System.nanoTime();
	    }while(start + interval_nano >= end);
	}
	
	public static byte[] getBytesFromFile(File file) throws IOException 
	{        
        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
            throw new IOException("File is too large!");
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;

        InputStream is = new FileInputStream(file);
        try {
            while (offset < bytes.length
                   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
        } finally {
            is.close();
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
        return bytes;
    }
	
	public static String combinePath(String path1, String path2)
	{
		return String.join(File.separator, path1, path2);
	}
	
	public static long getNumberPacketsData(Manifest manifest)
	{
		if(manifest==null)
			return 0;
		 
		return (long)Math.ceil((double)manifest.dataLength/(double)manifest.blockSize);
		
	}
	
	public static long getNumberPacketsMetadata(Manifest manifest)
	{
		if(manifest==null)
			return 0;
		
		return (long)Math.ceil((double)manifest.metaLength/(double)manifest.blockSize);
	}
	
	public static long getTotalNumberPackets(Manifest manifest)
	{
		return getNumberPacketsMetadata(manifest) + getNumberPacketsData(manifest);
	}
	
	public static void writeByteArrayToFile(String filename, byte[] data) throws IOException
	{
		writeByteArrayToFile(filename, data, 0, data.length);
	}
	
	public static void writeByteArrayToFile(String filename, byte[] data, int offset, int length) throws IOException
	{
		try (FileOutputStream fos = new FileOutputStream(filename)) 
		{
			   fos.write(data, offset, length);
		}
	}
	
	public static void setFileLength(String filename, long size) throws IOException
	{
		RandomAccessFile file = new RandomAccessFile(filename, "rw");
		file.seek(0);
		file.setLength(size);
		file.close();
	}
	
	public static void truncateBinaryFile(String filename, long start_bytes_position, long count_bytes) throws IOException
	{
		//https://stackoverflow.com/questions/32786629/what-is-the-best-way-of-deleting-a-section-of-a-binary-file-in-java-7
	    // prepare the paths
	    Path inPath = Paths.get(filename); // java.nio.file.Paths
	    Path outPath; // java.nio.file.Path
	    outPath = Files.createTempFile(null, "swp"); // java.nio.file.Files

	    // process the file
    	FileChannel readChannel = FileChannel.open(inPath, StandardOpenOption.READ);
        FileChannel writeChannel = FileChannel.open(outPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        
        readChannel.transferTo(start_bytes_position, count_bytes, writeChannel);
	    
	    // replace the original file with the temporary file
	    try 
	    {
	        // ATOMIC_MOVE may cause IOException here . . .
	        Files.move(outPath, inPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	    } catch (IOException e1) 
	    {
            // . . . so it's probably worth trying again without that option
        	Files.move(outPath, inPath, StandardCopyOption.REPLACE_EXISTING);
	    }
	}
	
	public static void mergeBinaryFile(String top_filename, String below_filename) throws IOException
	{
		File f = new File(below_filename);
		mergeBinaryFile(top_filename, below_filename, 0, f.length());
	}
	
	public static void mergeBinaryFile(String top_filename, String below_filename, long start_bytes_position, long count_bytes) throws IOException
	{
		//https://stackoverflow.com/questions/32786629/what-is-the-best-way-of-deleting-a-section-of-a-binary-file-in-java-7
	    // prepare the paths
	    Path topPath = Paths.get(top_filename); // java.nio.file.Paths
	    Path belowPath = Paths.get(below_filename); // java.nio.file.Paths
	    
	    // process the file
    	FileChannel readChannel = FileChannel.open(belowPath, StandardOpenOption.READ);
        FileChannel writeChannel = FileChannel.open(topPath, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        
        readChannel.transferTo(start_bytes_position, count_bytes, writeChannel);
	    
	    // replace the original file with the temporary file
	    Files.delete(belowPath);
	}
	
	public static int getNumberDatagramThreads(long total_length_stream, int block_size)
	{
		
		return 1;
	}
	
	public static void purgeDirectory(File dir) 
	{
		if(!dir.exists())
			return ;
		
	    for (File file: dir.listFiles()) 
	    {
	        if (file.isDirectory())
	            purgeDirectory(file);
	        
	        file.delete();
	    }
	}
	
	public static void purgeDirectory(String dir) 
	{
		File file = new File(dir);
		if(!file.exists() || !file.isDirectory())
			return ;
		
		purgeDirectory(file);
	}
	
	public static boolean isCorrectIpPort(int port)
	{
		return port>0 && port<65535;
	}
	
	public static BufferedImage getBufferedImage(Image img) {
	    if (img == null) {
	        return null;
	    }
	    int w = img.getWidth(null);
	    int h = img.getHeight(null);
	    // draw original image to thumbnail image object and 
	    // scale it to the new size on-the-fly 
	    BufferedImage bufimg = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
	    Graphics2D g2 = bufimg.createGraphics();
	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    g2.drawImage(img, 0, 0, w, h, null);
	    g2.dispose();
	    return bufimg;
	}
}
