package PacketTools;

import PacketConstructor.PacketType;

public class Range //implements Comparable
{
	public final long start;
	public final long end;
	public final PacketType type;
	
	
	public Range(long start, long end, PacketType type)
	{
		this.start = start;
		this.end = end;
		this.type = type; 
	}
	
	public Range(long single_point, PacketType type)
	{
		this(single_point, single_point, type);
	}
	
	public Range(long single_point)
	{
		this(single_point, null);
	}
	
	public Range(long start, long end)
	{
		this(start, end, null);
	}
	
	public String toString()
	{
		return "Range(start="+start+", end="+end+", type="+type+")";
	}
	
	
	public boolean equals(Object o)
	{
		if(o==null)
			return false;
		if(o==this)
			return true;
		if(o.getClass()!=this.getClass())
			return false;
		Range r = (Range)o;
		return (r.start==this.start && r.end==this.end);		
	}
	/*public int compareTo(Object o) 
	{
		Range compareTo = (Range)o;
		
		if(compareTo==this)
			return 0;
		
		if(compareTo.start == this.start)
			return 0;
		
		if(compareTo.start>this.start)
			return -1;
		else
			return 1;

    }*/
}