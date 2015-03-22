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
 * Each {@code NetFrame} has a designated {@code HDLCFrame} value.
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
	public static enum HDLCFrame
	{
		Poll, Final;
		
		enum Types
		{
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
		}
		
		enum Commands
		{
			// (S)upervisory-frame commands
			/**
			 * Receive Ready
			 * Used as positve acknowledgement (thru N(r)-1)
			 * when no I-frame is available for piggy backing.
			 * Primary can issue a POLL by setting P-bit.
			 * Secondary response with F-bit set if it has no data to send.
			 */
			RR,
			/**
			 * Receive Not Ready
			 * Used as positive acknowledgement (thru N(r)-1) and a request
			 * that no more I-frames be sent until a subsequent RR is used.
			 * 
			 * Primary station can set P-bit to solicit the receive status
			 * of a secondary/combined station.
			 * 
			 * Secondary station response to Poll with F-bit set if the station is busy.
			 */
			RNR,
			/**
			 * Reject.
			 * Go-Back-N technique (Retransmit from N(r))
			 */
			REJ,
			/**
			 * Selective Reject.
			 * Selective Repeat technique (Repeat  N(r))
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
	 * Placeholder {@code String}s for each {@code NetFrame} {@code HDLCFrame}'s {@code ControlCode}.
	 */
	private static BiMap<HDLCFrame.Types, String> FC;
	static
    {
		FC = HashBiMap.create();
		FC.put(HDLCFrame.Types.IFrame, "0NSSPNRS");
		FC.put(HDLCFrame.Types.SFrame, "10CCPNRS");
		FC.put(HDLCFrame.Types.UFrame, "11CCPBBB");
    }
        
	private static BiMap<HDLCFrame.Commands, String> CC;
    static
    {
		CC = HashBiMap.create();
		CC.put(HDLCFrame.Commands.RR,   "00");
		CC.put(HDLCFrame.Commands.RNR,  "01");
		CC.put(HDLCFrame.Commands.REJ,  "10");
		CC.put(HDLCFrame.Commands.SREJ, "11");
		
		CC.put(HDLCFrame.Commands.SNRM,  "00001");
		CC.put(HDLCFrame.Commands.SNRME, "11011");
		CC.put(HDLCFrame.Commands.SIM,   "11000");
		CC.put(HDLCFrame.Commands.DISC,  "00010");
//		CC.put(HDLCFrame.Commands.RD,    "00010");
		CC.put(HDLCFrame.Commands.UA,    "00110");
		CC.put(HDLCFrame.Commands.RIM,   "10000");
		CC.put(HDLCFrame.Commands.UI,    "00000");
		CC.put(HDLCFrame.Commands.UP,    "00100");
		CC.put(HDLCFrame.Commands.RSET,  "11001");
		CC.put(HDLCFrame.Commands.XID,   "11101");
		CC.put(HDLCFrame.Commands.FRMR,  "10001");
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
	 * Poll/Final value.
	 */
	private HDLCFrame pf;
	/**
	 * The {@code ControlCode} this frame is carrying.
	 */
	protected HDLCFrame.Commands cc;
	/**
	 * This {@code NetFrame}'s {@code HDLCFrame}.
	 */
	protected HDLCFrame.Types type;
	
	/**
	 * Parses a binary string to create a {@code NetFrame}
	 * Its type and data are auto-detected.
	 * 
	 * @param fromString encoded as binary {@code String}
	 */
	public NetFrame (String fromString) throws UnknownHostException, NullPointerException
	{
		//frame = frame.replaceAll(FLAG, "");
        
		if (fromString.isEmpty())
			throw new NullPointerException("Empty frame!");
		
		this.addr = getAddrFromBinary(fromString.substring(0, 32));
		this.fc = fromString.substring(32, 40);
                   
		if (fc.charAt(0) == '0')
		{
			this.type = HDLCFrame.Types.IFrame;
		}
		else if (fc.charAt(1) == '0')
		{
			this.type = HDLCFrame.Types.SFrame;
		}
		else if (fc.charAt(1) == '1')
		{
			this.type = HDLCFrame.Types.UFrame;
		}
		
		switch (this.type)
		{
            case IFrame:
            	this.info = fromString.substring(40);
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
		
		this.pf = (fc.charAt(4) == '1')? HDLCFrame.Poll : HDLCFrame.Final;
	}
	
	/**
	 * Used to create an {@code SFrame} from parameters.
	 */
	public NetFrame (InetAddress destAddr, HDLCFrame.Types type, HDLCFrame.Commands code)
	{
		this.addr = destAddr;
		this.setType(type, code);
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
	public NetFrame (InetAddress destAddr, HDLCFrame.Types type, String info)
	{
		this.addr = destAddr;
        this.setInfo(info);
		this.setType(type, null);
	}

	//*******************************SETTER METHODS************************************//
	
	/**
	 * Sets this {@code NetFrame}'s {@code HDLCFrame} and {@code ControlCode}
	 * by replacing placeholder values in {@code FC}.
	 * 
	 * @param type this {@code NetFrame}'s {@code HDLCFrame} enum value
	 * @param code the {@code ControlCode} of this {@code NetFrame}
	 */
	private void setType (HDLCFrame.Types type, HDLCFrame.Commands code)
	{
		this.cc = code;
		this.type = type;
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
	
	/**
	 * 
	 * @param set Poll on {@code true} or Final on {@code false}
	 */
	public void setPollFinal(HDLCFrame pf)
	{
		this.pf = pf;
		this.fc = fc.replace("P", String.valueOf(pf == HDLCFrame.Poll? 1:0));
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
	public HDLCFrame.Types getFrameType()
	{
		return type;
	}
	
	/**
	 * Get {@code ControlCode} for this {@code NetFrame}.
	 * 
	 * @return
	 */
	public HDLCFrame.Commands getCC()
	{
		return this.cc;
	}
	
	public HDLCFrame getPollFinal()
	{
		return this.pf;
	}
	
	public boolean isPoll()
	{
		return getPollFinal() == HDLCFrame.Poll;
	}
	
	public boolean isFinal()
	{
		return getPollFinal() == HDLCFrame.Final;
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
		
		if (info != null)
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