package chat;

import chat.NetFrame;
import chat.NetFrame.HDLCFrame;
import chat.NetFrame.HDLCFrame.*;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.google.common.net.InetAddresses;

public class DinoTalk extends Applet implements ActionListener, KeyListener, Runnable
{
	private static final long serialVersionUID = 1702635793607554428L;
	
	private String clientId, fromServer, userData, connectionStatus;
	private static final String STATUS_OK = "Connected to a Chat Server!";
	private static final String STATUS_KO = "Not connected to a Chat Server!";
	
	private boolean isConnected = false;
	/**
	 * User input field
	 */
	private JTextField textField;
	/**
	 * Displays server messages.
	 */
	private TextArea textArea;
	private JButton connectButton, msgButton;
	private Font fontb;
	private static JFrame frame;
	
	private Thread listeningThread;
	private InetAddress serverAddress;
	private int serverPort;
	private static final int DEFAULT_SERVER_PORT = 4444;
	private Connection server;
	
	/************************************************************************/
	
	@Override
	public void init()
	{
		textField = new JTextField("", 46);
		textArea = new TextArea("No Messages", 15, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
		textArea.setEditable(false);
		textArea.setEnabled(false);
		connectButton = new JButton("Connect");
		connectButton.setFocusTraversalKeysEnabled(true);
		connectionStatus = STATUS_KO;
		msgButton = new JButton("Send Message");
		msgButton.setEnabled(false);
		JButton closeButton = new JButton("Close");
		closeButton.setFocusTraversalKeysEnabled(true);
		
		fontb = new Font("Arial", Font.BOLD, 14);
		
		textField.addActionListener(this);
		msgButton.addKeyListener(this);
		connectButton.addActionListener(this);
		connectButton.addKeyListener(this);
		closeButton.addActionListener(this);
		closeButton.addKeyListener(this);
		
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
	public void keyPressed(KeyEvent e)
	{
		if(e.getKeyCode() == KeyEvent.VK_ENTER){
		      ((JButton) e.getSource()).doClick();
		}  
	}
	@Override
	public void keyTyped(KeyEvent e){}
	@Override
	public void keyReleased(KeyEvent e){}
	
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
			System.out.println("Attempting disconnect..");
			
			if(isConnected)
			{
				this.disconnect();
			}
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
		else if (arg == "Send Message" || evt.getSource() == textField)
		{
			sendMsg();
		}
		else
		{
			System.out.print(arg);
		}
		
		repaint();
	}
	
	/**
	 * Called when the connect button is pressed.
	 */
	private void sendMsg()
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
			
            connectButton.setEnabled(false);
            msgButton.setEnabled(true);
            msgButton.setFocusTraversalKeysEnabled(true);
            textField.requestFocusInWindow();
            textArea.setEnabled(true);
            textArea.setText("");
            connectionStatus = STATUS_OK;
            
            repaint();
            
			// optional - setting socket timeout to 5 secs
			// this is not necessary because application
			// runs with multiple threads
			// mySocket.setSoTimeout(500);
			
            server = new Connection(serverAddress, serverPort);
            handShake();
            
			// define new listeningThread
			listeningThread = new Thread(this);
			listeningThread.start();
			
            isConnected = true;
		}
		catch (UnknownHostException e)
		{
			connectionStatus = "Bad Host!";
			JOptionPane.showMessageDialog(null, "Don't know about host: " + serverAddress);
		}
		catch (IOException e)
		{
			connectionStatus = STATUS_KO;
			JOptionPane.showMessageDialog(null, "Server is not running!");
		}
		catch (Exception e)
		{
			connectionStatus = "Error connecting to the chat server!";
			JOptionPane.showMessageDialog(null, "Server is not running!");
		}
    }
	
	private void disconnect()
	{
			NetFrame disc = new NetFrame(server.getAddress(), Types.UFrame, Commands.DISC);
			disc.setPollFinal(HDLCFrame.Poll);
			try
			{
				server.send(disc);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			//Wait for clean disconnect
			checkServer();
	}
	
	/**
	 * Main method for the class.
	 * Initializes whole environment.
	 */
	public static void main(String args[])
	{
		// define window and call standard methods
		DinoTalk client = new DinoTalk();
		frame = new JFrame("Dinotalk - Chatting Program");
		client.init();
		client.start(); // client starts listening
		
		frame.add("Center", client);
		frame.setSize(550, 400);
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setResizable(true);
		frame.setLocation(150, 170);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
			listeningThread.interrupt();
			server.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
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
	
	/**
	 * To complete an HDLC handshake.
	 */
	private void handShake() throws SocketException, IOException
    {
		String received = server.read();
        NetFrame snrm = new NetFrame(received);
        
        if (snrm.getFrameType() == Types.UFrame && snrm.getCC() == Commands.SNRM)
        {
            NetFrame ua = new NetFrame(server.getAddress(), Types.UFrame, Commands.UA);
            server.send(ua);
        }
        else
        {
            System.err.println("ERROR : Did not receive SNRM frame from " + server.getAddress());
        }
    }
	
	/**
	 * checkServer - this is a main client algorithm.
	 */
	private void checkServer()
	{
		try
		{
			fromServer = server.read();
			
			if (!fromServer.isEmpty())
			{
				NetFrame recv = new NetFrame(fromServer);
				switch(recv.getFrameType())
				{
					case IFrame:
                        fromServer = recv.getInfo();
                        if (textArea.getText().isEmpty())
                        	textArea.append(fromServer);
                        else
                        	textArea.append("\n"+fromServer);
						break;
						
					case SFrame:
                        switch (recv.getCC())
                        {
                        	case RR:
                        		NetFrame toSend = null;
                                if (!userData.isEmpty()) //Something to send
                                {
                                	toSend = new NetFrame(server.getAddress(), Types.IFrame, userData);
                                    userData = "";
                                }
                                else if (recv.isPoll()) // Request for ack
                                {
                                	toSend = new NetFrame(server.getAddress(), Types.SFrame, Commands.RR);
                                }
                                toSend.setPollFinal(HDLCFrame.Final); //Send ack
                                server.send(toSend); 
                                break;
                        }
						break;
						
					case UFrame:
						switch (recv.getCC())
                        {
                        	case DISC:
                        		System.out.println("Server allowed clean disconnect.");
                        		stop();
                                break;
                        }
						break;
				}
			}
		}
		catch (SocketException | NullPointerException e)
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