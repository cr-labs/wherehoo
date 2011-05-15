package com.wherehoo.final2001;

import java.util.Vector;
import java.util.StringTokenizer;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.math.BigDecimal;

public class WHPolygon{

    private Point2D.Double[] coordinates;
    private GeneralPath path;
    private boolean zero_crossing;
    //for zero crossing case
    private WHPolygon leftPoly;
    private WHPolygon rightPoly;

    public WHPolygon(String poly_descriptor){

	zero_crossing = false;
	leftPoly=null;
	rightPoly=null;

	StringTokenizer vertex_tokens= new StringTokenizer(poly_descriptor,"(",false);
	String vertex;
	coordinates=new Point2D.Double[vertex_tokens.countTokens()];
	
	for(int i=0;vertex_tokens.hasMoreTokens();i++){
	    vertex=vertex_tokens.nextToken();
	    //read in the next vertex
	    coordinates[i]= new Point2D.Double(
					       Double.parseDouble(vertex.substring(0,vertex.indexOf(','))),
					       Double.parseDouble(vertex.substring(vertex.indexOf(',')+1,vertex.indexOf(')'))));
	}

	zero_crossing = this.checkForZeroCrossing();
	this.constructPath();
    }
      

    public WHPolygon(Point2D.Double[] vertices){
	coordinates=new Point2D.Double[vertices.length];
	for (int i=0;i<vertices.length;i++){
	    coordinates[i]= new Point2D.Double(vertices[i].getX(),vertices[i].getY());
	}
	zero_crossing=this.checkForZeroCrossing();
	this.constructPath();
    }    
    
    public WHPolygon(Point2D.Double client_location, double radius){
	
	coordinates = new Point2D.Double[4];
	//calculate the coordinates of four vertices
	double half_diagonal=radius*Math.sqrt(2);
	Point2D.Double poly_vertex=new Point2D.Double(client_location.getX(),client_location.getY());
	//calculate a first vertex
	WHGeo.project(poly_vertex,45,half_diagonal);
	coordinates[0]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a second vertex
	WHGeo.project(poly_vertex,180,2*radius);
	coordinates[1]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a third vertex
	WHGeo.project(poly_vertex,270,2*radius);
	coordinates[2]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a fourth vertex
	WHGeo.project(poly_vertex,0,2*radius);
	coordinates[3]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	
	zero_crossing=this.checkForZeroCrossing();
	this.constructPath();
    }
    
    public WHPolygon(Point2D.Double client_location, double heading, double width, double length){
	
	//create coordinates[]
	coordinates= new Point2D.Double[4];
	double half_width= (new BigDecimal(width).divide(new BigDecimal(2),20,BigDecimal.ROUND_HALF_EVEN)).doubleValue();
	//calculate the coordinates of four vertices of the search area
	Point2D.Double poly_vertex=new Point2D.Double(client_location.getX(),client_location.getY());
	double poly_side_angle;
	//calculate a first vertex
	poly_side_angle=((heading+270)%360);
	WHGeo.project(poly_vertex,poly_side_angle,half_width);
	coordinates[0]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a second vertex
	poly_side_angle=heading;
	WHGeo.project(poly_vertex,poly_side_angle,length);
	coordinates[1]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a third vertex
	poly_side_angle=((heading+90)%360);
	WHGeo.project(poly_vertex,poly_side_angle,width);
	coordinates[2]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a fourth vertex
	poly_side_angle=((heading+180)%360);
	WHGeo.project(poly_vertex,poly_side_angle,length);
	coordinates[3]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	zero_crossing=this.checkForZeroCrossing();
	this.constructPath();
	
    }
    
    public void project(double project_heading, double project_range){
	//project all the vertices
	Point2D.Double temp_loc;
	for(int i=0;i<coordinates.length;i++){
	    WHGeo.project(coordinates[i],project_heading,project_range);
	}
    }
    
