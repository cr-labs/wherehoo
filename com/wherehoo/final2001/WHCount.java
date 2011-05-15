package com.wherehoo.final2001;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.awt.geom.Point2D;


public class WHCount extends WHOperation {
    private WHPolygon search_poly;
    private double height;
    private byte[] data;
    private Calendar begin;
    private boolean begin_is_set;
    private Calendar end;
    private boolean end_is_set;
    private String mimetype;
    private boolean mimetype_is_set;
    private String protocol;
    private boolean protocol_is_set;
    private String meta;
    private boolean meta_is_set;
    private String metastatus;
    private int limit;
    private boolean limit_is_set;
    private Point2D.Double client_location;
    private int verbosity_level;

    public WHCount(WHPolygon _poly, Point2D.Double _client_location, double _height){
	
	
	search_poly=_poly;
	height=_height;
	meta_is_set=false;
	limit_is_set=false;
	begin_is_set=false;
	end_is_set=false;
	mimetype_is_set=false;
	protocol_is_set=false;

	client_location=_client_location;

	mimetype="";
	protocol="";
	meta="";
	verbosity_level = 0;
    }
    
    public void setMeta(String _meta){
	meta=_meta;
	meta_is_set=true;
    }

    public void setLimit(int _limit){
	limit = _limit;
	limit_is_set=true;
    }

    public void setBegin(Calendar _begin){
	begin = _begin;
	begin_is_set=true;
    }

    public void setEnd(Calendar _end){
	end=_end;
	end_is_set=true;
    }

    public void setMimetype(String _mimetype){
	mimetype=_mimetype;
	mimetype_is_set=true;
    }

    public void setProtocol(String _protocol){
	protocol=_protocol;
	protocol_is_set=true;
    }
    public void setVerbose(int level){
	verbosity_level = level;
	if (verbosity_level > 0) System.out.println("Verbosity set to :"+level);
    }

    public String getQueryString(){
	String queryString;
	//must check for zero-crossing, in which case we will have two adjacent search polies
	if (verbosity_level > 1) System.out.println("Composing SQL query string");
	
	queryString  = "select count(distinct uniqueidsha) ";
	queryString += "from wherehoo_polygons where "; 

	if (! search_poly.zeroCrossing()){
	    if (verbosity_level>1) System.out.println("No zero crossing");
	    //it either intersects
	    queryString += "poly_overlap(polygon(pclose(path'"+search_poly.toString()+"')),area) ";	 
	}	
	else {
	    if (verbosity_level>1) System.out.println("Zero crossing");
	    WHPolygon[] search_polies = search_poly.splitAlongGreatMeridian();
	    queryString += "(poly_overlap(polygon(pclose(path'"+search_polies[0].toString()+"')),area) ";
	    queryString += " OR poly_overlap(polygon(pclose(path'"+search_polies[1].toString()+"')),area)) ";
	}

	queryString +=  (begin_is_set) ? " AND begin_time >= '"+WHCalendar.calToTimeStamp(begin)+"' " 
	    : "AND begin_time <= now() ";
	queryString += (end_is_set) ? "AND end_time <= '"+WHCalendar.calToTimeStamp(end)+"' " 
	    : "AND end_time >= now() ";	
	queryString += (meta_is_set) ? "AND meta like '%"+meta+"%' " : "";
	queryString += (protocol_is_set) ? "AND protocol like '"+protocol+"' " : "";
	queryString += (mimetype_is_set) ? "AND mimetype like '"+mimetype+"' " : "";
	queryString += (limit==0) ? " " : "limit "+limit+" ";
       
	if (verbosity_level > 0) System.out.println("The query:");
	if (verbosity_level > 0) System.out.println(queryString);
	return queryString;
	
    }
    public synchronized void executeAndOutputToClient(Socket client_socket) throws IOException{
	
	ResultSet rs=null;
	Connection C;
	Statement s;
	PrintWriter out;
	String queryString;
	int count;
	
	out = new PrintWriter(client_socket.getOutputStream(),true);
	try{
            C = DriverManager.getConnection("jdbc:"+WHConstants.DB,"postgres","");
	    C.setAutoCommit(false);
	    
	    queryString=this.getQueryString();
	    
	    s = C.createStatement();
	    rs = s.executeQuery(queryString);
	    C.commit();
	    if(rs.next()){
		count=rs.getInt("count");
	    } else {
		count=0;
	    }
	    out.println(count);
	    //out.close();
	}
	catch (SQLException sqle) {
	    System.out.println("SQLException: " + sqle.getMessage());
	    System.out.println("SQLState:     " + sqle.getSQLState());
	    System.out.println("VendorError:  " + sqle.getErrorCode());
	}
    }
   
    public String toString(){
	String s="Poly: "+search_poly.toString()+"\n";
	s+="Height "+height+"\n";
	if (begin_is_set)
	    s+="Begin "+begin.toString()+"\n";
	else
	    s+="Begin not set\n";
	if(end_is_set)
	    s+="End "+end.toString()+"\n";
	else
	    s+="End not set\n";
	if (mimetype_is_set)
	    s+="Mimetype "+mimetype+"\n";
	else
	    s+="Mimetype not set\n";
	if(protocol_is_set)
	    s+="Protocol "+protocol+"\n";
	else
	    s+="Protocol not set\n";
	if (meta_is_set)
	    s+="Meta "+meta;
	else
	    s+="Meta not set";
	return s;
    } 
}
