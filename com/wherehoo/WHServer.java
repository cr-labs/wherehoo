package com.wherehoo;
/* whserver.java - wherehoo server for unix port access */

// WHEREHOO SERVER 
// COPYRIGHT (c) 2000, Jim Youll and Massachusetts Institute of Technology, Cambridge, MA
// all rights reserved. Thou shalt not steal, and all that.

import java.io.*;
import java.net.*;
import java.sql.*;


public class WHServer {
	
    private static String server_address;
    private static ServerSocket server_socket;

    public static void main(String[] args) throws IOException {
	
	// load the class for db server access
	try {
	    //System.out.println("load driver");
	    Driver driver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
	    DriverManager.registerDriver(driver);
	    //System.out.println("got the driver");
	  }
	catch (Exception e) {
	    
	    System.err.println("Unable to load SQL driver.");
	    e.printStackTrace();
	}
	

	// setup for incoming socket connections
	server_socket = new ServerSocket(WHConstants.PORT,WHConstants.Q_LEN);
	System.out.println("Wherehoo socket server v"+WHConstants.VERSION+" on port "+WHConstants.PORT);
	server_address = InetAddress.getLocalHost().getHostAddress();
	
	Socket client_socket;
	String client_address;
	// wait for a client connection, then start a new thread to handle it
	while (true) {
	    System.out.println("Host "+server_address+" blocking on accept()");
	    client_socket = server_socket.accept(); // block until next client connection
	    client_address = client_socket.getInetAddress().getHostAddress();
	    System.out.println("connection accepted from "+client_address+" Launching thread.");
	    client_socket.setSoTimeout(WHConstants.RXTIMEOUT);
	    new ClientProcess(client_socket).start();
	}
    }
}
