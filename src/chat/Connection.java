package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

/**
 * Represents
 * 
 * @author Andréas K.LeF.
 * @author David Alleyn
 * @author Kent Hagerman
 *
 */
class Connection
{
   /**
    * The virtual 'sliding window'
    */
	private SlidingWindow slidingWindow;
	private LinkedList<String> messageQueue;
	private Socket socket;
	private BufferedReader socketReader;
	private PrintWriter socketWriter;
      
    /**
     * Create a one-time use connection for server clients.
     * 
     * @param socket The socket to open this connection on.
     */
	public Connection(Socket socket) throws IOException
	{
		this.socket = socket;
		this.slidingWindow = new SlidingWindow(this);
		this.socketWriter = new PrintWriter(socket.getOutputStream(), true);
		this.socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.messageQueue = new LinkedList<String>();
	}
	
	/**
     * Create a one-time use connection.
     * 
     * @param socket The socket to open this connection on.
	 */
	public Connection(InetAddress serverAddress, int serverPort) throws IOException
	{
		this(new Socket(serverAddress, serverPort));
	}

	/**
	 * Send a single frame through this connection.
	 * 
	 * @param frame to be sent throught this connection.
	 */
	void send(NetFrame frame)
	{
		slidingWindow.add(frame);
		slidingWindow.run();
	}
	
	/**
	 * Send frames throught this connection.
	 * 
	 * @param frames to be sent through this connection.
	 * @throws InterruptedException In case of network write failure. Network probably down.
	 */
	void send(NetFrame[] frames) throws InterruptedException
	{
        for (NetFrame frame : frames)
		{
    		slidingWindow.add(frame);
		}
        
		slidingWindow.run();
	}
        
    protected void sendSRS(NetFrame message)
    {
        socketWriter.println(message);
    }
    
    protected void waitForAck() throws IOException
    {
        // something should arrive unblocking this, signlaing an ACK.
    	// i.e wait for that YOLOACK
    	socketReader.readLine();
    }
        
    public InetAddress getAddress()
    {
        return socket.getInetAddress();
    }
	
    /**
     * Check this connection for incoming messages
     * @return incoming message.
     */
	protected String read() throws IOException, SocketException
	{
		String result = socketReader.readLine();
//		System.out.println(socket + " says:\n"+ result);
        socketWriter.println("YOLOACK"); // Write anything to acknowledge mesage receipt..
		
		return result;
	}
	
	void close()
	{
		try
		{
			
			this.socket.close();
		}
		catch (IOException e){}
	}

	protected void enqeue(String message)
	{
		messageQueue.add(message);
	}

	/**
	 * Retourne les message et les claires
	 * @return List of messages.
	 */
	protected LinkedList<String> getMessages()
	{
		LinkedList<String> temp = messageQueue;
		messageQueue = new LinkedList<String>();
		return temp;
	}
	
	/**
	 * Returns a {@code String} representation of this {@code Connection}.
	 */
	@Override
	public String toString()
	{
		return socket.toString();
	}
}
