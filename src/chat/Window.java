package chat;

import java.util.LinkedList;

import chat.NetFrame;

/**
 * This class represents a sliding {@code NetFrame} window implemented using HDLC.
 * 
 * @author Andréas K.LeF.
 */
@SuppressWarnings("serial")
public class Window extends LinkedList<NetFrame>
{
	private static final int MAX_WINDOW_LENGTH = 2^6-1;
	
	/**
	 * {@code NetFrame} index for sliding window.
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
	public boolean add (NetFrame newFrame)
	{
		if (this.size() == MAX_WINDOW_LENGTH)
		{
			return false;
		}
		
		this.add(newFrame);
		
		if (newFrame.type == NetFrame.Type.SFrame)
		{
			receivedFrameIndex = newFrame.getReceivedFrameIndex();
		}
		
		return true;
	}
	
	void acknowledgeReception (int currentFrameIndex)
	{
		this.currentFrameIndex = currentFrameIndex;
	}
}
