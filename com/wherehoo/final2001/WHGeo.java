package com.wherehoo.final2001;

import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.math.BigDecimal;

public abstract class WHGeo{
    
     // constants for the geocalculations
    private static final double GEO_A = 6378137.0;
    private static final double GEO_B = 6356752.3142;
    private static final double GEO_E = 0.081819184;  		// Eccentricity
    private static final double GEO_E2 = 0.00669437999013;	// Eccentricity squared
    private static final double Ra = (GEO_A * Math.sqrt(1 - GEO_E2)); // solve part of R in advance to save cycles
    

    /*  requires: origin!=end (will return 0);
     *	origin is not a pole (will return -1, I feel it is overkill to throw an exception);
     *the coordinates follow wherehoo convention 
     *   -latitude between 0 and 360, starting at eastern-hemisphere half-equator and increasing north.
     *   -longitude between 0 and 360, starting at great meridian and increasing eastward.
     *effects: calculates the azimuth
     */
	
    public static double heading(Point2D.Double origin, Point2D.Double end){

	double lat1;
	double lat2;
	double lon1;
	double lon2;

	double dlat;
	double dlon;

	double rlat1;
	double rlat2;
	double rlon1;
	double rlon2;
	
	double drlat;
	double drlon;
	//longitudinal difference translated to angular difference along great circle
	double drlon_gc;
	
	double pre_theta1=0;
	double pre_theta2=0;

	boolean zero_lon_crossing;

	//these are helper variables that will help with quadrant calculations, will take -1,0,1 values;
	int w_e;
	int s_n;
	
	// measures come in as degrees, the trig functions want radians
	lat1=origin.getX();
	lat2=end.getX();
	lon1=origin.getY();
	lon2=end.getY();

	rlat1=Math.toRadians(lat1); 
	rlat2=Math.toRadians(lat2);
	rlon1=Math.toRadians(lon1);
	rlon2=Math.toRadians(lon2);

	//check if we have pole origin, if so, return -1
	
	if (lat1==90 || rlat1==-90) return -1;
	
	//zero crossings?
 	
        zero_lon_crossing=(Math.abs(lon1-lon2)>(360-Math.abs(lon1-lon2)));
	
	//calculate differences
	dlat=Math.min(Math.abs(lat2-lat1),360-Math.abs(lat2-lat1));
	dlon=Math.min(Math.abs(lon2-lon1),360-Math.abs(lon2-lon1));
	//System.out.println("dlat dlon :" + dlat +" "+dlon);

	drlat = Math.toRadians(dlat);
	drlon = Math.toRadians(dlon);
	//check if origin==end if so, return 0
	if (drlat==0 && drlon==0)
	    return 0;
	
	//System.out.println("Zero lon crossing "+zero_lon_crossing); 
	
	//calculate helper vars for determining quadrant (mathematical, has nothing to do with Earth coordinates)

	//s_n
	if (rlat2>rlat1){
	    s_n=1;
	} else {
	    if (rlat2==rlat1){
		s_n=0;
	    } else {
		s_n=-1;
	    }
	}
	//w_e
	if (rlon2>rlon1){
	    w_e=1;
	} else {
	    if (rlon2==rlon1){
		w_e=0;
	    } else {
		w_e=-1;
	    }
	}
	if (zero_lon_crossing) {
	    w_e=w_e*-1;
	}

	try{
	    /*
	      double H=Math.asin(Math.sin(rlat1)*Math.sin(rlat2)+{Math.cos(rlat1)*Math.cos(rlat2)*Math.cos(drlon)});
	      //System.out.println("H :"+H);
	      double Z=Math.abs(Math.asin(Math.cos(rlat2)*Math.sin(drlon)/Math.cos(H)));
	      //System.out.println("Z :"+Z);
	      pre_theta1=Math.toDegrees(Z);
	    */
	    BigDecimal pre_h_1= (new BigDecimal(Math.sin(rlat1))).multiply(new BigDecimal(Math.sin(rlat2)));
	    BigDecimal pre_h_2= (new BigDecimal(Math.cos(rlat1))).multiply(new BigDecimal(Math.cos(rlat2))).multiply(new BigDecimal(Math.cos(drlon)));
	    double H=Math.asin((pre_h_1.add(pre_h_2)).doubleValue());
	    //System.out.println("pre_h_2 :"+pre_h_2.doubleValue());
	    double pre_z=((new BigDecimal(Math.cos(rlat2))).multiply(new BigDecimal(Math.sin(drlon))).divide( new BigDecimal(Math.cos(H)),50,BigDecimal.ROUND_HALF_UP)).doubleValue();
	    //System.out.println("pre_z: "+pre_z);
	    double Z=Math.abs(Math.asin(pre_z));
	    //System.out.println("Z :"+Z);
	    pre_theta1=Math.toDegrees(Z);
	} catch (Exception e){
	    System.out.println(e);
	    pre_theta1=90;
	}
	if (!(pre_theta1>=0 && pre_theta1<=360)){
	    pre_theta1=90;
	}
	//System.out.println("pre_theta1  :"+pre_theta1);    

	//System.out.println("Pre_theta1: "+pre_theta1);
	/*adjust according to mathematical quadrant of the end point
	 *cartesian ccordinates with "origin" point at the origin
	 *and y axis is parallel to a meridian passing through "origin" point
	 */
	 //0 degrees
	if(s_n==1 && w_e==0) {
	    pre_theta2=pre_theta1;
	    //System.out.println("zero degrees");
	}
	//first quadrant
	if(s_n==1 && w_e==1){ 
	    //System.out.println("first quadrant");
	    pre_theta2=pre_theta1;
	}
	//90 degrees
	if(s_n==0 && w_e==1) {
	    pre_theta2=pre_theta1;
	    //System.out.println("90 degrees");
	}
	//second quadrant
	if(s_n==-1 && w_e==1) {
	    //System.out.println("second quadrant");
	    pre_theta2=180-pre_theta1;
	}
	//180 degrees
	if(s_n==-1 && w_e==0) {
	    //System.out.println("180 degrees");
	    pre_theta2=180;
	}
	//third quadrant
	if(s_n==-1 && w_e==-1) {
	    //System.out.println("third quadrant");
	    pre_theta2=180+pre_theta1;
	}
	//270 degrees
	if(s_n==0 && w_e==-1) {
	    //System.out.println("270 degrees");
	    pre_theta2=180+pre_theta1;
	}
	//fourth quadrant
	if(s_n==1 && w_e==-1) {
	    //System.out.println("fourth quadrant");
	    pre_theta2=360-pre_theta1;
	}
	//calculate the precision of the result
	//the precision is such that for a given set of coordinates
	//if a client travels towards end point with resulting heading 
	//returned with given precision, the client will end up no more than 1m off the end point
	//it should really be calculated by asin(1/distance(origin,end))and finding order of first non zero digit
	//but I found out that (log base 10 of distance)-1 is very close approximation
	double dist=Math.max(WHGeo.distance(origin,end),1);
	int precision = Math.max(((int) Math.rint(Math.log(dist)/Math.log(10))),0);
	//System.out.println("Precision: "+precision);
	
	
	//round off the result
	return ((new BigDecimal(pre_theta2)).divide(new BigDecimal(1),precision,BigDecimal.ROUND_HALF_UP)).doubleValue();
    }
    
