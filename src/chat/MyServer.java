package chat;
import chat.NetFrame.ControlCode;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.util.LinkedList;

import chat.NetFrame.Type;
import java.net.InetAddress;

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
	 * The messages being broadcast are most likely {@code NetFrame}s in {@code String} form.
	 * 
	 * @param clients List of {@code ClientConnection}s to message.
	 */
	public static void sendMessages(LinkedList<ClientConnection> clients)
	{
		for (ClientConnection currentClient : clients)
		{
			LinkedList<String> currentClientMessages = currentClient.getMessages();
			
			for (String message : currentClientMessages)
			{
				currentClient.send(message);
			}
		}
	}
        
        public static void consume(NetFrame received)
        {
            System.out.println("Consumed : " + received);
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
            NetFrame rrxp = new NetFrame(connection.getAddress(), Type.SFrame, ControlCode.RR);
            rrxp.setPollFinal(true);
            connection.send(rrxp.toString());
            
                                
            String message = connection.read();
            NetFrame rrxp_read = new NetFrame(message);

            if (rrxp_read.getFrameType() == Type.SFrame && rrxp_read.getCC() == ControlCode.RR)
            {
                //Received RR frame
                System.out.println("Station " + connection.getAddress() + " is read to receive");
            }
            else if (rrxp_read.getFrameType() == Type.IFrame && rrxp_read.getCC() == ControlCode.RR)
            {
                //Right branch
                InetAddress destination = rrxp_read.getDestinationAddress();
                
                boolean found = false;
                for (ClientConnection otherConnection : clientConnections)
                {
                    if (destination.equals(otherConnection.getAddress()))
                    {
                        otherConnection.addMessage(message);
                        found = true;
                    }
                }
                
                if (!found)
                {
                    consume(rrxp_read);
                }
            }
        }
	}
        
    public static void handShake(ClientConnection connection) throws IOException
    {
        {
            NetFrame snrm = new NetFrame(connection.getAddress(), Type.UFrame, ControlCode.SNRM);
            connection.send(snrm.toString());
        }
        {
            String message = connection.read();
            NetFrame ua = new NetFrame(message);

            if (ua.getFrameType() != Type.UFrame || ua.getCC() != ControlCode.UA)
            {
                System.out.println("ERROR : Did not receive UA frame from " + connection.getAddress());
            }
        }    
    }
	
	public static void main(String[] args) throws IOException
	{
		ServerSocket serverSocket = null;
		LinkedList<ClientConnection> clientConnections = new LinkedList<ClientConnection>();
			
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
                
        boolean connected = false;
        
        //During the simulation, 2 clients must be connected. Allow them to connect before simulation
        while (true)
        {
            try
            {
                System.err.print("Waiting for " +  (2 - clientConnections.size()) + " more clients to connect for simulation.");
                clientConnections.add(new ClientConnection(serverSocket.accept()));
                
                handShake(clientConnections.getLast());
                connected = true;
            }
            catch (InterruptedIOException e) 
            {
                    //Timeout, process queued messages
            }	
            
            pollStations(clientConnections);
            sendMessages(clientConnections);

            if (clientConnections.size() == 0 && connected)
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

//			NetFrame pollFrame = new NetFrame(connection.socket.getInetAddress().toString(),Type.IFrame, NetFrame.ControlCode.RNR);
//			connection.enqueue(pollFrame.toString());
//			
//			String pollResult = connection.read();
//			
//			if (pollResult != null && pollResult != "NAC")
//			{
//				System.out.println("Got response: " + pollResult);
//				
//				if (pollResult.startsWith("ACK"))
//				{
//					connection.addMessage(pollResult.getInfo);
//				}
//			}
	
}