package com.wherehoo;

import java.awt.geom.Point2D;
import java.io.*;
import java.net.Socket;
import java.util.Calendar;
import java.util.StringTokenizer;

public class WHDataCollector{

    private int verbosity_level;
    
    private Socket client_socket;
    private String act;
    private String idt;
    private String sha;
    private String uniqueidSHA;
    private String mimetype;
    private String protocol;
    private String shape;
    private String meta;
    
    private Point2D.Double[] coordinates;
    private double height;
    private WHPolygon ll_poly;
    private double width;
    private double length;
    private double heading;
    private double radius;
    private double project_heading;
    private double project_range;
    private int limit;      
    private byte[] data;
    private byte[] dataSHA;
    
    
    private static int[]deltabeg ={0,0,0,0,0,0}; // delta values: yy,mo,dd,hh,mi,ss
    private static int[]deltaend ={0,0,0,0,0,0}; // delta values: yy,mo,dd,hh,mi,ss
    private static int[]deltamin = {0,0,0,0,0,WHConstants.RECORD_MIN_LIFE}; // minimum delta for a record that hapiration date
    private Calendar begin;
    private Calendar end; // offset to when this record expires, starting "now"
    private Calendar basetime; // the "now" value used to set beg and end using the deltas provided
    
    private boolean delta_end_is_set;
    private boolean delta_beg_is_set;
    private boolean project_is_set;
    private boolean limit_is_set;
    private boolean mimetype_is_set;
    private boolean protocol_is_set;
    private boolean meta_is_set;
    
    private ErrorFields errors;
    private PrintWriter out;
    private BufferedReader in;
    
    public WHDataCollector(Socket _clientSocket){
  
	client_socket=_clientSocket;
	delta_end_is_set = false;
	delta_beg_is_set = false;
	project_is_set = false;
	limit_is_set = false;
	mimetype_is_set = false;
	protocol_is_set = false;
	meta_is_set = false;
	
	coordinates=new Point2D.Double[0];
	
	errors=new ErrorFields();
	verbosity_level = 0;
    }

    public void setVerbose(int level){
	
	verbosity_level=level;
	if (verbosity_level>0) System.out.println("Verbosity set to "+level);
    }
    
    public int getVerbose(){
	return verbosity_level;
    }
	
    

    public boolean readDataFromClient(){
	boolean result=false;
	try {
	    PrintWriter out = new PrintWriter(client_socket.getOutputStream(),true);
	    BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
	    
	    String line;
	    String cmd;
 
	    if (verbosity_level>0)
		if (verbosity_level>1) System.out.println("READING DATA FROM CLIENT:");
	    do {
		//read one line sent by the client
		line = in.readLine();
		//interpret the command
		cmd= this.interpretLine(line,in,out);
		if (verbosity_level>1) System.out.println("returned command: "+cmd);
	    } while (! cmd.equals(".") && ! cmd.equals("bye"));
	    //return false if the client wishes to disconnect or there was a fatal error
	    if (verbosity_level>1) System.out.println("got out of the loop");
	    if (cmd.equals(".")){
		result = true;
	    }
	    if (verbosity_level>1) System.out.println("result "+result);
	    //in.close();
	} catch (IOException ioe){}
	//out.close();
	return result;
    }
    
    public WHOperation createWHOperation() throws BadCommandException, IOException{
	
	//PrintWriter out = new PrintWriter(client_socket.getOutputStream(),true);
	//verify that we have consistent data
	if (verbosity_level>1) System.out.println("Started to create WHOperation, about to check data");
	this.checkAndProcessData();
	//out.println("OK");
	
	//create an appropriate WHOperation object and return
	if (act.equals("insert")){
	    WHInsert whi= new WHInsert(idt,ll_poly,height,data,begin,end,mimetype,protocol);
	    if (meta_is_set)
		whi.setMeta(meta);
	    return whi;
	}
	else {
	    if (act.equals("query")){
		WHSearch whq= new WHSearch(ll_poly,coordinates[0],height);
		if (delta_beg_is_set)
		    whq.setBegin(begin);
		if (delta_end_is_set)
		    whq.setEnd(end);
		if (limit_is_set)
		    whq.setLimit(limit);
		if (meta_is_set)
		    whq.setMeta(meta);
		if (mimetype_is_set)
		    whq.setMimetype(mimetype);
		if (protocol_is_set)
		    whq.setProtocol(protocol);
		return whq;
	    } 
	    else { 
		if (act.equals("count")){
		    WHCount whc= new WHCount(ll_poly,coordinates[0],height);
		    if (delta_beg_is_set)
		    whc.setBegin(begin);
		    if (delta_end_is_set)
			whc.setEnd(end);
		    if (limit_is_set)
			whc.setLimit(limit);
		    if (meta_is_set)
			whc.setMeta(meta);
		    if (mimetype_is_set)
			whc.setMimetype(mimetype);
		    if (protocol_is_set)
			whc.setProtocol(protocol);
		    return whc;
		}    
		else 
		    return new WHDelete(uniqueidSHA);
	    }
	}
    }
    
