package chat;

import chat.NetFrame.*;
import chat.NetFrame.HDLCFrame.*;

class SlidingWindow
{
	private static final int MAX_WINDOW_LENGTH = 8;
	private int NUMBER_SEND_SEQUENCE, NUMBER_RECEIVE_SEQUENCE;
	private HDLCFrame.Types previousFrameType;
	
    private NetFrame[] slidingWindow;
    private Connection connection;
        
	/**
	 * {@code NetFrame} index for sliding slidingWindow. Left, Right and Middle(?)
	 */
	private int L = 0, R = 8, M = 0;
	
	/**
	 * Constructor for the sliding SlidingWindow. Consists of an empty {@code LinkedList<Frame>} of HDLC frames of length {@code MAX_WINDOW_LENGTH}.
	 */
	public SlidingWindow (Connection connect)
	{
        this.connection = connect;
		this.slidingWindow = new NetFrame[MAX_WINDOW_LENGTH];
		NUMBER_SEND_SEQUENCE = 0;
		NUMBER_RECEIVE_SEQUENCE = 0;
	}
	
	public boolean add(NetFrame frame)
			//throws InterruptedException
	{
		if (previousFrameType == Types.IFrame && frame.getFrameType() != Types.IFrame)
        {
			// No longer sending I-Frames. Reset.
			NUMBER_SEND_SEQUENCE = 0;
        }
		else if (previousFrameType == Types.IFrame && frame.getFrameType() == Types.IFrame)
        {
			NUMBER_SEND_SEQUENCE = (NUMBER_SEND_SEQUENCE + 1) % 8;
        	frame.setNSS(NUMBER_SEND_SEQUENCE);
        }
		
		if (frame.getFrameType() == Types.IFrame || frame.getFrameType() == Types.SFrame)
        {
			if (frame.getFrameType() == Types.IFrame)
	        	frame.setNSS(NUMBER_SEND_SEQUENCE);
	        	
			frame.setNRS(NUMBER_RECEIVE_SEQUENCE);
        }
        
		//Wait for slidingWindow to catch up before sending message
//        while (M == R)
//        {
//            System.out.print("Sliding..");
//            Thread.sleep(500);
//        }

        previousFrameType = frame.getFrameType();
        slidingWindow[M] = frame;
        M = (M+1) % MAX_WINDOW_LENGTH; // increment for next frame
        
        return true;
	}
	
	/**
	 * 
	 */
    public void run()
    {
        if (M != L)
        {
            connection.sendSRS(slidingWindow[L]);
            
            this.L = (++L) % MAX_WINDOW_LENGTH;
            this.R = (++R) % MAX_WINDOW_LENGTH;
        }
    }
}