    public double distance(Point2D.Double origin){
	//if this has one vertex => return the distance to that vertex
	if (coordinates.length == 1){
	    return WHGeo.distance(origin, coordinates[0]);
	} else {
	    //if origin inside this => return 0
	    if (this.contains(origin)){
		//System.out.println("contained");
		return 0;
		//find the distance by calculating the distance to all the sides and taking the minimum
	    }
	    else {
		double d=WHGeo.distance(origin, new Line2D.Double(coordinates[coordinates.length-1],coordinates[0]));
		//System.out.println("temp_d: "+d);
		double temp_d;
		for (int i=1;i<coordinates.length;i++){
		    temp_d=WHGeo.distance(origin, new Line2D.Double(coordinates[i-1],coordinates[i]));
		    //System.out.println("temp_d: "+temp_d);
		    if (temp_d<d)
			d=temp_d;
		}
		return d;
	    }
	}
    }

    public double heading(Point2D.Double origin){
	//if this has one vertex => return the heading to that vertex
	if (coordinates.length == 1){
	    return WHGeo.heading(origin, coordinates[0]);
	} else {
	    //if origin inside this => return 0
	    if (this.contains(origin)){
		return 0;
	    } else {
		//find the closest side
		double d=WHGeo.distance(origin, new Line2D.Double(coordinates[coordinates.length-1],coordinates[0]));
		Line2D.Double closest_side = new Line2D.Double(coordinates[coordinates.length-1],coordinates[0]);
		double temp_d;
		for (int i=1;i<coordinates.length;i++){
		    temp_d=WHGeo.distance(origin, new Line2D.Double(coordinates[i-1],coordinates[i]));
		    if (temp_d<d){
			d=temp_d;
			closest_side=new Line2D.Double(coordinates[i-1],coordinates[i]);
		    }
		    //System.out.println("Closest side: "+closest_side.getX1()+","+closest_side.getY1()+","+closest_side.getX2()+","+closest_side.getY2());
		}
		return WHGeo.heading(origin, closest_side);
	    }
	}
    }
    
    public boolean contains(Point2D.Double point){
	if (zero_crossing){
	    //System.out.println("left:"+leftPoly.contains(point));
	    //System.out.println("right:"+rightPoly.contains(point));
	    return (leftPoly.contains(point) || rightPoly.contains(point));
	}
	else 
	    return path.contains(point);
    }
    public boolean zeroCrossing(){
	return zero_crossing;
    }
    public WHPolygon[] splitAlongGreatMeridian(){
	WHPolygon[] half_polies;
	if (zero_crossing){
	    half_polies= new WHPolygon[2];
	    half_polies[0]=(WHPolygon)leftPoly.clone();
	    half_polies[1]=(WHPolygon)rightPoly.clone();
	} else {
	    half_polies= new WHPolygon[1];
	    half_polies[0]=(WHPolygon)this.clone();
	}
	return half_polies;
    }
    
    public int vertexCount(){
	return coordinates.length;
    }
    public int[] getSides(){
	int[] sides=new int[coordinates.length];
	for (int i=0;i<coordinates.length-1;i++){
	    sides[i]=(int)WHGeo.distance(coordinates[i],coordinates[i+1]);
	}
	sides[coordinates.length-1]=(int)WHGeo.distance(coordinates[coordinates.length - 1],coordinates[0]);
	return sides;
    }
    public Point2D.Double[] getVertices(){
	Point2D.Double[] copy_of_coordinates = new Point2D.Double[coordinates.length];
	for (int i=0;i<coordinates.length;i++){
	    copy_of_coordinates[i] = new Point2D.Double(coordinates[i].getX(),coordinates[i].getY());
	}
	return copy_of_coordinates;
    }
    public String toString(){
	String poly_description="(";
	for (int i=0;i<coordinates.length;i++){
	    poly_description+="("+coordinates[i].getX()+","+coordinates[i].getY()+")";
	    if (i<coordinates.length-1)
		poly_description+=",";
	}
	poly_description+=")";
	return poly_description;
    }
    public String toString(int central_meridian){
	//this works for central meridians and initial coordinates that are in between 0 and 360
	String poly_description="(";
	double lon;
	for (int i=0;i<coordinates.length;i++){
	    if (i>0) poly_description+=",";
	    //append the latitude
	    poly_description+="("+coordinates[i].getX();
	    //append the transformed longitude
	    lon=coordinates[i].getY();
	    if (!(lon>=central_meridian-180 && lon<=central_meridian+180)){
		//put lon in the (0,360)range
		if (lon>central_meridian+180)
		    lon=lon-360;
		else 
		    //lon<central_meridian-180
		    lon=lon+360;
	    }
	    poly_description+=","+lon+")";
	}
	poly_description+=")";
	return poly_description;
    }
    public Object clone(){
	return new WHPolygon(coordinates);
    }

