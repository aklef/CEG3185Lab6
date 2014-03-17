/**
 * 
 */
package chat;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


/**
 * Represents a single link-layer HDLC frame.
 * Each {@code Frame} has a designatred {@code Type} enum value.
 * 
 * @author Andréas K.LeF.
 */
public class Frame
{
	private static final String FLAG = "01111110";
	@SuppressWarnings("unused")
	private static final int MAX_FRAME_SIZE = 64*8;
	private static int SEQUENCE_NUMBER = 0;
	
	public static enum Type {
		/**
		 * User data frame.
		 */
		IFrame, 
		/**
		 * Control frame
		 */
		SFrame, 
		/**
		 * Unnumbered frame for link management
		 */
		UFrame
		};
	private static EnumMap<Type, String> FC;
	static
    {
		FC = new EnumMap<Type, String>(Type.class);
		FC.put(Type.IFrame, "0NSPNR");
		FC.put(Type.SFrame, "10CCPNR");
		FC.put(Type.UFrame, "11CCPBBB");
    }
	public static enum ControlCode {RR, RNR, REJ, SREJ};
	private static EnumMap<ControlCode, String> CC;
	static
    {
		CC = new EnumMap<ControlCode, String>(ControlCode.class);
		CC.put(ControlCode.RR, "00");
		CC.put(ControlCode.RNR, "01");
		CC.put(ControlCode.REJ, "10");
		CC.put(ControlCode.SREJ, "11");
    }
	
	private String addr, control, info;
	protected String sequenceNumber;
	/**
	 * This {@code Frame}'s {@code Type}.
	 */
	protected Type type;
	
	/**
	 * Empty constructor
	 */
	public Frame (String frame)
	{
		this.fromString(frame);
	}

	public Frame (Object Addr, Type type, ControlCode Code)
	{
		this(Addr, type, Code, "");
	}
	
	public Frame (Object addr, Type type, ControlCode code, Object info)
	{
		this.addr = addr.toString();
		this.info = info.toString();
		this.setType(type, code);
	}
	
	private void setType (Type type, ControlCode code)
	{
		this.type = type;
		control = FC.get(type);
		
		switch (type) {
			// User data frame
			case IFrame:
				sequenceNumber = getSequenceNumber();
				control.replace("NS", sequenceNumber);
				break;
			
			// Control frame
			case SFrame:
				control.replace("CC", CC.get(code));
				break;
				
			// Unnumbered frame for link management
			case UFrame:
				break;
		}
	}
	
	protected Type getFrameType()
	{
		return type;
	}
	
	@SuppressWarnings("unused")
	private void setPollFinal(Boolean isTrue)
	{
		if (isTrue)
		{
			control.replace("P", "1");
		}
		else
		{
			control.replace("P", "0");
		}
	}
	
	/**
	 * Returns a {@code String} representation of this {@code Frame}.
	 */
	@Override
	public String toString()
	{
		String frame = FLAG + addr + control;
		
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
		return Integer.parseInt(control.substring(5, 7));
	}
	
	private String getSequenceNumber()
	{
		return Integer.toBinaryString(SEQUENCE_NUMBER++ % 8);
	}
	
	protected String getAddrInBinary(Object Addr)
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
	 * @return the decimal formatted IP.
	 */
	protected String getAddrFromBinary(Object Addr)
			throws IndexOutOfBoundsException, NumberFormatException
	{
		String addr = "", binaryNum = Addr.toString();
		
		for (int i = 0; i < 4; i++)
		{
			binaryNum = addr.substring(i*8, i*8 + 7);
			addr += Integer.parseInt(binaryNum);
		}
		
		return addr;
	}
	
	/**
	 * Parses a string as input from which to create this frame.
	 * 
	 * @param frame {@code String} representation of this frame.
	 */
	void fromString(String frame)
	{
		frame.replaceAll(FLAG, "");
		this.addr = (frame.substring(0, 4*8));	
		this.control = frame.substring(4*8, 5*8);
		this.info = frame.substring(5*8, frame.length() - 1);
		
		if (control.charAt(0) == '0')
		{
			this.type = Frame.Type.IFrame;
		}
		else if (control.charAt(1) == '0')
		{
			this.type = Frame.Type.SFrame;
		}
		else
		{
			this.type = Frame.Type.UFrame;
		}
	}
	
	//*******************************HELPER METHODS************************************//
	
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