package chat;

import chat.NetFrame.Type;
import chat.NetFrame.ControlCode;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.net.InetAddress;

public class MyServer
{
	/**
	 * The TCP port this server will listen on for incoming connections.
	 */
	public static final int LISTEN_PORT = 4444;
	public static final int SERVER_SOCKET_TIMEOUT = 15000;
	
	/**
	 * Send each connected client's queued messages to all other clients.
	 * 
	 * 
	 * The messages being broadcast are most likely {@code NetFrame}s in {@code String} form.
	 * 
	 * @param clients List of {@code ClientConnection}s to message.
	 */
	public static void sendMessages(LinkedList<Connection> clientConnections)
	{
		for (Connection connection : clientConnections)
		{
			LinkedList<String> clientMessages = connection.getMessages();
			
			try
			{
				for (String message : clientMessages)
				{
					connection.send(new NetFrame(message));
				}
				
			}
			catch (Exception e)
			{
				connection.close();
				clientConnections.remove(connection);
			}
		}
	}
    
	/**
	 * This method consumes a frame. Other functionality can go here.
	 * @param received
	 */
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
	public static void pollStations(LinkedList<Connection> clientConnections)
	{
		
        for (Connection connection : clientConnections)
        {
            String message = "";
            NetFrame rrxp_read = null, rrxp = new NetFrame(connection.getAddress(), Type.SFrame, ControlCode.RR);
            rrxp.setPollFinal(true);
            
            try
			{
                message = connection.read();
                rrxp_read = new NetFrame(message);
				connection.send(rrxp);
				
			}
			catch (Exception e)
			{
				connection.close();
				clientConnections.remove(connection);
				
				return;
			}

            if (rrxp_read.getFrameType() == Type.SFrame &&
            		rrxp_read.getCC() == ControlCode.RR)
            {
                //Received RR frame
                System.out.println("Station " + connection.getAddress() + " is ready to receive");
            }
            else if (rrxp_read.getFrameType() == Type.IFrame &&
            			rrxp_read.getCC() == ControlCode.RR)
            {                
                //Right branch
                InetAddress destination = rrxp_read.getDestinationAddress();
                
                //For each connection, check if the address matches
                //If so, queue it to send it there
                boolean found = false;
                for (Connection otherConnection : clientConnections)
                {
                    if (destination.equals(otherConnection.getAddress()))
                    {
                        otherConnection.enqeue(message);
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
    
	/**
	 * Initiates an HDLC handshake.
	 * @param connection wuth which to do the handshake
	 * @throws IOException
	 * @throws InterruptedException
	 */
    public static void handShake(Connection connection) throws Exception
    {
        // Set normal response mode
    	NetFrame snrm = new NetFrame(connection.getAddress(), Type.UFrame, ControlCode.SNRM);
        connection.send(snrm);

        String message = connection.read();
        NetFrame ua = new NetFrame(message);

        if (ua.getFrameType() != Type.UFrame || ua.getCC() != ControlCode.UA)
        {
            System.err.println("ERROR : Did not receive UA frame from " + connection.getAddress());
        }
    }
	
	public static void main(String[] args)
	{
        System.out.println("Server initializing...");
        
		ServerSocket serverSocket = null;
		LinkedList<Connection> clientConnections = new LinkedList<Connection>();
			
		try
		{
			serverSocket = new ServerSocket(LISTEN_PORT);
		}
		catch (IOException e)
		{
			System.err.println("FATAL: could not listen on port: " + args[0]);
			System.exit(-1);
		}
		
		//serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT);
        boolean clientsConnected = false;
        
		System.out.println("Initialized. Waiting for clients...");
        
        //During the simulation, 2 clients must be connected. Allow them to connect before simulation
        while (true)
        {
        	Connection con = null;
            try
            {
                con = new Connection(
            		// server thread waits here until a connection is made
            		serverSocket.accept());

                handShake(con);
                clientConnections.addLast(con);
                clientsConnected = true;
                System.out.println("Client added " + con.getAddress());
            }
            catch (InterruptedIOException e) 
            {
            	// Timeout, process queued messages
            	pollStations(clientConnections);
                sendMessages(clientConnections);
                
            }
			catch (Exception e)
			{
				// Cient did not connect properly
				System.out.println("Could not add client...");
				e.printStackTrace();
				con = null;
			}	
            
            if (clientConnections.size() == 0 && clientsConnected)
            {
                break;
            }
        }
        
		try
		{
			for (Connection clientConnection : clientConnections)
			{
				clientConnection.close();
			}
			serverSocket.close();
		}
		catch (IOException e)
		{
			System.out.println(e);
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