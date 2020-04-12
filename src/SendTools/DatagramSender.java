package SendTools;

import java.io.IOException;

import Exceptions.InvalidPacketStreamException;

public abstract class DatagramSender 
{
	public abstract byte[] getPacket() throws IOException, InvalidPacketStreamException;
}
