/**
 * 
 */
package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;

/**
 * @author Andréas K.LeF.
 * @author David Alleyn
 *
 */
class Connection
{
	protected static final int SOCKET_TIMEOUT = 2000;
	Socket socket;
	PrintWriter writer;
	protected LinkedList<String> messageQueue;
        
       /**
        * La fenetre coulissante 
        * 
        **/
	
	Window framesWindow;
	BufferedReader reader;
        
        
	public Connection(Socket socket) throws IOException
	{
		this.socket = socket;
		
		writer = new PrintWriter(socket.getOutputStream(), true);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		System.out.println("Connection from " + socket.getInetAddress() + " accepted.");
		//socket.setSoTimeout(SOCKET_TIMEOUT);
                
                messageQueue = new LinkedList<String>();
		
	}
	
	void send(NetFrame message) throws InterruptedException
	{
                if (framesWindow == null)
                {
                    framesWindow = new Window(this);

                }
            
                //writer.println(message);
            
		framesWindow.add(message);

	}
        
        void sendSRS(NetFrame message)
        {
            writer.println(message);
        }
        
        void waitForAck() throws IOException
        {
            reader.readLine();
        }
        
    InetAddress getAddress()
    {
        return socket.getInetAddress();
    }
	
	String read() throws IOException
	{
		String result = null;
		try
		{
                    
			result = reader.readLine();
                        writer.println("YOLOACK");
                        
		}
		catch (InterruptedIOException e)
		{
			result = null;
		}
		catch (java.net.SocketException e) 
		{
			result = null;
		}
		
		return result;
	}
	
	void close()
	{
		try
		{
			socket.close();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}

	protected void enqeue(String message) {
		messageQueue.add(message);
	}

	/**
	 * Retourne les message et les claires
	 * @return List of messages.
	 */
	protected LinkedList<String> getMessages() {
		LinkedList<String> temp = messageQueue;
		messageQueue = new LinkedList<String>();
		return temp;
	}
}