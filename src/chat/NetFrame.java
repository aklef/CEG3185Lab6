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
 * Each {@code NetFrame} has a designated {@code HDLCFrameTypes} value.
 * 
 * @author Andréas K.LeF.
 * @author David Alleyn
 * @author Kent Hagerman
 */
public class NetFrame
{
	@SuppressWarnings("unused")
	private static final String FLAG = "01111110";
	private static final int MAX_INFO_FIELD_SIZE = 64*8;
	
	/**
	 * The HDLC frame types.
	 */
	public static enum HDLCFrameTypes {
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
	 * Placeholder {@code String}s for each {@code NetFrame} {@code HDLCFrameTypes}'s {@code ControlCode}.
	 */
	private static BiMap<HDLCFrameTypes, String> FC;
	static
    {
		FC = HashBiMap.create();
		FC.put(HDLCFrameTypes.IFrame, "0NSSPNRS");
		FC.put(HDLCFrameTypes.SFrame, "10CCPNRS");
		FC.put(HDLCFrameTypes.UFrame, "11CCPBBB");
    }
        
	private static BiMap<HDLCFrameTypes.Commands, String> CC;
    static
    {
		CC = HashBiMap.create();
		CC.put(HDLCFrameTypes.Commands.RR,   "00");
		CC.put(HDLCFrameTypes.Commands.RNR,  "01");
		CC.put(HDLCFrameTypes.Commands.REJ,  "10");
		CC.put(HDLCFrameTypes.Commands.SREJ, "11");
		
		CC.put(HDLCFrameTypes.Commands.SNRM,  "00001");
		CC.put(HDLCFrameTypes.Commands.SNRME, "11011");
		CC.put(HDLCFrameTypes.Commands.SIM,   "11000");
		CC.put(HDLCFrameTypes.Commands.DISC,  "00010");
//		CC.put(HDLCFrameTypes.Commands.RD,    "00010");
		CC.put(HDLCFrameTypes.Commands.UA,    "00110");
		CC.put(HDLCFrameTypes.Commands.RIM,   "10000");
		CC.put(HDLCFrameTypes.Commands.UI,    "00000");
		CC.put(HDLCFrameTypes.Commands.UP,    "00100");
		CC.put(HDLCFrameTypes.Commands.RSET,  "11001");
		CC.put(HDLCFrameTypes.Commands.XID,   "11101");
		CC.put(HDLCFrameTypes.Commands.FRMR,  "10001");
    }
	
	private InetAddress addr;
	/**
	 * Frame Control
	 */
	private String fc;
	private String info;
	private String infoRemainder;
	/**
	 * N(S) the sequence number of the sent frame.
	 * This is incremented for successive I-frames, modulo 8.
	 */
	private String NSS;
	/**
	 * N(R), the receive sequence number. Both I and S frames contain a receive sequence number N(R).
	 * Its value is always the first frame not received; it acknowledges that all frames with N(S)
	 * values up to N(R)-1 (modulo 8) have been received and indicates the N(S) of the next frame it expects to receive.
	 */
	private String NRS;
	
	/**
	 * The {@code ControlCode} this frame is carrying.
	 */
	protected HDLCFrameTypes.Commands cc;
	/**
	 * This {@code NetFrame}'s {@code HDLCFrameTypes}.
	 */
	protected HDLCFrameTypes type;
	
	/**
	 * Parses a binary string to create a {@code NetFrame}
	 * Its type and data are auto-detected.
	 * 
	 * @param frame encoded as binary {@code String}
	 */
	public NetFrame (String frame) throws UnknownHostException
	{
		//frame = frame.replaceAll(FLAG, "");
        
		if (frame.isEmpty())
			throw new NullPointerException("Empty frame!");
		
		this.addr = getAddrFromBinary(frame.substring(0, 32));
		this.fc = frame.substring(32, 40);
                   
		if (fc.charAt(0) == '0')
		{
			this.type = NetFrame.HDLCFrameTypes.IFrame;
		}
		else if (fc.charAt(1) == '0')
		{
			this.type = NetFrame.HDLCFrameTypes.SFrame;
		}
		else if (fc.charAt(1) == '1')
		{
			this.type = NetFrame.HDLCFrameTypes.UFrame;
		}
		
		switch (this.type)
		{
            case IFrame:
            	this.info = frame.substring(40);
            	this.NSS = fc.substring(1, 4);
            	this.NRS = fc.substring(5);
                break;

            case SFrame:
            	this.NRS = fc.substring(5);
            	this.cc = CC.inverse().get(fc.substring(2,4));
                break;
                
            case UFrame:
            	this.cc = CC.inverse().get(fc.substring(2,4)+fc.substring(5));
                break; 
		}
	}
	
