package chat;

import java.util.LinkedList;

import chat.NetFrame;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Window
{
	private static final int MAX_WINDOW_LENGTH = 7;
	
        NetFrame[] messageQueue;
        Connection connection;
        
	/**
	 * {@code NetFrame} index for sliding window.
	 */
	protected int L = 0, R = 8, M = 0;
	
	/**
	 * Constructs an empty {@code LinkedList<Frame>}
	 */
	Window (Connection connect)
	{
                connection = connect;
		messageQueue = new NetFrame[MAX_WINDOW_LENGTH];
	}
	
	public boolean add(NetFrame newFrame) throws InterruptedException
	{
	
            //Wait for window to catch up before sending message
            while (M == R)
            {
                System.out.println("Waiting for window to slide");
                Thread.sleep(1000);
            }
            
            messageQueue[M++] = newFrame;
            
            if (M >= MAX_WINDOW_LENGTH)
                M = 0;
            
            System.out.println("Added to window");
            run();
            return true;
	}


    public void run() {
        

            System.out.println("Looping");
            if (M != L)
            {
                try {
                    System.out.println("Sending");
                    connection.sendSRS(messageQueue[L]);
                    System.out.println("Acking");
                    connection.waitForAck();
                    System.out.println("Acked");
                    L++;
                    R++;
                    
                    if (L >= MAX_WINDOW_LENGTH)
                        L = 0;
                    
                    if (R >= MAX_WINDOW_LENGTH)
                        R = 0;
                } catch (IOException ex) {
                    Logger.getLogger(Window.class.getName()).log(Level.SEVERE, null, ex);
                } 
                 
            }
           
        }
        
}
