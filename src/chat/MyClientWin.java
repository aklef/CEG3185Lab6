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
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.google.common.net.InetAddresses;

public class MyClientWin extends Applet implements ActionListener, KeyListener, Runnable
{
	private static final long serialVersionUID = 1702635793607554428L;
	
	private String clientId, fromServer, userData, connectionStatus;
	private static final String STATUS_OK = "Connected to a Chat Server!";
	private static final String STATUS_KO = "Not connected to a Chat Server!";
	
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
	private Font fontb;
	private static Frame frame;
	
	private Thread listeningThread;
	private InetAddress serverAddress;
	private int serverPort;
	private static final int DEFAULT_SERVER_PORT = 4444;
	private Connection server;
	
	/************************************************************************/
	
	@Override
	public void init()
	{
		textField = new TextField("", 50);
		textArea = new TextArea("No Messages", 15, 50);
		textArea.setEditable(false);
		textArea.setEnabled(false);
		connectButton = new JButton("Connect");
		connectionStatus = STATUS_KO;
		msgButton = new JButton("Send Message");
		msgButton.setEnabled(false);
		JButton closeButton = new JButton("Close");
		
		fontb = new Font("Arial", Font.BOLD, 14);
		
		textField.addKeyListener(this);
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
		super.paintComponents(g);
		
		if (isConnected)
			g.setColor(Color.GREEN);
		else
			g.setColor(Color.RED);
		
		g.setFont(fontb);
		g.drawString(connectionStatus, 80, 330);
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
				this.server.close();
			}
			System.exit(0);
		}
		// ******************************************
		// Connect button pressed
		// ******************************************
		else if (arg == "Connect")
		{
			connect();
		}
		// ******************************************
		// Send Message button pressed
		// ******************************************
		else if (arg == "Send Message")
		{
			if (!textField.getText().isEmpty())
			{
				//
				// copy content of the message text into
				// internal buffer for later processing
				// only one message can be stored into the
				// buffer
				//
				userData = clientId + ": " + textField.getText();
				textField.setText("");
			}
			else
			{
				userData = "";
			}
		}
		repaint();
	}
	
	/**
	 * Called when the connect button is pressed.
	 */
	private void connect()
    {
		try
		{
			// get server IP and name of client	
			if (InetAddresses.isInetAddress(textField.getText()))
			{
				serverAddress = InetAddresses.forString(textField.getText());
			}
			else
			{
				textField.setText("");
				serverAddress = InetAddress.getByName(JOptionPane.showInputDialog("Enter Chat server IP:", "127.0.0.1"));
			}
			
			clientId = JOptionPane.showInputDialog("Enter your name:", "Anonymous");
			
			try
			{
				serverPort = Integer.parseInt(JOptionPane
						.showInputDialog("Enter port number:", 4444));
			}
			catch (NumberFormatException e)
			{
				serverPort = DEFAULT_SERVER_PORT;
			}
			
			server = new Connection(serverAddress, serverPort);
            isConnected = true;
            msgButton.setEnabled(true);
    		msgButton.requestFocusInWindow();
            connectButton.setEnabled(false);
            textArea.setText("");
            textArea.setEnabled(true);
            connectionStatus = STATUS_OK;
            
            repaint();
            
			// optional - setting socket timeout to 5 secs
			// this is not necessary because application
			// runs with multiple threads
			// mySocket.setSoTimeout(5000);
			
			String received = server.read();
            NetFrame snrm = new NetFrame(received);
            if (snrm.getFrameType() != HDLCFrameTypes.UFrame || snrm.getCC() != HDLCFrameTypes.Commands.SNRM)
            {
                System.out.println("ERROR : Did not receive SNRM frame from " + server.getAddress());
            }
            
            NetFrame ua = new NetFrame(server.getAddress(), HDLCFrameTypes.UFrame, HDLCFrameTypes.Commands.UA);
            server.send(ua);
            
			// define new listeningThread
			listeningThread = new Thread(this);
			listeningThread.start();
		}
		catch (UnknownHostException e)
		{
			isConnected = false;
			connectionStatus = "Bad Host!";
			JOptionPane.showMessageDialog(null, "Don't know about host: " + serverAddress);
		}
		catch (IOException e)
		{
			isConnected = false;
			connectionStatus = STATUS_KO;
			JOptionPane.showMessageDialog(null, "Server is not running!");
		}
		catch (Exception e)
		{
			isConnected = false;
			connectionStatus = "Error connecting to the chat server!";
			JOptionPane.showMessageDialog(null, "Server is not running!");
		}
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
		System.out.println("Stopping!");
		try
		{
			//server.send(new NetFrame(server.getAddress(), HDLCFrameTypes.UFrame, HDLCFrameTypes.Commands.DISC));
			server.close();
			listeningThread.interrupt();
		}
		catch(Exception e){}
		
		System.exit(0);
	}
	
	/**
	 * Run - listeningThread method.
	 * Only executes once the connect button is pressed.
	 */
	@Override
	public void run()
	{
		userData = "";
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
	 * checkServer - this is a main client algorithm.
	 * Calls to the 
	 */
	public void checkServer()
	{
		try
		{
			fromServer = server.read();
			
			if (!fromServer.isEmpty())
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
                        if (receivedFrame.getCC() == HDLCFrameTypes.Commands.RR)
                        {
                            //Something to send
                            if (!userData.isEmpty())
                            {
                                NetFrame toSend = new NetFrame(server.getAddress(), HDLCFrameTypes.IFrame, HDLCFrameTypes.Commands.RR, userData);                
                                toSend.setPollFinal(false);
                                server.send(toSend);
                                userData = "";
                            }
                            //Nothing to send
                            else
                            {
                               NetFrame toSend = new NetFrame(server.getAddress(), HDLCFrameTypes.SFrame, HDLCFrameTypes.Commands.RR); 
                               toSend.setPollFinal(false);
                               server.send(toSend); 
                            }
                        }
                        else
                            System.err.println("Error: did not receive RR control code");
                        
						break;
				}
			}
		}
		catch (SocketException e)
		{
			System.err.println("Server dead! Going down now.");
			stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			stop();
		}
	}
}