package Metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;

import EnvVariables.Environment;
import EnvVariables.Parms;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

public final class DataType implements Serializable
{
	
	private static final long serialVersionUID = 418949576448069176L;
	
	private int id;
	private String label;
	private ContentActionInterface itf;
	
	private DataType(int id, String label, ContentActionInterface itf)
	{
		if(itf!=null && itf.metadataClass()!=null && !Metadata.class.isAssignableFrom(itf.metadataClass())) 
		{//class does not extends Metadata class
			throw new IllegalArgumentException();
	    }
		
		this.id = id;
		this.label = label;
		this.itf = itf;
	}
	
	
	private DataType(int id, String label)
	{
		this(id, label, null);
		
	}
	
	public String toString()
	{
		String name="";
		if(label!=null)
			name = label.toLowerCase();
		
		return "DataType(id="+id+", label="+name+")";
	}
    
    public boolean metadataClassEqual(Metadata metadata)
    {
    	if(itf==null && metadata!=null)
    		return false;
    	if(metadata==null && itf.metadataClass()==null)
    		return true;
    	if(metadata==null)
    		return false;
    	
    	return metadata.getClass().equals(itf.metadataClass());
    	
    }
	
    public boolean equals(Object o)
    {
    	if(o==null)
    		return false;
    	if(o==this)
    		return true;
    	if(o.getClass()!=this.getClass())
    		return false;
    	
    	DataType dt = (DataType)o;
    	
    	if(dt.id!=id)
    		return false;
    	
    	return true;
    }
	
    public boolean isInMemory()
    {	
    	return itf.isInMemoryContent();
    }
    
	public static DataType getType(int id)
	{
		return MAP.get(id);
	}
	
	public boolean hasMetadata()
	{
		if(itf==null)
			return false;
		
		return (itf.metadataClass()!=null);
	}
	
	public Class getMetadataClass()
	{
		if(itf==null)
			return null;
		
		return itf.metadataClass();
	}
	
	public int getId()
	{
		return id;
	}
	
	public ContentActionInterface getAction()
	{
		return itf;
	}
	
	
	
	/******************************************************************************
	 ******************************************************************************
	 ******************************************************************************
	 ******************************************************************************
	 ******************************************************************************
	 */
	
	private static final ContentActionInterface ITF_CLIPBOARD = new ContentActionInterface()
	{
		public Class metadataClass()
		{
			return ClipboardMetadata.class;
		}
		
		public boolean isInMemoryContent()
		{
			return true;
		}
		
		public String actionOn(byte[] b_metadata, Object data) throws IllegalArgumentException, ClassCastException, UnsupportedOperationException, IOException
		{
			if(b_metadata==null || data==null)
				throw new IllegalArgumentException();
			byte[] b_data = (byte[])data;
			
			ClipboardMetadata metadata = (ClipboardMetadata)Metadata.getMetadata(b_metadata);
			if(metadata==null)
				throw new IllegalArgumentException();
			if(metadata.flavor==null)
				throw new IllegalArgumentException();
			
			Transferable transferable;
			if(metadata.flavor.equals(DataFlavor.imageFlavor))
			{
				ByteArrayInputStream bais = new ByteArrayInputStream(b_data);
				BufferedImage bufferedImage = (BufferedImage)ImageIO.read(bais);
				
				transferable = new ImageTransferable(bufferedImage);
				
			}else if(metadata.flavor.equals(DataFlavor.stringFlavor))
			{
				String string = new String(b_data);
				
				transferable = new StringSelection(string);
				
			}else
			{
				throw new UnsupportedOperationException();
			}
			
			
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(transferable, null);
			
			return metadata.flavor.toString();
		}
		
		
		class ImageTransferable implements Transferable
		{
		    private Image image;

		    public ImageTransferable (Image image)
		    {
		        this.image = image;
		    }

		    public Object getTransferData(DataFlavor flavor)
		        throws UnsupportedFlavorException
		    {
		        if (isDataFlavorSupported(flavor))
		        {
		            return image;
		        }
		        else
		        {
		            throw new UnsupportedFlavorException(flavor);
		        }
		    }

		    public boolean isDataFlavorSupported (DataFlavor flavor)
		    {
		        return flavor == DataFlavor.imageFlavor;
		    }

		    public DataFlavor[] getTransferDataFlavors ()
		    {
		        return new DataFlavor[] { DataFlavor.imageFlavor };
		    }
		}
	};
	
	private static final ContentActionInterface ITF_FILE = new ContentActionInterface()
	{
		public Class metadataClass()
		{
			return FileMetadata.class;
		}
		
		public boolean isInMemoryContent()
		{
			return false;
		}
		
		public String actionOn(byte[] b_metadata, Object data) throws IllegalArgumentException, ClassCastException, SecurityException, IOException
		{
			if(b_metadata==null || data==null)
				throw new IllegalArgumentException();
			String filename = (String)data;
			
			FileMetadata metadata = (FileMetadata)Metadata.getMetadata(b_metadata);
			if(metadata==null)
				throw new IllegalArgumentException();
			
			File src = new File(filename);
			String path = Parms.instance().receiver().getOutPath()+ java.io.File.separator + metadata.name;
			src.renameTo(new File(path));
			
			return path;
		}
	};
	
	
	private static final ContentActionInterface ITF_PLAIN_TXT = new ContentActionInterface()
	{
		public Class metadataClass()
		{
			return null;
		}
		
		public boolean isInMemoryContent()
		{
			return true;
		}
		
		public String actionOn(byte[] b_metadata, Object data) throws IllegalArgumentException, ClassCastException, SecurityException, IOException
		{
			if(data==null)
				throw new IllegalArgumentException();
			
			byte[] b_data = (byte[])data;
			
			return new String(b_data, Environment.STRING_CODEX);
		}
	};
	
	/******************************************************************************
	 ******************************************************************************
	 ******************************************************************************
	 ******************************************************************************
	 ******************************************************************************
	 */
	
	public static final DataType Unspecified = new DataType(-1, "Unspecified", null);
	public static final DataType File = new DataType(1, "File", ITF_FILE);
	public static final DataType Clipboard = new DataType(2, "Clipboard", ITF_CLIPBOARD);
	public static final DataType PacketsRecovery = new DataType(3, "PacketsRecovery", null);
	public static final DataType PlainTxt = new DataType(4, "PlainTxt", ITF_PLAIN_TXT);
	
	public static final HashMap<Integer, DataType> MAP = new HashMap<Integer, DataType>() 
	{{ put(File.id, File); put(Clipboard.id, Clipboard); put(PacketsRecovery.id, PacketsRecovery); }};
}

