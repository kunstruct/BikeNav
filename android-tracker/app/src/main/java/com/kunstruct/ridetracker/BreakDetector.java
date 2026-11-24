package com.kunstruct.ridetracker;

/** Derived from GPS only; never controls sensor capture or persistence. */
final class BreakDetector {
    static final long MIN_BREAK_NS=120_000_000_000L;
    private long start=-1,end,total,movementStart=-1;
    private int count,movementFixes;
    private double latitude,longitude,stoppedDistance,pendingDistance,totalDistance;

    void add(RouteData.Point point,boolean connected,double accuracy,Double speed,double legDistance) {
        if(!connected)interrupt();
        if(accuracy>25||speed==null) {interrupt();return;}
        boolean quiet=speed<=0.5;
        if(start>=0&&RouteData.metres(latitude,longitude,point.lat,point.lon)>25)interrupt();
        if(start<0) {
            if(quiet){start=end=point.mono;latitude=point.lat;longitude=point.lon;}
            return;
        }
        if(quiet) {
            end=point.mono;stoppedDistance+=pendingDistance+legDistance;pendingDistance=0;
            movementStart=-1;movementFixes=0;
        } else {
            pendingDistance+=legDistance;
            if(speed>=1.5) {
                if(movementStart<0)movementStart=point.mono;
                if(++movementFixes>=2&&point.mono-movementStart>=2_000_000_000L)interrupt();
            } else {movementStart=-1;movementFixes=0;}
        }
    }
    private boolean qualified() {return start>=0&&end-start>=MIN_BREAK_NS;}
    void interrupt() {
        if(qualified()){total+=end-start;totalDistance+=stoppedDistance;count++;}
        start=-1;movementStart=-1;movementFixes=0;stoppedDistance=pendingDistance=0;
    }
    long millis() {return (total+(qualified()?end-start:0))/1_000_000;}
    int count() {return count+(qualified()?1:0);}
    double distance() {return totalDistance+(qualified()?stoppedDistance:0);}
}