	/**
	 * Used to create an {@code SFrame} from parameters.
	 */
	public NetFrame (InetAddress destAddr, HDLCFrameTypes type, HDLCFrameTypes.Commands code)
	{
		this(destAddr, type, code, "");
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
	public NetFrame (InetAddress destAddr, HDLCFrameTypes type, HDLCFrameTypes.Commands code, String info)
	{
		this.addr = destAddr;
        this.setInfo(info);
		this.setType(type, code);
	}

	//*******************************SETTER METHODS************************************//
	
	/**
	 * Sets this {@code NetFrame}'s {@code HDLCFrameTypes} and {@code ControlCode}
	 * by replacing placeholder values in {@code FC}.
	 * 
	 * @param type this {@code NetFrame}'s {@code HDLCFrameTypes} enum value
	 * @param code the {@code ControlCode} of this {@code NetFrame}
	 */
	private void setType (HDLCFrameTypes type, HDLCFrameTypes.Commands code)
	{
		this.type = type;
		this.cc = code;
		String tempfc = FC.get(type);
		
		switch (type)
		{
			case IFrame:
				break;
			
			case SFrame:
				tempfc = tempfc.replace("CC", CC.get(code).substring(0, 2));
				break;
				
			case UFrame:
				tempfc = tempfc.replace("CC", CC.get(code).substring(0, 2));
				tempfc = tempfc.replace("BBB", CC.get(code).substring(2));
				break;	
		}
		this.fc = tempfc;
		setPollFinal(false);
	}
	
	void setInfo(String info)
	{
		if (info.length() > MAX_INFO_FIELD_SIZE)
		{
			this.infoRemainder = info.substring(MAX_INFO_FIELD_SIZE+1);
			this.info = info.substring(0, MAX_INFO_FIELD_SIZE);
		}
		else
			this.info = info;
	}

	public void setPollFinal(Boolean set)
	{
		if (set)
		{
			this.fc = fc.replace("P", "1");
		}
		else
		{
			this.fc = fc.replace("P", "0");
		}
	}

	/**
	 * Sets the N(S), the sequence number of the sent frame.
	 * @param nss
	 */
	public void setNSS(int nss)
	{
		this.NSS = fixedLenthDecimalString(nss, 3);
		this.fc = fc.replace("NSS", NSS);
	}
	
	/**
	 * Sets the N(R), the receive sequence number acknowledgement from the window.
	 * @param nrs
	 */
	public void setNRS(int nrs)
	{
		this.NRS = fixedLenthDecimalString (nrs, 3);
		this.fc = fc.replace("NRS", NRS);
	}

	//*******************************GETTER METHODS************************************//
	
	InetAddress getDestinationAddress()
	{
		return this.addr;
	}
	
	public String getInfo()
	{
		return this.info;
	}
	
	/**
	 * Get {@code HDLCFrameType} for this {@code NetFrame}.
	 * 
	 * @return
	 */
	public HDLCFrameTypes getFrameType()
	{
		return type;
	}
	
	/**
	 * Get {@code ControlCode} for this {@code NetFrame}.
	 * 
	 * @return
	 */
	public HDLCFrameTypes.Commands getCC()
	{
		return this.cc;
	}
	
	String getRemaining ()
	{
		return this.infoRemainder;
	}
	
	//*******************************HELPER METHODS************************************//
	
	/**
	 * Returns a {@code String} representation of this {@code NetFrame}.
	 */
	@Override
	public String toString()
	{
		//String frame = FLAG + getAddrInBinary(addr);
        String frame = getAddrInBinary(addr) + this.fc;
		
		if (!info.equals(""))
			frame += info;
		
		//return (frame + FLAG);
        return frame;
	}
	
	protected int getReceivedFrameIndex()
	{
		return Integer.parseInt(fc.substring(5, 7));
	}
	
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
	 * @return the IPv4 address
	 * @throws UnknownHostException 
	 */
	static InetAddress getAddrFromBinary(String addr) throws UnknownHostException
	{
		String result = "", binary;
		
		for (int i = 0; i < 32; i+=8)
		{
			binary = addr.substring(i, i + 8);
                        
			result += Integer.parseInt(binary, 2);		
            result += ".";
		}
		
        result = result.substring(0, result.length()-1);
                
		return InetAddress.getByName(result);
	}
	
	/**
	 * 
	 * @param length the desired length of the formatted {@code String}.
	 * @param input that is to be formatted.
	 * @return
	 */
	static String fixedLenthDecimalString(int input, int length) {
	    return String.format("%0"+length+"d", input);
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