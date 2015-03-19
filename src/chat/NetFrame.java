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
 * Each {@code NetFrame} has a designated {@code Frames} value.
 * 
 * @author Andréas K.LeF.
 * @author David Alleyn
 * @author Kent Hagerman
 */
public class NetFrame
{
	@SuppressWarnings("unused")
	private static final String FLAG = "01111110";
	private static final int MAX_FRAME_SIZE = 64*8;
	private static int SEQUENCE_NUMBER = 0;
	
	/**
	 * The HDLC frame types.
	 */
	public static enum Frames {
		/**
		 * Information frame. Contains data that needs to be extracted.
		 * Will contain {@code NetFrame} sequence numbers
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
		UFrame;
		
		enum Commands
		{
			// (S)upervisory-frame commands
			/**
			 * Receive Ready
			 */
			RR,
			/**
			 * Receive Not Ready
			 */
			RNR,
			/**
			 * Reject 
			 */
			REJ,
			/**
			 * Selective Reject
			 */
			SREJ,
			
			// (U)nnumbered-frame commands 
			/**
			 * Set normal response mode
			 */
			SNRM,
			SNRME, SIM,
			/**
			 * Disconnect
			 */
			DISC,
			UA, RD, RIM, UI, UP, RSET, XID, FRMR;
		}
	}
		
	/**
	 * Placeholder {@code String}s for each {@code NetFrame} {@code Frames}'s {@code ControlCode}.
	 */
	private static BiMap<Frames, String> FC;
	static
    {
		FC = HashBiMap.create();
		FC.put(Frames.IFrame, "0NSPNR00");
		FC.put(Frames.SFrame, "10CCPNR0");
		FC.put(Frames.UFrame, "11CCPBBB");
    }
        
	private static BiMap<Frames.Commands, String> CC;
	
    static
    {
		CC = HashBiMap.create();
		CC.put(Frames.Commands.RR,   "00XXX");
		CC.put(Frames.Commands.RNR,  "01XXX");
		CC.put(Frames.Commands.REJ,  "10XXX");
		CC.put(Frames.Commands.SREJ, "11XXX");
		
		CC.put(Frames.Commands.SNRM,  "00001");
		CC.put(Frames.Commands.SNRME, "11011");
		CC.put(Frames.Commands.SIM,   "11000");
		CC.put(Frames.Commands.DISC,  "00010");
//		CC.put(Frames.Commands.RD,    "00010");
		CC.put(Frames.Commands.UA,    "00110");
		CC.put(Frames.Commands.RIM,   "10000");
		CC.put(Frames.Commands.UI,    "00000");
		CC.put(Frames.Commands.UP,    "00100");
		CC.put(Frames.Commands.RSET,  "11001");
		CC.put(Frames.Commands.XID,   "11101");
		CC.put(Frames.Commands.FRMR,  "10001");
    }
	
	InetAddress addr;
	/**
	 * Frame Control
	 */
	private String fc;
	private String info;
	private String infoRemainder;
	protected String sequenceNumber;
	
	/**
	 * The {@code ControlCode} this frame is carrying.
	 */
	protected Frames.Commands cc;
	/**
	 * This {@code NetFrame}'s {@code Frames}.
	 */
	protected Frames type;
	
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
	public NetFrame (InetAddress destAddr, Frames type, Frames.Commands Code)
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
	public NetFrame (InetAddress destAddr, Frames type, Frames.Commands code, String info)
	{
		this.addr = destAddr;
                this.setInfo(info);
		this.setType(type, code);
	}
	
	/**
	 * Sets this {@code NetFrame}'s {@code Frames} and {@code ControlCode}
	 * by replacing placeolder values in {@code FC}.
	 * 
	 * @param type this {@code NetFrame}'s {@code Frames} enum value
	 * @param code the {@code ControlCode} of this {@code NetFrame}
	 */
	private void setType (Frames type, Frames.Commands code)
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
		//String frame = FLAG + getAddrInBinary(addr) + CC.get(cc);
                String frame = getAddrInBinary(addr) + FC.get(this.type)  + CC.get(cc);
		
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
		//return (frame + FLAG);
                return frame;
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
		//frame = frame.replaceAll(FLAG, "");
        
		this.addr = getAddrFromBinary(frame.substring(0, 32));
		this.fc = frame.substring(32, 40);
        this.cc = CC.inverse().get(frame.substring(40, 45));
		                
		if (fc.charAt(0) == '0')
		{
			this.type = NetFrame.Frames.IFrame;
		}
		else if (fc.charAt(1) == '0')
		{
			this.type = NetFrame.Frames.SFrame;
		}
		else if (fc.charAt(1) == '1')
		{
			this.type = NetFrame.Frames.UFrame;
		}
		
		switch (this.type)
		{
			// User data frame
            case IFrame:
                    this.info = frame.substring(45, frame.length());
                    break;

            // Control frame or nnumbered frame for link management
            case SFrame: case UFrame:
                    this.info = "";
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
	
	Frames getFrameType()
	{
		return type;
	}
	
	/**
	 * Returns this {@code NetFrame}'s {@code ControlCode}.
	 * 
	 * @return
	 */
	Frames.Commands getCC()
	{
		return this.cc;
	}
	
	String getRemaining ()
	{
		return this.infoRemainder;
	}
	
	//*******************************HELPER METHODS************************************//
	
	static String getAddrInBinary(InetAddress addr)
	{
		String res = "";
		
		for (String threeDigitNum : addr.toString().substring(1).split("\\."))
		{
            res += padLeft(Integer.toBinaryString(Integer.parseInt(threeDigitNum)));
		}
                
		return res;
	}
	
    public static String padLeft(String s) {
        while (s.length() != 8)
            s = "0" + s;
        return s;
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
	static InetAddress getAddrFromBinary(String addr)
			throws IndexOutOfBoundsException, NumberFormatException, UnknownHostException
	{
		String result = "", temp;
		
		for (int i = 0; i < 32; i+=8)
		{
			temp = addr.substring(i, i + 8);
                        
			result += Integer.parseInt(temp, 2);		
            result += ".";
		}
		
        result = result.substring(0, result.length() - 1);
                
		return InetAddress.getByName(result);
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
