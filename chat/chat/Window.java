/**
 * 
 */
package chat;

import java.util.LinkedList;

/**
 * This class represents a sliding {@code NetworkFrame} window implemented using HDLC.
 * 
 * @author Andréas K.LeF.
 */
public class Window extends LinkedList<NetworkFrame>
{
	private static final int MAX_WINDOW_LENGTH = 2^5-1;
	
	/**
	 * {@code NetworkFrame} index for sliding window.
	 */
	protected int currentFrameIndex = 0, receivedFrameIndex = 0, sentframeIndex = 0;
	
	/**
	 * Constructs an empty {@code LinkedList<Frame>}
	 */
	Window ()
	{
		super();
	}
	
	@Override
	public boolean add (NetworkFrame newFrame)
	{
		if (this.size() == MAX_WINDOW_LENGTH)
		{
			return false;
		}
		
		this.add(newFrame);
		
		if (newFrame.type == NetworkFrame.Type.SFrame)
		{
			receivedFrameIndex = newFrame.getReceivedFrameIndex();
		}
		
		return true;
	}
	
	void acknowledgeReception (int frameIndex)
	{
		
	}
}
