package PacketConstructor;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PacketType
{
	private static final long serialVersionUID = 12345412L;
	
	public static final int TYPE_UNSPECIFIED = -1;
	public static final int TYPE_MANIFEST = 0;
	public static final int TYPE_METADATA = 1;
	public static final int TYPE_DATA = 2;
	

	
	private int type;
	
	private PacketType(int type)
	{
		this.type = type;
	}
	
	public static PacketType getType(int id) throws IllegalArgumentException
	{
		switch(id)
		{
			case TYPE_UNSPECIFIED:
				return new PacketType(id);
			case TYPE_DATA:
				return new PacketType(id);
			case TYPE_METADATA:
				return new PacketType(id);
			case TYPE_MANIFEST:
				return new PacketType(id);
			default:
				throw new IllegalArgumentException();
		}
	}
    
    public int getId()
    {
    	return type;
    }
    
    public boolean equals(Object o)
    {
    	if(o==null)
    		return false;
    	if(o==this)
    		return true;
    	if(o.getClass()!=getClass())
    		return false;
    	
    	PacketType pType = (PacketType)o;
    	
    	return pType.type== type;
    }
    
    public String toString()
    {
    	String label;
    	switch(type)
		{
		    case TYPE_UNSPECIFIED:
		    	label = "TYPE_UNSPECIFIED";
		    	break;
			case TYPE_DATA:
				label = "TYPE_DATA";
				break;
			case TYPE_METADATA:
				label = "TYPE_METADATA";
				break;
			case TYPE_MANIFEST:
				label = "TYPE_MANIFEST";
				break;
			default:
				label = "???";
		}
		
    	return "(id="+type+", label="+label+")";
    }
    
    
    /* ---------------------------------------------------------------------------
     * 
     * 									DATA
     * ---------------------------------------------------------------------------
     */
	public static PacketType Data()
	{
		return new PacketType(TYPE_DATA);
	}
	
	public static boolean isData(PacketType p)
	{
		return p.type==TYPE_DATA;
	}
	
	public boolean isData()
	{
		return isData(this);
	}
	
	
	  /* ---------------------------------------------------------------------------
     * 
     * 									METADATA
     * ---------------------------------------------------------------------------
     */
	public static PacketType Metadata()
	{
		return new PacketType(TYPE_METADATA);
	}
	
	public static boolean isMetadata(PacketType p)
	{
		return p.type==TYPE_METADATA;
	}
	
	public boolean isMetadata()
	{
		return isMetadata(this);
	}
	
	
	  /* ---------------------------------------------------------------------------
     * 
     * 									UNSPECIFIED
     * ---------------------------------------------------------------------------
     */
	public static PacketType Unspecified()
	{
		return new PacketType(TYPE_UNSPECIFIED);
	}
	
	public static boolean isUnspecified(PacketType p)
	{
		return p.type==TYPE_UNSPECIFIED;
	}
	
	public boolean isUnspecified()
	{
		return isUnspecified(this);
	}
	
	
	 /* ---------------------------------------------------------------------------
     * 
     * 									MANIFEST
     * ---------------------------------------------------------------------------
     */
	public static PacketType Manifest()
	{
		return new PacketType(TYPE_MANIFEST);
	}
	
	public static boolean isManifest(PacketType p)
	{
		return p.type==TYPE_MANIFEST;
	}
	
	public boolean isManifest()
	{
		return isManifest(this);
	}
	

}

