package chat;

import java.io.IOException;
import java.net.*;

public class ServerConnection extends Connection
{	
	public ServerConnection(Socket socket) throws IOException
	{
		super(socket);
	}
}