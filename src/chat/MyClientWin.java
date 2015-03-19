package chat;

import chat.NetFrame.*;
import java.applet.Applet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.net.InetAddresses;

public class MyClientWin extends Applet implements ActionListener, KeyListener, Runnable
{
	private static final long serialVersionUID = 1702635793607554428L;
	
	private String
			clientId, serverIP,
			fromServer, fromUser,
			connectionStatus	  = "Not Connected to Chat Server!";
	private int serverPort = 4444;
	private boolean isConnected = false;
	/**
	 * User input field
	 */
	private TextField textField;
	/**
	 * Displays server messages.
	 */
	private TextArea textArea;
	private JButton connectButton, msgButton;
	private static Frame frame;
	
	Thread thread;
	private Connection serverConnection;
	
	/************************************************************************/
	
	@Override
	public void init()
	{
		textField = new TextField("", 50);
		textArea = new TextArea("No Messages", 15, 50);
		textArea.setEditable(false);
		connectButton = new JButton("Connect");
		msgButton = new JButton("Send Message");
		msgButton.setEnabled(false);
		JButton closeButton = new JButton("Close");
		// Button chkmsgbutton = new Button("Check Messages");
		
		connectButton.addActionListener(this);
		connectButton.addKeyListener(this);
		closeButton.addActionListener(this);
		closeButton.addKeyListener(this);
		msgButton.addActionListener(this);
		msgButton.addKeyListener(this);
		
		add(textField);
		add(connectButton);
		add(closeButton);
		add(msgButton);
		// add(chkmsgbutton);
		add(textArea);
		
		try
		{
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI ( this ) ;
		}
		catch (Exception e){}
	}
	
	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
		
		Font fontb = new Font("Arial", Font.BOLD, 14);
		
		if (isConnected)
		{
			g.setColor(Color.GREEN);
		}
		else
		{
			g.setColor(Color.RED);
		}
		
		g.setFont(fontb);
		g.drawString(connectionStatus, 60, 330);
		
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
	public void actionPerformed(ActionEvent evt)
	{
		String arg = evt.getActionCommand();
		
		if (arg == "Close")
		{
			if(isConnected)
			{
				serverConnection.close();
			}
			
			System.exit(0);
		}
		
		// ******************************************
		// connect button pressed
		// ******************************************
		else if (arg == "Connect" && !isConnected)
		{
			try
			{
				// get server IP and name of client	
				try
				{
					InetAddresses.forString(textField.getText());
				}
				catch (IllegalArgumentException e)
				{
					serverIP = JOptionPane.showInputDialog("Enter Chat server IP:", "127.0.0.1");
				}
				
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
				
				serverConnection = new Connection(new Socket(serverIP, serverPort));
				
                System.out.println("Connected");
                isConnected = true;
                msgButton.setEnabled(true);
                connectButton.setEnabled(false);
				// optional - setting socket timeout to 5 secs
				// this is not necessary because application
				// runs with multiple threads
				// mySocket.setSoTimeout(5000);
				
				String received = serverConnection.read();
                System.out.println(received);
                
                NetFrame snrm = new NetFrame(received);
                if (snrm.getFrameType() != Type.UFrame || snrm.getCC() != ControlCode.SNRM)
                {
                    System.out.println("ERROR : Did not receive SNRM frame from " + serverConnection.getAddress());
                }
                
                NetFrame ua = new NetFrame(serverConnection.getAddress(), Type.UFrame, ControlCode.UA);
                try
                {
                    serverConnection.send(ua);
                }
                catch (InterruptedException ex)
                {
                    Logger.getLogger(MyClientWin.class.getName()).log(Level.SEVERE, null, ex);
                }
                                
				connectionStatus = "Connected to the chat server!";
                
				//
				// define new thread
				//
				thread = new Thread(this);
				thread.start();
			}
			catch (UnknownHostException e)
			{
				isConnected = false;
				connectionStatus = "Not Connected to the chat server!";
				JOptionPane.showMessageDialog(null, "Don't know about host: " + serverIP);
			}
			catch (IOException e)
			{
				isConnected = false;
				connectionStatus = "Not Connected to the chat server!";
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
				fromUser = clientId + ": " + textField.getText();
				textField.setText("");
			}
			else
			{
				fromUser = null;
			}
			
		}
		//repaint();
	}
	
	/**
	 * Main method for the class.
	 * Initializes whole environment.
	 */
	public static void main(String args[])
	{
		// define window and call standard methods
		MyClientWin client = new MyClientWin();
		frame = new Frame("Dinotalk - Chatting Program");
		client.init();
		client.start(); // client starts listening
		
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we){
				   System.exit(0);
			}
		});
		
		frame.add("Center", client);
		frame.setSize(400, 400);
		frame.setResizable(false);
		frame.setLocation(150, 170);
		frame.setVisible(true);
		client.connectButton.requestFocusInWindow();
	}
	
	/**
	 * Stop server.
	 */
	@Override
	public void stop()
	{
		try
		{
			thread.interrupt();
		}
		catch(Exception e){}
		
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
			try	
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e){}
		}
	}
	
	@Override
	public void keyTyped(KeyEvent key){}
	
	@Override
	public void keyPressed(KeyEvent key)
	{
		System.out.println("Pressed "+key);
		System.out.println(key.getKeyCode()+" compared to "+KeyEvent.VK_ENTER);
		
		if (key.getKeyCode() == KeyEvent.VK_ENTER && key.getComponent().getClass() == JButton.class)
		{
			((JButton) key.getComponent()).doClick();
		}
		else if (key.getKeyCode() == KeyEvent.VK_ENTER && key.getComponent().getClass() == TextField.class)
		{
			msgButton.doClick();
		}
	}
	
	@Override
	public void keyReleased(KeyEvent key){}
	
	/**
	 * checkServer - this is a main client algorithm
	 */
	public void checkServer()
	{
		try
		{
			if ((fromServer = serverConnection.read()) != null)
			{
				NetFrame receivedFrame = new NetFrame(fromServer);

				// switch on type of frame
				switch(receivedFrame.getFrameType())
				{
					case IFrame:
                        //Consume
                        fromServer = receivedFrame.getInfo();
						textArea.setText(textArea.getText() + "\n" + fromServer); // put message on screen
						break;
						
					case SFrame: case UFrame:
                        if (receivedFrame.getCC() == ControlCode.RR)
                        {
                            //Something to send
                            if (fromUser != null)
                            {
                                NetFrame toSend = new NetFrame(serverConnection.getAddress(), Type.IFrame, ControlCode.RR, fromUser);
                                serverConnection.send(toSend);
                                fromUser = null;
                            }
                            //Nothing to send
                            else
                            {
                               NetFrame toSend = new NetFrame(serverConnection.getAddress(), Type.SFrame, ControlCode.RR); 
                               serverConnection.send(toSend); 
                            }
                        }
                        else
                        {
                            System.out.println("Error: did not receive RR control code");
                        }

						break;
				}
			}
		}
		catch (Exception e)
		{
			serverConnection.close();
			stop();
		}
	}
}