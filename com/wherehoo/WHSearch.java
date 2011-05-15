package com.wherehoo;
import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.awt.geom.Point2D;


public class WHSearch extends WHOperation {
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

    public WHSearch(WHPolygon _poly, Point2D.Double _client_location, double _height){
	
	
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

	verbosity_level=0;
    }
    
    public void setMeta(String _meta){
	if (verbosity_level>1) System.out.println("Meta set to: "+_meta);
	meta=_meta;
	meta_is_set=true;
    }

    public void setLimit(int _limit){
	if (verbosity_level>1) System.out.println("Limit set to: "+_limit);
	limit = _limit;
	limit_is_set=true;
    }

    public void setBegin(Calendar _begin){
	if (verbosity_level>1) System.out.println("Begin set");
	begin = _begin;
	begin_is_set=true;
    }

    public void setEnd(Calendar _end){
	if (verbosity_level>1) System.out.println("End set");
	end=_end;
	end_is_set=true;
    }

    public void setMimetype(String _mimetype){
	if (verbosity_level>1) System.out.println("Mimetype set to :"+_mimetype);
	mimetype=_mimetype;
	mimetype_is_set=true;
    }

    public void setProtocol(String _protocol){
	if (verbosity_level>1) System.out.println("Protocol set to: "+_protocol);
	protocol=_protocol;
	protocol_is_set=true;
    }

    public void setVerbose(int level){
	verbosity_level = level;
	if (verbosity_level>1) System.out.println("Verbosity set to "+ level);
    }

    public String getQueryString(){
	String queryString;
	//wherehoo=# select poly from mypolytable where '((0,0),(4,4))' ?# poly or '((0,0),(4,4))' ~ poly;
	//must check for zero-crossing, in which case we will have two adjacent search polies
	if (verbosity_level>1) System.out.println("Composing SQL query string");
	queryString  = "select data,meta,mimetype,protocol,area,height,end_time,uniqueidsha ";
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
       
	if (verbosity_level>0) System.out.println("The query:");
	if (verbosity_level>0) System.out.println(queryString);
	return queryString;
    }
    
    public synchronized void executeAndOutputToClient(Socket client_socket) throws IOException {
	
	ResultSet rs=null;
	Connection C;
	Statement s;
	String queryString;
	
	if (verbosity_level>0) System.out.println("Executing and outputing to client");
	try{
	    if (verbosity_level>1) System.out.println("Connecting to database");
	    C = DriverManager.getConnection("jdbc:"+WHConstants.DB,"postgres","");
	    C.setAutoCommit(false);
	    queryString=this.getQueryString();
	    s = C.createStatement();
	    if (verbosity_level>1) System.out.println("About to execute query");
	    rs = s.executeQuery(queryString);
	    if (verbosity_level>1) System.out.println("Commiting the query");
	    C.commit();
	    if (verbosity_level>1) System.out.println("About to output to client");
	    this.outputToClient(rs,client_socket);
	    C.close();
	}
	catch (SQLException sqle) {
	    System.out.println("SQLException: " + sqle.getMessage());
	    System.out.println("SQLState:     " + sqle.getSQLState());
	    System.out.println("VendorError:  " + sqle.getErrorCode());
	} 
    }
	
    
    private synchronized void outputToClient(ResultSet rs, Socket client_socket) throws IOException {
	WHPolygon poly;
	String mimetype;
	String protocol;
	String meta;
	String meta_status;
	byte[] data;
	long ttl;
	
	double heading;
	double distance;
	String quadrant;
	String client_data_command;
	
	PrintWriter out;
	BufferedOutputStream outstream;
	ByteArrayOutputStream outbytes;
	BufferedReader in;
	
	java.util.Date endtime;
	java.util.Date nowtime;

	if (verbosity_level>1) System.out.println("Opening the PrintWriter");
	out = new PrintWriter(client_socket.getOutputStream(),true);
	if (verbosity_level>1) System.out.println("Opening the BufferedOutputStream");
	outstream = new BufferedOutputStream(client_socket.getOutputStream());
	if (verbosity_level>1) System.out.println("Opening the ByteArrayOutputStream");
	outbytes = new ByteArrayOutputStream();
	if (verbosity_level>1) System.out.println("Opening the BufferedReader");
	in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
	try {
	    if (verbosity_level>1) System.out.println("Outputing the results to the client");
	    client_data_command = new String();
	    while (rs.next() && ! client_data_command.equals("bye")){
		if (verbosity_level>1) System.out.println("Next row");
		poly = new WHPolygon(rs.getString("area"));
		if (verbosity_level>1) System.out.println("got poly from the table");
		mimetype = rs.getString("mimetype");
		if (verbosity_level>1) System.out.println("got mime from the table");
		protocol = rs.getString("protocol");
		if (verbosity_level>1) System.out.println("got protocol from the table");
		meta = rs.getString("meta");
		if (verbosity_level>1) System.out.println("got meta from the table");
		data = rs.getBytes("data");
		
		if (meta.equals("") || (meta==null)) {
		    meta_status = "NONE"; meta="";
		}
		else {
		    meta_status = "META";
		}
		if (verbosity_level>1) System.out.println("determined meta status");
		//this is a big, big problem. Does it always work?
		endtime = rs.getDate("end_time");
		nowtime = new java.util.Date();
		if (verbosity_level>1) System.out.println("got end time from the table");
		ttl = Math.abs(endtime.getTime() - nowtime.getTime()) / 1000L;
		ttl = Math.min(ttl, WHConstants.MAXTTL);
		if (verbosity_level>1) System.out.println("calculated ttl");
		heading=WHGeo.heading(client_location,poly);
		if (verbosity_level>1) System.out.println("calculated heading");
		distance=WHGeo.distance(client_location,poly);
		if (verbosity_level>1) System.out.println("calculated distance");
		quadrant = WHGeo.quadrant(heading);
		if (verbosity_level>1) System.out.println("calculated quadrant");
		out.println(Math.round(heading)+" "+quadrant+" "+Math.round(distance)+" "+ttl+" "+data.length+" "+protocol+" "+mimetype+" "+metastatus);
		if (verbosity_level>1) System.out.println("sent the heading to client");
		if (verbosity_level>0) System.out.println(Math.round(heading)+" "+quadrant+" "+Math.round(distance)+" "+ttl+" "+data.length+" "+protocol+" "+mimetype+" "+metastatus);
		do {   
		    client_data_command = in.readLine().trim().toLowerCase();
		    if (verbosity_level>1) System.out.println("Client sent: "+client_data_command);
		    if(client_data_command.equals("meta")) 
			out.println(meta); 
		    
		    if(client_data_command.equals("data")) {
			outbytes.write(data);
			outbytes.writeTo(outstream);
			outstream.flush();
			outbytes.reset();
		    }
		} while(client_data_command.equals("meta")|| client_data_command.equals("data"));
	    }
	    //out.close();
	    //outbytes.close();
	    //outstream.close();
	    
	} catch(SQLException sqle) {
	    System.out.println(sqle.toString()); 
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
