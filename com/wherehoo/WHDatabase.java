package com.wherehoo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

abstract class WHDatabase{

    private static synchronized boolean checkTable(String tablename, String fieldname, String fieldvalue) throws SQLException{
	String tn="";
	String fn="";
	ResultSet rs=null;
	boolean result=false;
	String queryString;
       
	Connection C = DriverManager.getConnection("jdbc:"+WHConstants.DB,"postgres","");
	Statement stmt = C.createStatement();
	// construct and run a SELECT query
	queryString  = "select * from "+tablename+" ";
	queryString += "where "+fieldname+"='";
	queryString += fieldvalue;
	queryString += "' ";
	rs = stmt.executeQuery(queryString);
	result = rs.next();
	C.close();
	return (result);
    }
    
    
    private static synchronized String fetchFromTable(String tablename, String keyfieldname, String keyfieldvalue, String targetfieldname) throws SQLException{
	ResultSet rs=null;
	String result="";
	String queryString;
	Connection C;
	
	C = DriverManager.getConnection("jdbc:"+WHConstants.DB,"postgres","");
	Statement stmt = C.createStatement();
	queryString  = "select "+targetfieldname+" from "+tablename+" ";
	queryString += "where "+keyfieldname+"='";
	queryString += keyfieldvalue;
	queryString += "' ";
	rs = stmt.executeQuery(queryString);
	//System.out.println("fetchFromTable query string: "+queryString); 
	if (rs.next()) { 
	    result = rs.getString(targetfieldname); 
	}
	C.close();
	return (result) ;
    }
    protected static synchronized boolean checkUser(String idt){
	try {
	    return checkTable("users","userid",idt);
	}
	catch (SQLException sqle){
	    return false;
	}
    }
    protected static synchronized boolean checkProtocol(String pro){
	try{
	    return checkTable("protocol","protocol",pro);
	}
	catch (SQLException sqle){
	    return false;
	}
    }
    protected static synchronized boolean checkSignature(byte[] data, byte[] datSHA, String idt){
	//to compare the sha-1 of data[] calculated using user secret fetched from the table in database
	try{
	    byte[] databaseSHA;
	    databaseSHA = WHDatabase.SHAhash(data,fetchFromTable("users","userid",idt,"secret"));
	    return (java.util.Arrays.equals(databaseSHA,datSHA));
	}
	catch (SQLException sqle){
	    System.out.println("SQLException : "+sqle.getMessage());
	    return false;
	}
    }
    protected static synchronized boolean checkUID(String uid){
	try {
	    return checkTable("wherehoo_polygons","uniqueidsha",uid);
	}
	catch (SQLException sqle){
	    return false;
	}
    }

    private static byte[] SHAhash(byte[] data, String mysecret) {
	byte[] mdfinal;
	try {
	    MessageDigest md = MessageDigest.getInstance("SHA-1");
	    md.update(data); 				// the data  and...
	    md.update(mysecret.getBytes()); // my secret
	    mdfinal = md.digest(); 			// yield the secure hash
	}
	catch (NoSuchAlgorithmException nsae) { return null; }	
	return mdfinal;
    }
}



