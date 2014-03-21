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
class Connection
{
	Socket socket;
	PrintWriter writer;
	BufferedReader reader;
	
	public Connection(Socket socket) throws IOException
	{
		this.socket = socket;
		
		writer = new PrintWriter(socket.getOutputStream(), true);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	void send(String message)
	{
		writer.println(message);
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