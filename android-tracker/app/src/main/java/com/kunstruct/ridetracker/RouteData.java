package com.kunstruct.ridetracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/** Filtering affects the preview and metrics only. Raw GPS samples are never edited. */
public final class RouteData {
    public static final class Point {
        public double lat,lon;
        public long mono,segment;
        public boolean connect;
        public Point(double lat,double lon,long mono,long segment) {this.lat=lat;this.lon=lon;this.mono=mono;this.segment=segment;}
    }
    public final List<Point> points=new ArrayList<>();
    public double distance,maxSpeed;
    public boolean hasSpeed;
    public int rejected,gaps;
    final BreakDetector breaks=new BreakDetector();
    private double travelDistance,speedOne,speedTwo;
    private int speedSamples;
    private Point distanceAnchor;
    private boolean breakRoute;

    public static RouteData load(RideStore store,long id) {
        return load(store.getReadableDatabase(),id);
    }
    static RouteData load(SQLiteDatabase db,long id) {
        RouteData data=new RouteData();
        try(Cursor c=db.rawQuery("SELECT latitude,longitude,mono_ns,segment_id,accuracy,speed_mps FROM samples WHERE ride_id=? AND kind='gps' ORDER BY id",new String[]{""+id})) {
            while(c.moveToNext()) data.add(c.getDouble(0),c.getDouble(1),c.getLong(2),c.getLong(3),c.isNull(4)?null:c.getDouble(4),c.isNull(5)?null:c.getDouble(5));
        }
        return data;
    }

    void add(double lat,double lon,long mono,long segment,Double accuracy,Double speed) {
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||
                accuracy==null||!Double.isFinite(accuracy)||accuracy<0||accuracy>50) {rejected++;breakRoute=true;return;}
        Double validSpeed=speed!=null&&Double.isFinite(speed)&&speed>=0&&speed<=100?speed:null;
        Point p=new Point(lat,lon,mono,segment);double legDistance=0;
        if(!points.isEmpty()) {
            Point previous=points.get(points.size()-1);
            double seconds=(mono-previous.mono)/1e9;
            if(segment==previous.segment && seconds<=0) {rejected++;return;}
            double metres=metres(previous.lat,previous.lon,lat,lon);
            if(segment==previous.segment && seconds>0 && seconds<=10 && metres/seconds>100) {rejected++;breakRoute=true;return;}
            p.connect=!breakRoute && segment==previous.segment && seconds>0 && seconds<=10;
            if(!p.connect)gaps++;
        }
        if(!p.connect||distanceAnchor==null||(validSpeed!=null&&validSpeed<0.5))distanceAnchor=p;
        else {
            // Accumulate small displacements so slow traffic is not discarded at 1 Hz.
            double movement=metres(distanceAnchor.lat,distanceAnchor.lon,lat,lon);
            if(movement>=3){legDistance=movement;distanceAnchor=p;}
        }
        breaks.add(p,p.connect,accuracy,validSpeed,legDistance);travelDistance+=legDistance;
        distance=Math.max(0,travelDistance-breaks.distance());
        if(!p.connect||validSpeed==null||accuracy>25)speedSamples=0;
        if(validSpeed!=null&&accuracy<=25) {
            if(speedSamples>0) {
                double supported=speedSamples==1?Math.min(speedOne,validSpeed):
                        speedOne+speedTwo+validSpeed-Math.min(speedOne,Math.min(speedTwo,validSpeed))-Math.max(speedOne,Math.max(speedTwo,validSpeed));
                hasSpeed=true;maxSpeed=Math.max(maxSpeed,supported);
            }
            speedTwo=speedOne;speedOne=validSpeed;speedSamples=Math.min(2,speedSamples+1);
        }
        points.add(p);
        breakRoute=false;
    }

    public static double metres(double lat1,double lon1,double lat2,double lon2) {
        double a=Math.sin(Math.toRadians(lat2-lat1)/2),b=Math.sin(Math.toRadians(lon2-lon1)/2);
        double h=a*a+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*b*b;
        return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)));
    }
}