    public static double heading(Point2D.Double origin, Line2D.Double line_end){

	//some helper variables
	Point2D.Double p1;
	Point2D.Double p2;
	double headingP1P2;
	double headingP1O;
	double headingP2P1;
	double headingP2O;
	double angleOP1P2;
	double angleOP2P1;

	//calculate them
	//p1 will be upper point, and p2 will be lower point
	if (line_end.getX1()>line_end.getX2()){
	    p1= new Point2D.Double(line_end.getX1(),line_end.getY1());
	    p2= new Point2D.Double(line_end.getX2(),line_end.getY2());
	}
	else {
	    p2= new Point2D.Double(line_end.getX1(),line_end.getY1());
	    p1= new Point2D.Double(line_end.getX2(),line_end.getY2());
	}
	headingP1P2= WHGeo.heading(p1,p2);
	headingP1O = WHGeo.heading(p1,origin);
	headingP2P1= WHGeo.heading(p2,p1);
	headingP2O = WHGeo.heading(p2,origin);
	angleOP1P2 = Math.abs(headingP1P2-headingP1O);
	if (angleOP1P2>180)
	    angleOP1P2=360-angleOP1P2;
	angleOP2P1 = Math.abs(headingP2P1-headingP2O);
	if (angleOP2P1>180)
	    angleOP2P1=360-angleOP2P1;
	//check if either of these angles is obtuse
	if((angleOP1P2>90)||(angleOP2P1>90)){
	    //return the heading of the closer point
	    double d1=WHGeo.distance(origin,p1);
	    double d2=WHGeo.distance(origin,p2);
	    if(d1<d2)
		return WHGeo.heading(origin,p1);
	    else 
		return WHGeo.heading(origin,p2);
	}
	else {
	    //return heading that is perpendicular to P1P2 line
	    //determine if the origin is above or below the line
	    if (headingP1P2<headingP1O)
		return (((headingP1P2-90)+360)%360);
	    else
		return (((headingP1P2+90)+360)%360);
	}
    }

