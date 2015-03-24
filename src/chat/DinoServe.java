package chat;

import chat.NetFrame;
import chat.NetFrame.HDLCFrame;
import chat.NetFrame.HDLCFrame.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.net.InetAddress;

public class DinoServe
{
	/**
	 * The TCP port this server will listen on for incoming connections.
	 */
	private static final int DEFAULT_PORT = 4444;
	private static final int DEFAULT_SOCKET_TIMEOUT = 250;
	
	/**
	 * Server initializes an empty list of connections to clients.
	 */
	private static LinkedList<Connection> clients;
	
	/**
	 * Send each connected client's queued messages to all other clients.
	 * The messages being broadcast are  {@code NetFrame}s in {@code String} form.
	 * 
	 * @param clients List of {@code ClientConnection}s to message.
	 */
	private static void sendMessages()
	{
		for (Connection connection : clients)
		{
			try
			{
				for (String message : connection.getMessages())
				{
                	NetFrame msg = new NetFrame(message);
					connection.send(msg);
				}
			}
			catch (Exception e)
			{
				terminate(connection);
			}
		}
	}
    
	/**
	 * This method terminates a connection. 
	 * Connections die here.
	 * @param deadMan
	 */
	private static void terminate(Connection deadMan)
    {
		if (clients.contains(deadMan))
			clients.remove(deadMan);
		
		deadMan.close();
    }
	
	/**
	 * This method consumes a frame. Other functionality can go here.
	 * Frames go here to die.
	 * @param received
	 */
	private static void consume(NetFrame received)
    {
       System.out.println("Consumed : " + received);
    }
	
	/**
	 * Accepts a list of <code>clientConnections<code> to poll.
	 * 
	 * @param clients List each <code>ClientConnection<code> to poll.
	 * @throws UnknownHostException 
	 * @throws IOException Error reading from client connection.
	 */
	private static void pollStations() throws SocketException, UnknownHostException, IOException
	{
        String msg = "";
        for (Connection client : clients)
        {
            NetFrame RR = new NetFrame(client.getAddress(), Types.SFrame, Commands.RR), response = null;
            RR.setPollFinal(HDLCFrame.Poll);
            
            try
			{
            	client.send(RR);
            	msg = client.read();
            	response = new NetFrame(msg);
			}
            catch (NullPointerException e)
			{
				e.printStackTrace();
				return;
			}
            
            switch (response.getFrameType())
            {
            	case IFrame:
            		System.out.println(response.getInfo());
                    
                    //For each connection, check if the address matches
                    //If so, queue it to send it there
                    for (Connection destinationClient : clients)
                    {
                            destinationClient.enqeue(msg);
                    }
                    
            		break;
            		
            	case SFrame:
            		if (response.getCC() == Commands.RR && response.isFinal())
                    {
                        //System.out.println(client.getAddress() + " is RR.");
                    }
            		break;
            		
            	case UFrame:
            		switch (response.getCC())
                    {
                    	case DISC:
                    		//Client requesting clean disconnect
                    		System.err.println(client.getAddress() + " requesting disconnect.");
                    		NetFrame disc = new NetFrame(client.getAddress(), Types.UFrame, Commands.DISC);
                			disc.setPollFinal(HDLCFrame.Final);
                			client.send(disc);
                			
                			if (clients.contains(client))
                				clients.remove(client);
                            break;
                    }
            		break;
            }
        }
	}
    
	/**
	 * Initiates an HDLC handshake.
	 * @param client with which to do the handshake
	 */
	private static void handShake(Connection client) throws InterruptedException, Exception
    {
        // Set normal response mode
    	NetFrame snrm = new NetFrame(client.getAddress(), Types.UFrame, Commands.SNRM);
        client.send(snrm);

        NetFrame ua = new NetFrame(client.read());

        if (!(ua.getFrameType() == Types.UFrame && ua.getCC() == Commands.UA))
        {
            System.err.println("ERROR : Did not  UA frame from " + client.getAddress());
        }
    }
	/**
	 * Main server thread.
	 */
	public static void main(String[] args)
	{
        System.out.println("Server initializing...");
        
		ServerSocket serverSocket = null;
		clients = new LinkedList<Connection>();
			
		try
		{
			serverSocket = new ServerSocket(DEFAULT_PORT);
			serverSocket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT);
		}
		catch (IOException e)
		{
			System.err.println("FATAL: could not listen on port: " + args[0]);
			System.exit(-1);
		}
		Connection client = null;
        boolean clientsConnected = false;
        
		System.out.println("Server socket open.");
		System.out.println("Waiting for clients...");
		
        //Server will loop here indefinitely.
        while (true)
        {
            try
            {
            	//System.out.print("."); //XXX Heartbeat
                client = new Connection(
            		// thread blocks here until a connection is made
            		serverSocket.accept());
                
                handShake(client);
                clients.addLast(client);
                clientsConnected = true;
                
                System.out.println("Client added " + client.getAddress());
            }
			catch (UnknownHostException e)
			{
				// Cient did not connect properly
				terminate(client);
                e.printStackTrace();
			}
            catch (SocketTimeoutException e) 
            {
            	// The listen socket timed out.
            	//System.err.println("Timeout.");
            }
			catch (Exception e)
			{
                System.err.println("Listen loop error:");
                e.printStackTrace();
			}
            
            try
            {
            	// The listen socket timed out. Process queued messages
            	pollStations();
                sendMessages();
            }
            catch (IOException e)
			{
				// Cient disconnected
				terminate(client);
                e.printStackTrace();
			}
            
            if (clients.size() == 0 && clientsConnected)
            {
            	System.err.println("All clients have disconnected. Terminating server.");
            	break; // exit loop
            }
        }
        
		try // we're done.
		{
			for (Connection connection : clients)
			{
				terminate(connection);
			}
			serverSocket.close();
		}
		catch (IOException e)
		{
			System.exit(-1);
		}
	}
}