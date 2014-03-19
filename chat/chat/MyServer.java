package chat;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.util.LinkedList;

import chat.Frame.Type;

public class MyServer
{
	/**
	 * The TCP port this server will listen on for incoming connections.
	 */
	public static final int LISTEN_PORT = 4444;
	public static final int SERVER_SOCKET_TIMEOUT = 1000;
	
	/**
	 * Send each connected client's queued messages to all other clients.
	 * A very for-midable method.
	 * 
	 * The messages being broadcast are most likely {@code Frame}s in {@code String} form.
	 * 
	 * @param clients List of {@code ClientConnection}s to message.
	 */
	public static void broadcastMessages(LinkedList<ClientConnection> clients)
	{
		for (ClientConnection currentClient : clients)
		{
			LinkedList<String> currentClientMessages = currentClient.getMessages();
			
			for (String message : currentClientMessages)
			{
				System.out.println("Sending : " + message);
				
				for (ClientConnection otherClient : clients)
				{
					if (!currentClient.equals(otherClient))
					{
						otherClient.send(message);
					}
				}
			}
		}
	}
	
	/**
	 * Accepts a list of <code>clientConnections<code> to poll.
	 * 
	 * @param clientConnections List each <code>ClientConnection<code> to poll.
	 * @throws IOException Error reading from client connection.
	 */
	public static void pollStations(LinkedList<ClientConnection> clientConnections) throws IOException
	{
		
		for (ClientConnection connection : clientConnections)
		{
			
			Frame pollFrame = new Frame(connection.socket.getInetAddress().toString(),Type.IFrame, Frame.ControlCode.RNR);
			connection.enqueue(pollFrame.toString());
			
			String pollResult = connection.read();
			
			if (pollResult != null && pollResult != "NAC")
			{
				System.out.println("Got response: " + pollResult);
				
				if (pollResult.startsWith("ACK"))
				{
					connection.addMessage(pollResult.getInfo);
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		
		ServerSocket serverSocket = null;
		LinkedList<ClientConnection> clientConnections = new LinkedList<ClientConnection>();
		
		boolean hasClients = false;
				
		try
		{
			serverSocket = new ServerSocket(LISTEN_PORT);
		}
		catch (IOException e)
		{
			System.err.println("Could not listen on port: " + args[0]);
			System.exit(-1);
		}
		
		serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT);
		
		while (true)
		{
			try
			{
				System.err.print("Polling. ");
				clientConnections.add(new ClientConnection(serverSocket.accept()));
				hasClients = true;
			}
			catch (InterruptedIOException e) 
			{
				//Timeout, process queued messages
			}
			
			broadcastMessages(clientConnections);
			pollStations(clientConnections);
			
			if (clientConnections.size() == 0 && !hasClients)
			{
				break;
			}
		}
		
		// We have escaped the listen loop; close the server.
		serverSocket.close();
		
		for (ClientConnection clientConnection : clientConnections)
		{
			clientConnection.close();
		}
	}
}