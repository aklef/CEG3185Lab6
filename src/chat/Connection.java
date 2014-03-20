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

/**
 * @author Andréas K.LeF.
 *
 */
abstract class Connection
{
	@SuppressWarnings("unused")
	private static int RECEIVED_SEQUENCE_NUMBER = 0;
	Socket socket;
	PrintWriter writer;
	BufferedReader reader;
	
	public Connection(Socket socket) throws IOException
	{
		this.socket = socket;
		
		writer = new PrintWriter(socket.getOutputStream(), true);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	public void send(String message)
	{
		// Parse input
		
		writer.println(message);
	}
        
        public InetAddress getAddress()
        {
            return socket.getInetAddress();
        }
	
	String read() throws IOException
	{
		String result = null;
		try
		{
			result = reader.readLine();
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
}