    public static double heading(Point2D.Double origin, WHPolygon poly_end){
	return poly_end.heading(origin);
    }
    
    /*
     * @requires: 
     * positive angle value
     * positive range value
     * @effects:
     * Given an angle, range and current position,
     * "project" the user so that lat,lon are translated to the new position
     * at the point that is (angle and range) from the current lat,lon
     */
    public static void project(Point2D.Double location, double angle, double range){
	
	double r;
	double rlat;
	double rlon;
	double dx;
	double dy;
	double drlat;
	double drlon;
	double rpjheading;
	
	BigDecimal rangeBD;
	BigDecimal rlatBD;
	BigDecimal rlonBD;
	BigDecimal dxBD;
	BigDecimal dyBD;
	BigDecimal drlatBD;
	BigDecimal drlonBD;
	BigDecimal rBD;
	BigDecimal bd_of_2=new BigDecimal(2);
	
	// need radians
	rlat = Math.toRadians(location.getX());
	rlon = Math.toRadians(location.getY());
	rpjheading = Math.toRadians(angle);
	//create corresponding BDs
	rlatBD=new BigDecimal(rlat);
	rlonBD=new BigDecimal(rlon);
	rangeBD=new BigDecimal(range);
	// calculate r - the earth's radius at this latitude
	// r = (GEO_A * Math.sqrt(1 - GEO_E2)) / (1 - (GEO_E2 * Math.pow(Math.sin(Math.toRadians(this.lat)),2)));
	// solved first part of this (see class static vars), to save clocks
	rBD =new BigDecimal(Ra / (1 - (GEO_E2 * Math.pow(Math.sin(rlat),2))));
	//System.out.println("Earth radius   : "+(int)r);
	// distance in meters
    
	dxBD = rangeBD.multiply(new BigDecimal(Math.cos(rpjheading)));
	dyBD = rangeBD.multiply(new BigDecimal(Math.sin(rpjheading)));
	//System.out.println("dx,dy          : "+dxBD.doubleValue()+","+dyBD.doubleValue());
	// translate into distance in radians around the spherical earth
	drlatBD = dxBD.divide(rBD,20,BigDecimal.ROUND_HALF_EVEN);
	
	// set location's coordinates.
	double rlat1 = rlatBD.add(drlatBD).doubleValue();
        //dlon = 2 * Math.sin(Math.sin(dx / (2*r))/Math.cos(rlat));
	drlonBD = bd_of_2.multiply(new BigDecimal(Math.asin(((new BigDecimal(Math.sin((dyBD.divide(bd_of_2.multiply(rBD),20,BigDecimal.ROUND_HALF_EVEN).doubleValue())))).divide((new BigDecimal(Math.cos(rlat1))),20,BigDecimal.ROUND_HALF_EVEN)).doubleValue())));
	double rlon1= rlonBD.add(drlonBD).doubleValue();
	//System.out.println("drlat,drlon    : "+drlatBD.doubleValue()+","+drlonBD.doubleValue());
      
	location.setLocation(Math.toDegrees(rlat1),Math.toDegrees(rlon1));
	//System.out.println("Location set at: "+location.getX()+","+location.getY());
	WHGeo.toWHFormat(location);
	//System.out.println("Loc after conv : "+location.getX()+","+location.getY());
    }
    
