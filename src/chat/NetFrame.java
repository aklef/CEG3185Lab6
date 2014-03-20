package chat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Represents a single link-layer HDLC frame.
 * Each {@code NetFrame} has a designated {@code Type} {@code enum} value.
 * 
 * @author Andréas K.LeF.
 */
public class NetFrame
{
	private static final String FLAG = "01111110";
	private static final int MAX_FRAME_SIZE = 64*8;
	private static int SEQUENCE_NUMBER = 0;
	
	public static enum Type {
		/**
		 * User data frame. Contains actual user data that needs
		 * to be extracted. Contains {@code NetFrame} sequence numbers
		 */
		IFrame, 
		/**
		 * Control frame. Contains a {@code ControlCode}
		 * for {@code NetFrame} management but to user info.
		 */
		SFrame, 
		/**
		 * Unnumbered frame for link management. 
		 * Might be unused in this context.
		 */
		UFrame
		};
		
	/**
	 * Placeholder {@code String}s for each {@code NetFrame} {@code Type}'s {@code ControlCode}.
	 */
	private static BiMap<Type, String> FC;
	static
    {
		FC = HashBiMap.create();
		FC.put(Type.IFrame, "0NSPNR");
		FC.put(Type.SFrame, "10CCPNR");
		FC.put(Type.UFrame, "11CCPBBB");
    }
	public static enum ControlCode
	{
		RR, RNR, REJ, SREJ, 
		SNRM, SNRME, SIM, DISC, UA, RD, RIM, UI, UP, RSET, XID, FRMR;
	}
	private static BiMap<ControlCode, String> CC;
	static
    {
		CC = HashBiMap.create();
		CC.put(ControlCode.RR, "00");
		CC.put(ControlCode.RNR, "01");
		CC.put(ControlCode.REJ, "10");
		CC.put(ControlCode.SREJ, "11");
		
		CC.put(ControlCode.SNRM,  "00001");
		CC.put(ControlCode.SNRME, "11011");
		CC.put(ControlCode.SIM,   "11000");
		CC.put(ControlCode.DISC,  "00010");
		CC.put(ControlCode.RD,    "00010");
		CC.put(ControlCode.UA,    "00110");
		CC.put(ControlCode.RIM,   "10000");
		CC.put(ControlCode.UI,    "00000");
		CC.put(ControlCode.UP,    "00100");
		CC.put(ControlCode.RSET,  "11001");
		CC.put(ControlCode.XID,   "11101");
		CC.put(ControlCode.FRMR,  "10001");
    }
	
	InetAddress addr;
	private String fc, info, infoRemainder;
	protected String sequenceNumber;
	
	/**
	 * The {@code ControlCode} this frame is carrying.
	 */
	protected ControlCode cc;
	/**
	 * This {@code NetFrame}'s {@code Type}.
	 */
	protected Type type;
	
	/**
	 * Parses a binary string to create a {@code NetFrame}
	 * Its type and data are auto-detected.
	 * 
	 * @param frame encoded as binary {@code String}
	 * @throws UnknownHostException 
	 * @throws IndexOutOfBoundsException 
	 * @throws NumberFormatException
	 */
	public NetFrame (String frame)
			throws NumberFormatException, IndexOutOfBoundsException, UnknownHostException
	{
		this.fromString(frame);
	}
	
	/**
	 * Used to create an {@code SFrame} from parameters.
	 */
	public NetFrame (InetAddress destAddr, Type type, ControlCode Code)
	{
		this(destAddr, type, Code, "");
	}
	
	/**
	 * Used to create an {@code IFrame} from parameters.
	 * 
	 * @param addr of the client in TCP/IP.
	 * @param type of {@code NetFrame} as enum value.
	 * @param code the {@code ControlCode} of this {@code NetFrame}
	 * @param info client data.
	 * @throws UnknownHostException 
	 */
	public NetFrame (InetAddress destAddr, Type type, ControlCode code, String info)
	{
		this.addr = destAddr;
		this.setInfo(info);
		this.setType(type, code);
	}
	
	/**
	 * Sets this {@code NetFrame}'s {@code Type} and {@code ControlCode}
	 * by replacing placeolder values in {@code FC}.
	 * 
	 * @param type this {@code NetFrame}'s {@code Type} enum value
	 * @param code the {@code ControlCode} of this {@code NetFrame}
	 */
	private void setType (Type type, ControlCode code)
	{
		this.type = type;
		this.cc = code;
		String tempfc = FC.get(type);
		
		switch (type) {
			// User data frame
			case IFrame:
				sequenceNumber = getSequenceNumber();
				tempfc.replace("NS", sequenceNumber);
				break;
			
			// Control frame
			case SFrame:
				tempfc.replace("CC", CC.get(code));
				break;
				
			// Unnumbered frame for link management
			case UFrame:
				break;
		}
		
		this.fc = tempfc;
	}
	
	void setInfo(String info)
	{
		if (info.length() > MAX_FRAME_SIZE)
		{
			this.infoRemainder = info.substring(MAX_FRAME_SIZE+1);
		}

		this.info = info;
	}
	

