package com.wherehoo.final2001;

public class WHConstants{


    public static final double 	VERSION = (double) 2.11;
    protected static final String	copyrightNotice="COPYRIGHT (c) 2000,2001, Jim Youll and The Media Laboratory, Cambridge, MA. all rights reserved";

//    protected static final String	DB = "postgresql://localhost:5432/wherehoo";
//    protected static final String	DB = "postgresql://localhost/wherehoo";
    protected static final String	DB = "postgresql:wherehoo";

    protected static final int	MAXDATA = 65535; //max size of binary data stored in a wherehoo record
    protected static final int	MAXMETA = 1024;  //max size of metadata text field describing the wherehoo data field contents
    protected static final int	MAXTTL = 99999999; // longest TTL that will be reported
    protected static final int 	MAXWIDLEN = 999999; // largest value for RAD, WID or LEN
    protected static final int 	RECORD_MIN_LIFE = 12; // seconds ahead of "now" for min. expiration timestamp on a record being inserted
    protected static final int	MAXIDT = 10; // max length of IDENT field

    // constants for the geocalculations
    protected static final double GEO_A = 6378137.0;
    protected static final double GEO_B = 6356752.3142;
    protected static final double GEO_E = 0.081819184;  		// Eccentricity
    protected static final double GEO_E2 = 0.00669437999013;	// Eccentricity squared
    protected static final double Ra = (GEO_A * Math.sqrt(1 - GEO_E2)); // solve part of R in advance to save cycles
    
    // other constants
    protected static final int	SIGNATUREBYTECOUNT = 20; // number of bytes to read after DATA - containing the signature

    //server constants
    public static final int Q_LEN = 50;
    public static final int PORT  = 5859; // standard port is 5859, test port is 5650
    public static final int RXTIMEOUT = 200000; //msec of silence permitted from client before disconnect
    public static final int RXLOOPTIMEOUT = 1000;
    
    //supported commands
    /* the array contents (index and string)
     * (0 idt)(1 sha)(2 act)(3 llh)(4 beg)(5 end)
     * (6 hdg)(7 len)(8 lim)(9 met)(10 mim)(11 pjt )(12 pro)
     * (13 rad)(14 shp)(15 wid)(16 dat)(17 uid)(18 dbg)(19 nop)(20 bye)(21 .)
     */
    private static final String[] commands={"idt","sha","act","llh","beg","end","hdg","len","lim","met","mim","pjt","pro","rad","shp","wid","dat","uid","dbg","nop","bye","."};	
    
    protected static int commandIndex(String command){
	for (int i=0;i<commands.length;i++){
	    if (command.equals(commands[i]))
		return i;
	}
	return -1;
    }
}