    public static void project(WHPolygon poly_location, double project_heading, double project_range){
	poly_location.project(project_heading, project_range);
    }
    
    public static double distance(Point2D.Double origin, Point2D.Double end){
     
	double drlon;
	double drlat;
	double rlat1;
	double rlon1;
	double rlat2;
	double rlon2;
	
	BigDecimal drlonBD;
	BigDecimal drlatBD;
	BigDecimal drlonBD_half;
	BigDecimal drlatBD_half;

	BigDecimal rlat1BD;
	BigDecimal rlon1BD;
	BigDecimal rlat2BD;
	BigDecimal rlon2BD;
  
	BigDecimal bd_of_2 = new BigDecimal(2);
	BigDecimal bd_of_2PI = bd_of_2.multiply(new BigDecimal(Math.PI));
	
	BigDecimal rBD;
	BigDecimal aBD;
	double a;
	double c;
	double d;
	
	
	// measures come in as degrees, the trig functions want radians
	rlat1=Math.toRadians(origin.getX()); 
	rlat2=Math.toRadians(end.getX());
	rlon1=Math.toRadians(origin.getY());
	rlon2=Math.toRadians(end.getY());

	rlat1BD= new BigDecimal(rlat1);
	rlat2BD= new BigDecimal(rlat2);
	rlon1BD= new BigDecimal(rlon1);
	rlon2BD= new BigDecimal(rlon2);
	/* calculate r - the earth's radius at this latitude
	 * r = (GEO_A * Math.sqrt(1 - GEO_E2)) / (1 - (GEO_E2 * Math.pow(Math.sin(Math.toRadians(this.lat)),2)));
	 * solved first part of this (see class static vars), to save clocks
	 */
	rBD = new BigDecimal(Ra / (1 - (GEO_E2 * Math.pow(Math.sin(rlat1),2))));
	
	drlatBD=((rlat2BD.subtract(rlat1BD)).abs()).min(bd_of_2PI.subtract(rlat2BD.subtract(rlat1BD).abs()));
	drlonBD=((rlon2BD.subtract(rlon1BD)).abs()).min(bd_of_2PI.subtract(rlon2BD.subtract(rlon1BD).abs()));
	drlatBD_half=drlatBD.divide(bd_of_2,20,BigDecimal.ROUND_HALF_EVEN);
	drlonBD_half=drlonBD.divide(bd_of_2,20,BigDecimal.ROUND_HALF_EVEN);
       
	//////System.out.println("rlat1 rlon1 rlat2 rlon2: "+rlat1+" "+rlon1+" "+rlat2+" "+rlon2);
	//////System.out.println("drlat, drlon : "+drlat+" "+drlon);
	
	//a = Math.pow(Math.sin(drlat/2),2) + Math.cos(rlat1) * Math.cos(rlat2) * Math.pow(Math.sin(drlon/2),2); 
	aBD = (new BigDecimal(Math.sin(drlatBD_half.doubleValue()))).multiply(new BigDecimal(Math.sin(drlatBD_half.doubleValue()))).add((new BigDecimal(Math.cos(rlat1))).multiply(new BigDecimal(Math.cos(rlat2))).multiply(new BigDecimal(Math.sin(drlonBD_half.doubleValue()))).multiply(new BigDecimal(Math.sin(drlonBD_half.doubleValue())))); 
	//go back to a, since BigDecimal does not support square roots...
	a = aBD.doubleValue();
	c = 2 * Math.asin(Math.min(1,Math.sqrt(a))); 
	d = rBD.doubleValue() * c;

	return d;
    }