    //
    //PRIVATE METHODS
    //

    private void constructPath(){
	path = new GeneralPath();
	path.moveTo((float)coordinates[0].getX(),(float)coordinates[0].getY());
	for (int i=1;i<coordinates.length;i++){
	    path.lineTo((float)coordinates[i].getX(),(float)coordinates[i].getY());
	}
	path.closePath();
    }

    private boolean checkForZeroCrossing(){
	
	boolean result = false;
	if (coordinates.length>1){
	    double lon1;
	    double lon2;
	    //check for the zero crossing 
	    for (int i=1;i<coordinates.length;i++){
		//if the difference between longitudes of two points is more than 180 at least once
		//the result will be true
		lon1=coordinates[i-1].getY();
		lon2=coordinates[i].getY();
		result=(result || ((Math.abs(lon1-lon2)>180)&&(lon1!=0)&&(lon2!=0)&&(lon1!=360)&&(lon2!=360)));
		//System.out.println("Result: "+result);
	    }
	    lon1=coordinates[coordinates.length-1].getY();
	    lon2=coordinates[0].getY();
	    result=(result || ((Math.abs(lon1-lon2)>180)&&(lon1!=0)&&(lon2!=0)&&(lon1!=360)&&(lon2!=360)));
	    //System.out.println("Result: "+result);
	    if (result){
		//set up half-polies
		Vector coordinatesL=new Vector();
		Vector coordinatesR=new Vector();
		boolean left = (coordinates[0].getY()>180);  
		
		Point2D.Double p1;
		Point2D.Double p2;
		double lat1;
		double lat2;
		
		for (int i=1;i<=coordinates.length;i++){
		    p1=coordinates[(i-1)%coordinates.length];
		    p2=coordinates[(i)%coordinates.length];
		    
		    lon1=p1.getY();
		    lon2=p2.getY();
		    lat1=p1.getX();
		    lat2=p2.getX();

		    if (Math.abs(lon1-lon2)>180){
			//zero crossing

			//find intersection of the side and Great Meridian
			double distanceP1GM=WHGeo.distance(p1,new Point2D.Double(lat1,0));
			double distanceGMP2=WHGeo.distance(new Point2D.Double(lat2,0),p2);
			double lat=((distanceP1GM/(distanceP1GM+distanceGMP2))*(lat2-lat1))+lat1;
			//System.out.println("Lat :"+lat);
			Point2D.Double cross_point = new Point2D.Double(lat,0);
			WHGeo.toWHFormat(cross_point);
			//add that point to both polygons, but change longitude to 360 on left
			coordinatesL.addElement(new Point2D.Double(cross_point.getX(),cross_point.getY()+360));
			coordinatesR.addElement(cross_point);
			//change left variable
			left = ! left;
		    }
		    if (left)
			coordinatesL.addElement(p2);
		    else
			coordinatesR.addElement(p2);
		}
		
		//change the vectors into arrays and create polies
		Point2D.Double[] left_poly_array= new Point2D.Double[coordinatesL.size()];
		coordinatesL.copyInto(left_poly_array);
		Point2D.Double[] right_poly_array= new Point2D.Double[coordinatesL.size()];
		coordinatesR.copyInto(right_poly_array);
		leftPoly = new WHPolygon(left_poly_array);
		rightPoly= new WHPolygon(right_poly_array);
	    }
	}
	return result;
    }
    public static double half_diagonal(double wid, double len){
	double angle = WHPolygon.half_diagonal_angle(wid,len);
	double c = ((new BigDecimal(wid)).divide(new BigDecimal(Math.sin(Math.toRadians(angle))),20,BigDecimal.ROUND_HALF_EVEN)).doubleValue();
	return c;
    }
    
    public static double half_diagonal_angle(double wid, double len){
	double tangent = ((new BigDecimal(wid)).divide(new BigDecimal(len),20,BigDecimal.ROUND_HALF_EVEN)).doubleValue();
	return Math.toDegrees(Math.atan(tangent));
    }
}










