package chat;

import chat.NetFrame.ControlCode;
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

import chat.NetFrame.*;

public class MyClientWin extends Applet implements Runnable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1702635793607554428L;
	
	static boolean bConnected = false;
	

	static String
			clientId, serverIP,
			fromServer, fromUser,
			sConnection	  = "Not Connected to the chat server!";
	private int serverPort = 4444;
	/**
	 * User input field
	 */
	static TextField textField;
	/**
	 * Displays server messages.
	 */
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
							.showInputDialog("Enter port number:", 4444));
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
				
				String received = serverConnection.read();
                                NetFrame snrm = new NetFrame(received);
                                
                                if (snrm.getFrameType() != Type.UFrame || snrm.getCC() != ControlCode.SNRM)
                                {
                                    System.out.println("ERROR : Did not receive SNRM frame from " + serverConnection.getAddress());
                                }
                                
                                NetFrame ua = new NetFrame((Object)serverConnection.getAddress(), Type.UFrame, ControlCode.UA);
                                serverConnection.send(ua.toString());
                                
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
		Frame frame = new Frame("Dinotalk - Chatting Program");
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


		
		try
		{
			if ((fromServer = serverConnection.read()) != null)
			{
				NetFrame receivedFrame = new NetFrame(fromServer);

				//
				// switch on type of frame
				//
				switch(receivedFrame.getFrameType())
				{
					case IFrame:
                        //Consume
                        fromServer = receivedFrame.getInfo();
						textArea.setText(textArea.getText() + "\n" + fromServer); // put message on screen
						break;
						
					case SFrame:
						if (receivedFrame.getCC() == ControlCode.RR)
                        {
                            //Something to send
                            if (fromUser != null)
                            {
                                NetFrame toSend = new NetFrame((Object)serverConnection.getAddress(), Type.IFrame, ControlCode.RR, fromUser);
                                serverConnection.send(toSend.toString());
                                fromUser = null;
                            }
                            //Nothing to send
                            else
                            {
                               NetFrame toSend = new NetFrame((Object)serverConnection.getAddress(), Type.SFrame, ControlCode.RR); 
                               serverConnection.send(toSend.toString()); 
                            }
                        }
                        else
                        {
                            System.out.println("Error: did not receive RR control code");
                        }
						break;
						
					case UFrame:
                        //Ignore
						break;
				}
				
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