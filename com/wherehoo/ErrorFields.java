package com.wherehoo;
 
class ErrorFields {
    protected boolean act,idt,wid,len,hdg,rad,beg,end,dat,shp,sha,llh,xyz,pjt,lim,pro,mim,met,uid;
   
    
    protected ErrorFields(){
	wid=true;
	len=true;
	hdg=true;
	rad=true;
	dat=true;
	shp=true;
	sha=true;
	act=true;
	idt=true;
	pro=true;
	uid=true;
	mim=true;
	
	llh=false;
	pjt=false;
	lim=false;
	beg=false;
	end=false;
	met=false;
    }
    public String toString(){
	String s="wid="+wid+", len="+len+", hdg="+hdg+", rad="+rad+", dat="+dat;
	s+=", shp="+shp+", sha="+sha+", act="+act+", idt="+idt+", pro="+pro+", uid="+uid+", mim="+mim;
	s+=", llh="+llh+", pjt="+pjt+", lim="+lim+", beg="+beg+", end="+end+", met="+met;
	return s;
    }
}