    public static double distance(Point2D.Double origin, Line2D.Double line_end){
	//some helper variables
	Point2D.Double p1;
	Point2D.Double p2;
	double headingP1P2;
	double headingP1O;
	double headingP2P1;
	double headingP2O;
	double angleOP1P2;
	double angleOP2P1;
	
	//calculate them
	p1= new Point2D.Double(line_end.getX1(),line_end.getY1());
	p2= new Point2D.Double(line_end.getX2(),line_end.getY2());
	//System.out.println("p1: "+p1.getX()+","+p1.getY());
	//System.out.println("p2: "+p2.getX()+","+p2.getY());
	//System.out.println("origin: "+origin.getX()+","+origin.getY());
	headingP1P2= WHGeo.heading(p1,p2);
	//System.out.println("hp1p2: "+headingP1P2);
	headingP1O = WHGeo.heading(p1,origin);
	//System.out.println("hp1o: "+headingP1O);
	headingP2P1= WHGeo.heading(p2,p1);
	//System.out.println("hp2p1: "+headingP2P1);
	headingP2O = WHGeo.heading(p2,origin);
	//System.out.println("hp2o: "+headingP2O);
	angleOP1P2 = Math.abs((new BigDecimal(headingP1P2).subtract(new BigDecimal(headingP1O))).doubleValue());
	if (angleOP1P2>180)
	    angleOP1P2 = 360-angleOP1P2;
	angleOP2P1 = Math.abs(new BigDecimal(headingP2P1-headingP2O).doubleValue());
	if (angleOP2P1>180)
	    angleOP2P1 = 360-angleOP2P1;
	//System.out.println("angles: "+angleOP1P2+","+angleOP2P1);
	
	//check if either of these angles is obtuse
	if((angleOP1P2>90)||(angleOP2P1>90)){
	    //System.out.println("obtuse");
	    //return the distance of the closer point
	    return Math.min(WHGeo.distance(origin,p1),WHGeo.distance(origin,p2));
	}
	else {
	    //System.out.println("not obtuse");
	    //return the distance to P1P2 line
	    //System.out.println("distance: "+WHGeo.distance(origin,p1));
	    //System.out.println("sin : "+Math.sin(Math.toRadians(angleOP1P2)));
	    BigDecimal temp_dist = new BigDecimal(WHGeo.distance(origin,p1));
	    BigDecimal sin = new BigDecimal(Math.sin(Math.toRadians(angleOP1P2)));
	    return ((temp_dist.multiply(sin)).doubleValue());
	}
    }

    public static double distance(Point2D.Double origin, WHPolygon poly_end){
	return poly_end.distance(origin);
    }

    public static String quadrant(double angle){
	
	String quadrant="";
	
	if 		( (angle > 292.5) || (angle <= 67.5))  { quadrant = "N"; }
	else if ( (angle > 112.5) && (angle <= 247.5)) { quadrant = "S"; }
	
	if 		( (angle > 22.5)  && (angle <= 157.5)) { quadrant += "E"; }
	else if ( (angle > 202.5) && (angle <= 337.5)) { quadrant += "W"; }
	
	return quadrant;
    }
  
    public static void toWHFormat(Point2D.Double point){
	BigDecimal x= new BigDecimal(point.getX());
	BigDecimal y= new BigDecimal(point.getY());
	BigDecimal _360 = new BigDecimal(360);
	double lat,lon;
	//if latitude has values outside (-90,90), convert
	//convert to (0,360) range
	BigDecimal adjustX=x.divide(_360,BigDecimal.ROUND_FLOOR);
	adjustX=new BigDecimal(adjustX.intValue());
      	x=x.subtract(adjustX.multiply(_360));
	//convert to (-90,90) range
	lat=x.doubleValue();
	if (lat<=180 && lat>90){
	    //System.out.println("lat between 90 and 180");
	    x=x.negate();
	    x=x.add(new BigDecimal(180));
	   
	} else {
	    if (!(lat<90)){
		x=x.subtract(_360);
		if(lat>180 && lat<270){
		    //System.out.println("lat between 180 and 270");
		    x=x.negate();
		    x=x.add(new BigDecimal(-180));
		}
	    }   
	}
	//if longitude has values outside (0,360), convert
	BigDecimal adjustY=y.divide(_360,BigDecimal.ROUND_FLOOR);
	adjustY=new BigDecimal(adjustY.intValue());
	y=y.subtract(adjustY.multiply(_360));
	point.setLocation(x.doubleValue(),y.doubleValue());
    }
}
	
    






