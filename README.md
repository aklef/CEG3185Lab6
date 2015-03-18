# aklef/CEG3185Lab6
This project virtualises the HDLC data transfer protocol using sliding window frame management.

> MyServer.java is the server,
> 
> MyClientWin.java the client.
 
This repository contains an Eclipse project in Java, to be compiled using the Java 1.7 JDK.
 
You can run the server with a port number as parameter:

    java MyServer 4444

or any other port number, and it will accept connections on that port.
 
The client can run on any other machine that has internet access to the host and a 1.7 JRE with the following command:
 
    java MyClientWin

Or by pressing the "run" button in Eclipse.

Once "connect" is pressed, the client will ask for the IP address of the server, your choice of username and the port number (4444 in our previous example).

If the client enters  nothing and continually enters "ok", the following default values will be used:


    IP: 127.0.0.1 (loopback)
    Port: 4444
	Username: Anonymous{minute of the hour}