	public void setPollFinal(Boolean isTrue)
	{
		if (isTrue)
		{
			fc.replace("P", "1");
		}
		else
		{
			fc.replace("P", "0");
		}
	}
	
	/**
	 * Returns a {@code String} representation of this {@code NetFrame}.
	 */
	@Override
	public String toString()
	{
		String frame = FLAG + addr + cc;
		
		switch (this.type) {
			case IFrame:
				frame += info;
				break;
			
			case SFrame:
				break;
				
			case UFrame:
				frame += info;
				break;
		}
		return (frame + FLAG);
	}
	
	protected int getReceivedFrameIndex()
	{
		return Integer.parseInt(fc.substring(5, 7));
	}
	
	private static String getSequenceNumber()
	{
		return Integer.toBinaryString(SEQUENCE_NUMBER++ % 8);
	}
	
	/**
	 * Takes a string as input from which to create this frame.
	 * 
	 * @param frame {@code String} representation of this frame.
	 * @throws UnknownHostException 
	 * @throws IndexOutOfBoundsException 
	 * @throws NumberFormatException 
	 */
	void fromString(String frame)
			throws NumberFormatException, IndexOutOfBoundsException, UnknownHostException
	{
		frame.replaceAll(FLAG, "");
		this.addr = getAddrFromBinary(frame.substring(0, 4*8));	
		this.fc = frame.substring(4*8, 5*8);
		
		if (fc.charAt(0) == '0')
		{
			this.type = NetFrame.Type.IFrame;
		}
		else if (fc.charAt(1) == '0')
		{
			this.type = NetFrame.Type.SFrame;
		}
		else
		{
			this.type = NetFrame.Type.UFrame;
		}
		
		switch (this.type) {
		// User data frame
		case IFrame:
			this.cc = null;
			try
			{
				this.info = frame.substring(5*8, frame.length() - 1);
			}
			catch (Exception e)
			{
				this.info = "";
			}
			break;
		
		// Control frame
		case SFrame:
			this.cc = CC.inverse().get(fc.substring(2, 4));
			this.info = null;
			break;
			
		// Unnumbered frame for link management
		case UFrame:
			this.cc = CC.inverse().get(fc.substring(2, 4) + fc.substring(2, 8));
			break;
		}
		
	}

	//*******************************GETTER METHODS************************************//
	
	InetAddress getDestinationAddress()
	{
		return this.addr;
	}
	
	String getInfo()
	{
		return this.info;
	}
	
	Type getFrameType()
	{
		return type;
	}
	
	/**
	 * Returns this {@code NetFrame}'s {@code ControlCode}.
	 * 
	 * @return
	 */
	ControlCode getCC()
	{
		return this.cc;
	}
	
	String getRemaining ()
	{
		return this.infoRemainder;
	}
	
	//*******************************HELPER METHODS************************************//
	
	static String getAddrInBinary(Object Addr)
	{
		String res = "";
		
		for (String threeDigitNum : Addr.toString().split("."))
		{
			res += Integer.toBinaryString(Integer.parseInt(threeDigitNum));
		}
		return res;
	}
	
	/**
	 * Assuming the input is a string 4*8 chars in length,
	 * this method will convert each block of 8 chars into
	 * a decimal number to form an IP address.
	 * @param Addr the {@code String} representing an IP in binary
	 * @return the IP.
	 * @throws IndexOutOfBoundsException
	 * @throws NumberFormatException
	 * @throws UnknownHostException
	 */
	static InetAddress getAddrFromBinary(Object Addr)
			throws IndexOutOfBoundsException, NumberFormatException, UnknownHostException
	{
		String addr = "", binaryNum = Addr.toString();
		
		for (int i = 0; i < 4; i++)
		{
			binaryNum = binaryNum.substring(i*8, i*8 + 7);
			addr += Integer.parseInt(binaryNum);
			
			if (i != 3)
				addr += ".";
		}
		
		return InetAddress.getByName(addr);
	}
	
	/**
	 * 
	 * @param length the desired length of the formatted {@code String}.
	 * @param input that is to be formatted.
	 * @return
	 */
	static String fixedLenthDecimalString(Object input, int length) {
	    return String.format("%1$"+length+ "d", input);
	}
	
	static String calculateCrc(BitSet input)
	{
		// get bytes from string
		byte bytes[] = toByteArray(input);
		 
		Checksum checksum = new CRC32();
		
		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);
		 
		// get the current checksum value
		String crc = checksum.toString();
		 
		return crc;
	}
	
	static byte[] toByteArray(BitSet bits)
	{
		byte[] bytes = new byte[bits.length() / 8 + 1];
		
		for (int i = 0; i < bits.length(); i++)
		{
			if (bits.get(i))
			{
				bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
			}
		}
		return bytes;
	}
	
	static BitSet fromByteArray(byte[] bytes)
	{
		BitSet bits = new BitSet();
		for (int i = 0; i < bytes.length * 8; i++)
		{
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0)
			{
				bits.set(i);
			}
		}
		return bits;
	}
}