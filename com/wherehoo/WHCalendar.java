package com.wherehoo;
import java.util.*;
import java.sql.*;
public class WHCalendar{

    public static Calendar addAllDates(Calendar _cal, int offsets[]) {
	Calendar cal = Calendar.getInstance();
	cal = (Calendar) _cal.clone();
	// Add the offset to the current object
	cal.add(Calendar.YEAR,			offsets[0]);
	cal.add(Calendar.MONTH,			offsets[1]);
	cal.add(Calendar.DAY_OF_MONTH,	offsets[2]);
	cal.add(Calendar.HOUR_OF_DAY,	offsets[3]);
	cal.add(Calendar.MINUTE,		offsets[4]);
	cal.add(Calendar.SECOND,		offsets[5]);
	return cal;
    }
    
    public static Timestamp calToTimeStamp(Calendar cal) {
	String s="";
	s  = cal.get(Calendar.YEAR)+"-"+(cal.get(Calendar.MONTH)+1)+"-"+cal.get(Calendar.DAY_OF_MONTH)+" ";
	s += cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);
	return Timestamp.valueOf(s);
    }
    public static Timestamp retrieveTimestamp(SQLException sqle){
	/*this method is a patch for an apparent bug in SQL driver
	 *it throws exception for every date before 1900-01-01 00:00:00
	 *fortunately, the string description of the timestamp is
	 *included in error message. 
	 *The sample message:
	 *Bad Timestamp Format at 19 in 1888-10-09 00:02:01
	 *This method scrapes "1888-10-09 00:02:01", creates Calendar
	 *and then the timestamp
	 */


	int year,month,day,hour,minute,second;
	long milis;
	String message = sqle.getMessage();
	String ts=message.substring(message.indexOf("in")+2).trim();
	Timestamp t=Timestamp.valueOf(ts);
	return t;
    }
	
}
