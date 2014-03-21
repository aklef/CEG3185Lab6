package chat;

import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class ClientConnection extends Connection
{	
	LinkedList<String> messageQueue;
	Window framesWindow; 
	
	private static final int SOCKET_TIMEOUT = 2000; 
	
	ClientConnection(Socket socket) throws IOException
	{
		super (socket);
		
		System.out.println("Connection from " + socket.getInetAddress() + " accepted.");
		socket.setSoTimeout(SOCKET_TIMEOUT);
		System.out.println("Client accepted");
		
		messageQueue = new LinkedList<String>();
		messageQueue.addLast(" " + reader.readLine() + " joined");
	}
	
	void enqeue(String message)
	{
		messageQueue.add(message);
	}
	
	/**
	 * Retourne les message et les claires
	 * @return List of messages.
	 */
	LinkedList<String> getMessages()
	{
		LinkedList<String> temp = messageQueue;
		messageQueue = new LinkedList<String>();
		return temp;
	}
}