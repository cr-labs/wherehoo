package com.wherehoo;
// WHEREHOO SERVER 

// COPYRIGHT (c) 2000, Jim Youll and MIT Media Laboratory, Cambridge, MA
// all rights reserved. Thou shalt not steal, and all that.
// this handles an individual client connection

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

class ClientProcess extends Thread {
    
    private Socket client_socket;
    private WHDataCollector data_collector;
    private WHOperation operation;
   
    // constructor for the new process
    ClientProcess (Socket s) { 
	client_socket = s; 
    }
    
    public void run() {
	try {
	    PrintWriter out=new PrintWriter(client_socket.getOutputStream(),true);
	    data_collector=new WHDataCollector(client_socket);
	    data_collector.setVerbose(0);
	    if (data_collector.readDataFromClient()){
		//System.out.println("sucessfully read data from client");
		try {
		    operation=data_collector.createWHOperation();
		    operation.setVerbose(data_collector.getVerbose());
		    //System.out.println("Created WHOperation");
		    out.println("OK");
		    operation.executeAndOutputToClient(client_socket);
		}
		catch (BadCommandException bce){	    
		    System.out.println("NAK "+bce.getMessage());
		    out.println("NAK "+bce.getMessage());
		}
		catch (Exception e){
		    System.out.println(e.toString());
		}
	    }
	    System.out.println("Client disconnecting");
	    out.println(".");
	    out.println("BYE");
	    out.close();
	} catch (IOException ioe){
	    System.out.println("IOE Exception: "+ioe.getMessage());
	}
    }
}
	

