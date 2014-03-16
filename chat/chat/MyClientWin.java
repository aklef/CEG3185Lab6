package chat;

import java.applet.Applet;
import java.awt.Button;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.TextArea;
import java.awt.TextField;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import chat.Frame.Type;

public class MyClientWin extends Applet implements Runnable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1702635793607554428L;
	
	static boolean bConnected = false;
	
	static Socket mySocket  = null;
	
	static String
			clientId, serverIP,
			fromServer, fromUser,
			sConnection	  = "Not Connected to the chat server!";
	private int serverPort = 4444;
	
	static TextField textField;
	static TextArea textArea;
	
	Thread thread;
	
	private static ServerConnection serverConnection;
	
	@Override
	public void init()
	{
		textField = new TextField("", 50);
		textArea = new TextArea("No Messages", 15, 50);
		Button button = new Button("Connect");
		Button closebutton = new Button("Close");
		Button msgbutton = new Button("Send Message");
		// Button chkmsgbutton = new Button("Check Messages");
		
		add(textField);
		add(button);
		add(closebutton);
		add(msgbutton);
		// add(chkmsgbutton);
		add(textArea);
	}
	
	@Override
	public void paint(Graphics g)
	{
		// Font font = new Font("Arial", Font.PLAIN, 12);
		Font fontb = new Font("Arial", Font.BOLD, 14);
		
		g.setFont(fontb);
		g.drawString(sConnection, 60, 330);
		
		/*
		 * try { fromServer = in.readLine(); }catch (InterruptedIOException e) {
		 * } if (fromServer) != null) { textArea.setText(textArea.getText()+
		 * "\n" + fromServer); }
		 */		
	}
	
	/**
	 * Trapping button actions
	 */
	@Override
	public boolean action(Event evt, Object arg)
	{
		if (arg == "Close")
		{
			if(bConnected)
			{
				serverConnection.close();
			}
			
			System.exit(0);
		}
		
		// ******************************************
		// connect button pressed
		// ******************************************
		else if (arg == "Connect" && !bConnected)
		{
			try
			{
				// get server IP and name of client		
				serverIP = JOptionPane.showInputDialog("Enter IP of chat server:", "127.0.0.1");
				clientId = JOptionPane.showInputDialog("Enter your name:", "Anonymous");
				
				try
				{
					serverPort = Integer.parseInt(JOptionPane
							.showInputDialog("Enter port number:"));
				}
				catch (NumberFormatException e)
				{
					serverPort = 4444;
				}
				
				// connect to the socket
				
				serverConnection = new ServerConnection(new Socket(serverIP, serverPort));

				
				// optional - setting socket timeout to 5 secs
				// this is not necessary because application
				// runs with multiple threads
				//
				// mySocket.setSoTimeout(5000);
				
				bConnected = true;
				//
				// define input and output streams for reading and
				// writing to the socket
				//
				serverConnection.send(clientId);
				
				//
				// set screen messages
				//
				sConnection = "Connected to the chat server!";
				
				//
				// define new thread
				//
				thread = new Thread(this);
				thread.start();
				
			}
			catch (UnknownHostException e)
			{
				bConnected = false;
				sConnection = "Not Connected to the chat server!";
				JOptionPane.showMessageDialog(null, "Don't know about host: " + serverIP);
			}
			catch (IOException e)
			{
				bConnected = false;
				sConnection = "Not Connected to the chat server!";
				JOptionPane.showMessageDialog(null, "Server is not running!");
			}
		}
		
		// ******************************************
		// Send Message button pressed
		// ******************************************
		else if (arg == "Send Message")
		{
			if (textField.getText() != null)
			{
				//
				// copy content of the message text into
				// internal buffer for later processing
				// only one message can be stored into the
				// buffer
				//
				fromUser = textField.getText();
				textField.setText("");
			}
			else
			{
				fromUser = null;
			}
			
		}
		repaint();
		return true;
	}
	
	/**
	 * Main method for the class.
	 * Initializes whole environment.
	 */
	public static void main(String args[])
	{
		//
		// define window and call standard methods
		//
		MyClientWin app = new MyClientWin();
		Frame frame = new Frame("Totally Extinct Dinosaurs - Client Chatting Program");
		app.init();
		app.start();
		
		frame.add("Center", app);
		frame.setSize(400, 400);
		frame.setVisible(true);
	}
	
	/**
	 * Stop server.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void stop()
	{
		thread.interrupt();
		thread.stop();
	}
	
	/**
	 * Run - thread method.
	 */
	@Override
	public void run()
	{
		while (true)
		{
			checkServer();
			try	{Thread.sleep(10);}
			catch (InterruptedException e){}
		}
	}
	
	/**
	 * FIXME
	 * checkServer - this is a main client algorithm
	 */
	public void checkServer()
	{
		String 
			sTemp = null,
			sFrameType = null;
		
		chat.Frame receivedFrame;
		Type receivedFrameType;
		
		try
		{
			if ((fromServer = serverConnection.read()) != null)
			{
				receivedFrame = new chat.Frame(fromServer);
				receivedFrameType = receivedFrame.getFrameType();
				//
				// switch on type of frame
				//
				switch(receivedFrameType)
				{
					case IFrame:
						
						break;
						
					case SFrame:
						
						break;
						
					case UFrame:
						
						break;
				}
				
				//FIXME Start
				if (receivedFrameType.equals("SEL"))
				{
					fromServer = fromServer.substring(4, fromServer.length());
					sTemp = textArea.getText();
					
					//
					// put message on screen
					//
					textArea.setText(sTemp + "\n" + fromServer);
				}
				//
				// if received frame was POLLING
				// and data to be send
				// return ACK with data
				// otherwise
				// return NACK frame
				//
				else if (receivedFrameType.equals("POL"))
				{
					//
					// message in stack to be send to the server
					//
					if (fromUser != null)
					{
						serverConnection.send("ACK" + clientId + " says: " + fromUser);
						fromUser = null;
					}
					else
					{
						serverConnection.send("NAC");
					}
				}
				// FIXME End
			}
		}
		// catch exceptions while reading/writing from/to server
		catch (Exception e)
		{
			serverConnection.close();
			stop();
		}
	}
}