    private void checkAndProcessData() throws BadCommandException {
	
	String bad_variables=new String();
	
	if (verbosity_level>1){
	    if (verbosity_level>1) System.out.println("CHECKING THE COLLECTED DATA");
	    if (verbosity_level>1) System.out.println("ERROR FIELDS");
	    if (verbosity_level>1) System.out.println(errors.toString());
	    if (verbosity_level>1) System.out.println("WHDATACOLLECTOR FIELDS:");
	    if (verbosity_level>1) System.out.println(this.toString());
	}
	
	if (errors.act){
	    throw new BadCommandException("ACT");
	} else {
	    //
	    //INSERT
	    //
	    if (act.equals("insert")){
		//check idt
		if ((errors.idt)||(! WHDatabase.checkUser(idt)))
		    bad_variables+="IDT ";
		if (verbosity_level>1) System.out.println("checked idt");
		//check mimetype
		if (errors.mim)
		    bad_variables+="MIM ";
		if (verbosity_level>1) System.out.println("checked mim");
		//check protocol
		if ((errors.pro)||(! WHDatabase.checkProtocol(protocol)))
		    bad_variables+="PRO ";
		if (verbosity_level>1) System.out.println("checked pro");
		//check pjt
		if ((errors.pjt)&&(project_is_set))
		    bad_variables+="PJT ";
		if (verbosity_level>1) System.out.println("checked pjt");
		//check llh
		if (errors.llh)
		    bad_variables+="LLH ";
		if (verbosity_level>1) System.out.println("checked llh");
		
		//set the basetime to "now"
		basetime=Calendar.getInstance();
		basetime.setTime(new java.util.Date());
		if (verbosity_level>1) System.out.println("set the basetime");
		//check beg
		if (errors.beg){
		    bad_variables+="BEG ";
		} else {
		    if (delta_beg_is_set) 
			begin = WHCalendar.addAllDates(basetime,deltabeg);
		    else 
			begin = basetime;
		}
		if (verbosity_level>1) System.out.println("Checked begin");
		//check end
		if (errors.end){
		    bad_variables+="END ";
		} else {
		    if(!errors.beg){
			Calendar endMin = Calendar.getInstance();
			endMin = WHCalendar.addAllDates(begin,deltamin);
			
			if (! delta_end_is_set) {
			    end=Calendar.getInstance();
			    end.set(9999,11,31,23,59,59); // default END is a really long time from now
			} else { // an offset was provided, so set a real end time
			    end = WHCalendar.addAllDates(basetime,deltaend);
			}
			if(end.getTime().before(endMin.getTime()))
			    bad_variables+="END ";
			if (!(begin.getTime().before(end.getTime())))
			    bad_variables+="BEG";	
		    }
		}
		    if (verbosity_level>1) System.out.println("Checked end");
		//check data
		if (errors.dat){
		    bad_variables+="DAT";
		} else {
		    if (! errors.idt){
			if (!WHDatabase.checkSignature(data,dataSHA,idt)) { 
			    bad_variables+= "SHA "; 
			}    
		    }
		}
		if (bad_variables.length()!=0)
		    throw new BadCommandException(bad_variables.trim());
		else {
		    //create a polygon object representing the inserted area
		    ll_poly=new WHPolygon(coordinates);
		    //if project set, project
		    if (project_is_set)
			ll_poly.project(project_heading,project_range);
		}
	    }
		
	    //
	    //QUERY AND COUNT
	    //
	    if (act.equals("query")||act.equals("count")){
		//check limit
		if ((limit_is_set)&&(errors.lim))
		    bad_variables+="LIM ";
		if (verbosity_level>1) System.out.println("limit checked");
		//check search area
		if (errors.shp){
		    bad_variables+="SHP ";
		    if (verbosity_level>1) System.out.println("shape checked, errors");
		} else {
		    if (verbosity_level>1) System.out.println("shape checked, no errors");
		    if (shape.equals("rect_ctr")){
			if (verbosity_level>1) System.out.println("shape rectangel-center");
			//check radius
			if (errors.rad)
			    bad_variables+="RAD ";
			if (verbosity_level>1) System.out.println("radius checked");
		    } else {
			if (verbosity_level>1) System.out.println("shape rectangle forward");
			//check heading
			if (errors.hdg)
			    bad_variables+="HDG ";
			if (verbosity_level>1) System.out.println("heading checked");
			//check width
			if (errors.wid)
			    bad_variables+="WID ";
			if (verbosity_level>1) System.out.println("width checked");
			//check length
			if (errors.len)
			    bad_variables+="LEN ";
			if (verbosity_level>1) System.out.println("length checked");
		    } 
		}
		//check mimetype
		if ((errors.mim)&&(mimetype_is_set))
		    bad_variables+="MIM "; 
		if (verbosity_level>1) System.out.println("mimetype checked");
		//check protocol
		if (((errors.pro)||(!WHDatabase.checkProtocol(protocol)))&&(protocol_is_set))
		    bad_variables+="PRO ";
		if (verbosity_level>1) System.out.println("protocol checked");
		//set the basetime to "now"
		basetime = Calendar.getInstance();
		basetime.setTime(new java.util.Date());	    
		if (verbosity_level>1) System.out.println("basetime set to now");
		//check beg
		if (errors.beg){
		    bad_variables+="BEG ";
		}
		if (errors.end){
		    bad_variables+="END ";
		}
		if ((delta_end_is_set)&&(!(errors.beg || errors.end))){
		    end = WHCalendar.addAllDates(basetime,deltaend);
		    //set the begin time
		    if (delta_beg_is_set)
			begin = WHCalendar.addAllDates(basetime, deltabeg);
		    else 
			begin = basetime;
		    //check if begin time before end time 
		    //check if there is a minimum interval between the begin and end
		    
		    Calendar endMin = Calendar.getInstance();
		    endMin = WHCalendar.addAllDates(begin,deltamin);
		    if ((end.getTime().before(endMin.getTime()))||(end.getTime().before(begin.getTime())))
			bad_variables+="END ";	
		}
		//check pjt 
		if ((errors.pjt)&&(project_is_set))
		    bad_variables+="PJT ";
		
		//check llh, should be a point
		//then set search area description string
		if ((errors.llh)||(coordinates.length!=1))
		    bad_variables+="LLH ";
		//throw exception if there were any errors
		if (bad_variables.length()!=0)
		    throw new BadCommandException(bad_variables.trim());
		else {
		    //create a search area polygon
		    if (shape.equals("rect_fwd"))
			ll_poly=new WHPolygon(coordinates[0],heading,width,length);
		    else
			ll_poly=new WHPolygon(coordinates[0],radius);
		    if (verbosity_level>-1){
			System.out.println("Search polygon: "+ll_poly.toString());
		    }
		    //if project is set, project it
		    if (project_is_set)
			ll_poly.project(project_heading,project_range);
		}
	    }
	    
	    //DELETE
	    if (act.equals("delete")){
		//check idt
		if ((errors.idt)||(! WHDatabase.checkUser(idt)))
		    bad_variables += "IDT ";
		//check uid
		if ((errors.uid)||(! WHDatabase.checkUID(uniqueidSHA)))
		    bad_variables += "UID ";
		//throw exception if there were any errors
		if (bad_variables.length()!=0)
		    throw new BadCommandException(bad_variables.trim());
	    }
	}
	if (verbosity_level>0)
	    System.out.println("BAD VARIABLES : "+bad_variables);
    }
	
	
    private String interpretLine(String line,BufferedReader in, PrintWriter out){

	StringTokenizer line_tokens;
	StringTokenizer param_tokens;
	
	// get the command portion
	String cmd;
	String param;

	if (verbosity_level>1)
	    System.out.println("INTERPRETING LINE: "+line);


	line_tokens = new StringTokenizer(line," ",false);

	if (line_tokens.hasMoreTokens()) {
	    cmd=((String)line_tokens.nextToken()).toLowerCase(); 
	} else { 
	    cmd="";
	}
	if (verbosity_level>1) System.out.println("COMMAND: "+cmd);
	// get the parameters
	if (line_tokens.hasMoreTokens()) {
	    param = (String) line.substring(cmd.length()).trim();
	} else {
	    param = "";
	}
	if (verbosity_level>1) System.out.println("PARAMS: "+param);

	//set appropriate variables
	switch(WHConstants.commandIndex(cmd)){
	    
	    //IDT
	case 0:
	    idt=param.toLowerCase();
	    errors.idt=(idt.equals("") || (idt.length() > WHConstants.MAXIDT));
	    out.println("wherehoo_server "+WHConstants.VERSION+" "+WHConstants.RXTIMEOUT/1000+" "+WHConstants.RECORD_MIN_LIFE+" "+WHConstants.MAXMETA+" "+WHConstants.MAXDATA);
	    if (verbosity_level>1) 
		System.out.println("wherehoo_server "+WHConstants.VERSION+" "+WHConstants.RXTIMEOUT/1000+" "+WHConstants.RECORD_MIN_LIFE+" "+WHConstants.MAXMETA+" "+WHConstants.MAXDATA);
	    if (verbosity_level>1) System.out.println("Interpreting IDT ="+idt);
	    return cmd;
	    
	    //SHA
	case 1:
	    sha=param;
	    if (verbosity_level>1) System.out.println("Interpreting SHA ="+sha);
	    return cmd;
	    
	    //ACT
	case 2:
	    act = param.toLowerCase();
	    errors.act=(!(act.equals("insert")||act.equals("count")||act.equals("delete")||act.equals("query")));
	    if (verbosity_level>0) System.out.println("Interpreting ACT ="+act);
	    return cmd;
	    
	    //LLH
	case 3:
	    //read the values for lat, lon, h into respective vectors
	    param_tokens= new StringTokenizer(param);
	    height=-1;
	    if (verbosity_level>0) System.out.println("Interpreting LLH: "+param);
	    try {
		Point2D.Double[] temp_array=new Point2D.Double[0];
		coordinates= new Point2D.Double[0];
		double temp_height;
		int counter=0;
		do {
		    //System.out.println("create temp_array");
		    temp_array= new Point2D.Double[counter+1];
		    //copy coordinates to temp_array
		    System.arraycopy(coordinates,0,temp_array,0,counter);				     
		    //System.out.println("read next set of coordinates into temp_array and temp_height");
		    //read latitudude and longitude
		    temp_array[counter]=new Point2D.Double(
							   Double.parseDouble(param_tokens.nextToken()),
							   Double.parseDouble(param_tokens.nextToken()));
		    //convert to proper wherehoo ranges
		    //System.out.println("before wh conversion");
		    WHGeo.toWHFormat(temp_array[counter]);
		    //System.out.println("got the point");
		    //read height
		    temp_height=(Double.parseDouble(param_tokens.nextToken()));
		    if (counter==0)
			height=temp_height;
		    //check if the height is the same as in previous point that was read
		    if (height!=temp_height)
			throw new BadCommandException();
		    //create new coordinates array, one element larger than before
		    coordinates= new Point2D.Double[counter+1];
		    //copy temp_array into coordinates
		    System.arraycopy(temp_array,0,coordinates,0,counter+1);
		    //increase counter
		    if (verbosity_level>1) System.out.println("COORDINATES: "+coordinates.toString());
		    if (verbosity_level>1) System.out.println("HEIGHT: "+height);
		    counter=counter+1;
		} while (param_tokens.hasMoreElements());
		errors.llh = false;
	    } catch (Exception e){
		errors.llh=true;
	    }
	    return cmd;
	    
	    //BEG
	case 4:

	    param_tokens = new StringTokenizer(param); 
	    delta_beg_is_set = true;
	    if (verbosity_level>1) System.out.println("Interpreting BEG "+param);
	    try {  
		deltabeg[0]=Integer.parseInt(param_tokens.nextToken()); 
		deltabeg[1]=Integer.parseInt(param_tokens.nextToken()); 
		deltabeg[2]=Integer.parseInt(param_tokens.nextToken()); 
		deltabeg[3]=Integer.parseInt(param_tokens.nextToken()); 
		deltabeg[4]=Integer.parseInt(param_tokens.nextToken()); 
		deltabeg[5]=Integer.parseInt(param_tokens.nextToken());
		errors.beg=false;
	    } catch(Exception e) { 
		errors.beg = true; 
	    }
	    return cmd;
	    
	    //END
	case 5:
	    param_tokens = new StringTokenizer(param); 
	    delta_end_is_set = true;
	    try {  
		deltaend[0]=Integer.parseInt(param_tokens.nextToken()); 
		deltaend[1]=Integer.parseInt(param_tokens.nextToken()); 
		deltaend[2]=Integer.parseInt(param_tokens.nextToken()); 
		deltaend[3]=Integer.parseInt(param_tokens.nextToken()); 
		deltaend[4]=Integer.parseInt(param_tokens.nextToken()); 
		deltaend[5]=Integer.parseInt(param_tokens.nextToken());
		errors.end = false;
	    } catch(Exception e) { 
		errors.end = true; 
	    }
	    return cmd;
	    
	    //HDG 
	case 6:
	    if (verbosity_level>1) System.out.println("Interpreting HDG ="+param);
	    try{
		heading = (((Double.parseDouble(param))%360)+360)%360;
		errors.hdg = false;
	    } catch(NumberFormatException nfe) {
		errors.hdg = true;
	    }
	    return cmd;
	    
	    //LEN
	case 7:
	    if (verbosity_level>1) System.out.println("Interpreting LEN ="+param);
	    try{
		length = Float.parseFloat(param);
		errors.len = (length<1);
	    } catch (NumberFormatException nfe){
		errors.len = true;
	    }
	    return cmd;
	    
	    //LIM
	case 8:
	    if (verbosity_level>1) System.out.println("Interpreting LIM ="+param);
	    try{
		limit_is_set = true;
		limit = Integer.parseInt(param);
		errors.lim = (limit<0);
	    } catch (NumberFormatException nfe){
		errors.lim = true;
	    }
	    return cmd;
	    
	    //MET
	case 9:
	    if (verbosity_level>1) System.out.println("Interpreting MET ="+param);
	    meta_is_set = true;
	    meta = param.substring(0, Math.min(param.length(),WHConstants.MAXMETA)).trim();
	    return cmd;
	    
	    //MIM
	case 10:
	    if (verbosity_level>1) System.out.println("Interpreting MIM ="+param);
	    mimetype_is_set=true;
	    mimetype=param.toLowerCase().trim();
	    errors.mim=(mimetype.equals(""));
	    return cmd;
	    
	    //PJT
	case 11:
	    if (verbosity_level>1) System.out.println("Interpreting PJT ="+param);
	    try {
		project_is_set=true;
		param_tokens = new StringTokenizer(param); 
		//normalize the project angle
		project_heading = (((Float.parseFloat(param_tokens.nextToken()))%360)+360)%360; 
		project_range = Float.parseFloat(param_tokens.nextToken());
		errors.pjt=(project_range<0);
	    } catch(Exception e) {
		errors.pjt = true;
	    }
	    return cmd;
	      
	    //PRO
	case 12:
	    if (verbosity_level>1) System.out.println("Interpreting PRO ="+param);
	    protocol_is_set=true;
	    protocol=param.toUpperCase();
	    errors.pro=false;
	    return cmd;
	    
	    //RAD is a shortcut to set WID = LEN = RAD
	case 13:
	    if (verbosity_level>1) System.out.println("Interpreting RAD ="+param);
	    try{
		radius=Float.parseFloat(param);
		width=radius;
		length=radius;
		errors.rad = (radius<1);
		errors.wid=errors.rad;
		errors.len=errors.rad;
	    } catch (NumberFormatException nfe){
	    errors.rad = true;
	    }
	    return cmd;
	    
	    //SHP
	case 14:
	    if (verbosity_level>1) System.out.println("Interpreting SHP ="+param);
	    shape=param.toLowerCase();
	    errors.shp= (!(shape.equals("rect_fwd")||shape.equals("rect_ctr")));
	    return cmd;
	    
	    //WID
	case 15:
	    if (verbosity_level>1) System.out.println("Interpreting WID ="+param);
	    try{
		width=Float.parseFloat(param);
		//set the error field true if width is less than 1 
		errors.wid = (width<1);
	       
	    } catch (NumberFormatException nfe){
		errors.wid = true;
	    }
	    return cmd;
	    
	    //DAT
	case 16:
	    if (verbosity_level>1) System.out.println("Interpreting DAT ");
	    int datalen;
	    try { 
		datalen = Integer.parseInt(param); 
	    } catch(NumberFormatException nfe) {
		errors.dat = true;
		return cmd;
	    }
	    if (verbosity_level>1) System.out.println("got datalen");
	    if ((datalen < 1) || (datalen > WHConstants.MAXDATA)){
		errors.dat = true;
		return cmd; 
	    } else {
		try{
		    data = new byte[datalen];
		    dataSHA = new byte[WHConstants.SIGNATUREBYTECOUNT]; 
		    //read the data
		    for (int _j=0; _j < datalen; _j++) {
			int _timer = WHConstants.RXTIMEOUT;
			while (! in.ready()) { 
			    if (verbosity_level>1) System.out.println("not ready");
			    try { Thread.sleep(WHConstants.RXLOOPTIMEOUT);
			    } catch (Exception e) { }
			    _timer -= WHConstants.RXLOOPTIMEOUT;
			    if (_timer < 0) {
				errors.dat = true;
				throw new java.io.InterruptedIOException(); 
			    }
			}
			data[_j] = (byte) in.read();
		    }
		    if (verbosity_level>1) System.out.println("read data");
		    //read the signature	
		    for (int _j=0; _j < WHConstants.SIGNATUREBYTECOUNT; _j++) {
			int _timer = WHConstants.RXTIMEOUT;
			while (! in.ready()) { 
			    try { Thread.sleep(WHConstants.RXLOOPTIMEOUT);
			    } catch (Exception e) { } 
			    _timer -= WHConstants.RXLOOPTIMEOUT;
			    if (_timer < 0) {
				errors.dat = true;
				throw new java.io.InterruptedIOException(); 
			    }
			}
			dataSHA[_j] = (byte) in.read();
		    }
		    if (verbosity_level>1) System.out.println("read signature");
		    out.println("ACK");
		    if (verbosity_level>1) System.out.println("printed ack");
		    errors.dat = false;
		} catch (Exception e){
		    errors.dat = true;
		}
		if (verbosity_level>1) System.out.println("Returning cmd");
		if (verbosity_level>1) System.out.println("Data :"+data.length);
		if (verbosity_level>1) System.out.println("SHA :"+dataSHA.length);
		return cmd;
	    }
	    //UID
	case 17:
	    if (verbosity_level>1) System.out.println("Interpreting UID ="+param);
	    uniqueidSHA=param;
	    errors.uid=false;
	    return cmd;
	    
	    //DBG
	case 18:
	    if (verbosity_level>1) System.out.println("Interpreting DBG");
	    this.setVerbose(1);
	    return "dbg";
	    
	    //NOP
	case 19:
	    if (verbosity_level>1) System.out.println("Interpreting NOP");
	    out.println("ACK");
	    return cmd;
	    
	    //BYE
	case 20:
	    if (verbosity_level>1) System.out.println("Interpreting BYE");
	    return cmd;
	    //.
	case 21:
	    if (verbosity_level>1) System.out.println("Interpreting  . ");
	    return cmd;
	    
	    //COMMAND NOT RECOGNIZED
	default:
		return ""; // jim 12-17-01
//	    return "bye";
	}
    }
    public String toString(){
	String s=new String();
	s+="ACT: "+act;
	s+=",  IDT: "+idt;
	s+=",  SHA: "+sha;
	s+=",  UNIQUEIDSHA: "+uniqueidSHA;
	s+=",  MIMETYPE: "+mimetype;
	s+=",  PROTOCOL: "+protocol;
	s+=",  SHAPE: "+shape;
	s+=",  META: "+meta;
	
	s+=",  COORDINATES: "+coordinates.toString();
	s+=",  HEIGHT: "+height;
	s+=",  WIDTH: "+width;
	s+=",  LENGTH: "+length;
	s+=",  HEADING: "+heading;
	s+=",  RADIUS: "+radius;
	s+=",  PROJECT: "+project_heading;
	s+=",  PROJECT RANGE: "+project_range;
	s+=",  LIMIT: "+limit;
	return s;
    }

}
   
   
    



