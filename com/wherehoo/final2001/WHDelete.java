package com.wherehoo.final2001;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;


public class WHDelete extends WHOperation {
    
    private String uid;
    private int verbosity_level;

    public WHDelete(String _uid){
	uid=_uid;
	verbosity_level=0;
    }
    
    public synchronized void executeAndOutputToClient(Socket client_socket) throws IOException {
	
	Connection C;
	Statement s;
	PrintWriter out;
	String queryString;
	String result="NAK";

	out = new PrintWriter(client_socket.getOutputStream(),true);
	try {
	    C = DriverManager.getConnection("jdbc:"+WHConstants.DB,"postgres","");
	    C.setAutoCommit(false);
	    
	    // delete an existing record given a valid uniqueidSHA and companion idt (its creator)
	    s = C.createStatement();
	    queryString  = "delete from wherehoo_polygons where ";
	    queryString += "uniqueidSHA ='"+uid+"'";
	    // System.out.println(queryString);
	    if (s.executeUpdate(queryString) == 1)
		result="ACK";
	    C.commit();
	}
	catch (SQLException sqle) {
	    System.out.println("SQLException: " + sqle.getMessage());
	    System.out.println("SQLState:     " + sqle.getSQLState());
	    System.out.println("VendorError:  " + sqle.getErrorCode());
	}
	out.println(result);
	out.close();
    }
    public void setVerbose(int level){
	verbosity_level = level;
    }
}
