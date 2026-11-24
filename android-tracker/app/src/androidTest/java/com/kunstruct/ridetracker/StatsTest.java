package com.kunstruct.ridetracker;

import android.test.AndroidTestCase;
import android.graphics.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class StatsTest extends AndroidTestCase {
    @Override protected void setUp() throws Exception {
        super.setUp();assertTrue(android.os.Build.HARDWARE.equals("ranchu")||android.os.Build.HARDWARE.equals("goldfish"));
    }
    private void point(RouteData data,int second,double metres,Double speed) {
        data.add(12.95+metres/111195,77.6,second*1_000_000_000L,1,4.0,speed);
    }
    static RouteData profile() {
        RouteData data=new RouteData();
        for(int t=0;t<=300;t++) {
            double metres=t<60?t*10:t<=240?600:600+(t-240)*10;
            data.add(12.95+metres/111195,77.6,t*1_000_000_000L,1,4.0,t>=60&&t<=240?0.0:10.0);
        }
        return data;
    }
    public void testBreakExcludesWholeQualifyingStopFromAverage() {
        RouteData data=profile();RideStats stats=new RideStats(data,300000);
        assertEquals(180000,stats.breakMillis);assertEquals(120000,stats.rideMillis);assertEquals(1,stats.breaks);
        assertEquals(data.distance*3600/120000,stats.averageKmh,.000001);
        assertTrue(stats.averageKmh>data.distance*3600/300000);assertEquals(36.0,stats.topKmh,.000001);
    }
    public void testExactlyTwoMinutesQualifiesButShortTrafficStopDoesNot() {
        RouteData data=new RouteData();for(int t=0;t<=119;t++)point(data,t,0,0.0);
        assertEquals(0,data.breaks.millis());point(data,120,0,0.0);assertEquals(120000,data.breaks.millis());
        RouteData shortStop=new RouteData();for(int t=0;t<=90;t++)point(shortStop,t,0,0.0);
        point(shortStop,91,10,10.0);point(shortStop,92,20,10.0);point(shortStop,93,30,10.0);
        assertEquals(0,new RideStats(shortStop,93000).breakMillis);
    }
    public void testStationaryJitterAndSingleSpeedSpikeDoNotEndBreakOrInflateDistance() {
        RouteData data=new RouteData();
        for(int t=0;t<=180;t++)point(data,t,t==75?5:0,t==75?25.0:0.0);
        RideStats stats=new RideStats(data,180000);
        assertEquals(180000,stats.breakMillis);assertEquals(0,stats.rideMillis);assertFalse(stats.hasAverage);
        assertEquals(0.0,data.distance,.000001);assertEquals(0.0,stats.topKmh,.000001);
    }
    public void testSustainedMovementEndsBreakAtLastStationaryFix() {
        RouteData data=new RouteData();for(int t=0;t<=130;t++)point(data,t,0,0.0);
        for(int t=131;t<=140;t++)point(data,t,(t-130)*10,10.0);
        RideStats stats=new RideStats(data,140000);assertEquals(130000,stats.breakMillis);assertEquals(10000,stats.rideMillis);assertEquals(1,stats.breaks);
    }
    public void testGpsLossIsNotInventedAsABreak() {
        RouteData data=new RouteData();for(int t=0;t<=60;t++)point(data,t,0,0.0);
        for(int t=300;t<=360;t++)point(data,t,0,0.0);
        assertEquals(0,data.breaks.millis());assertEquals(360000,new RideStats(data,360000).rideMillis);assertEquals(1,data.gaps);
    }
    public void testSegmentAndBootBoundariesDoNotJoinStops() {
        RouteData data=new RouteData();for(int t=0;t<=60;t++)point(data,t,0,0.0);
        for(int t=0;t<=60;t++)data.add(12.95,77.6,t*1_000_000_000L,2,4.0,0.0);
        assertEquals(0,data.breaks.millis());
        for(int t=61;t<=120;t++)data.add(12.95,77.6,t*1_000_000_000L,2,4.0,0.0);
        RideStats stats=new RideStats(data,180000);assertEquals(120000,stats.breakMillis);assertEquals(60000,stats.rideMillis);
    }
    public void testMissingSpeedAndPoorAccuracyCannotConfirmBreaks() {
        for(Double speed:new Double[]{null,Double.NaN,Double.POSITIVE_INFINITY,-1.0,101.0}) {
            RouteData data=new RouteData();for(int t=0;t<=180;t++)point(data,t,0,speed);
            assertEquals(0,data.breaks.millis());assertFalse(data.hasSpeed);
        }
        RouteData poor=new RouteData();for(int t=0;t<=180;t++)poor.add(12.95,77.6,t*1_000_000_000L,1,35.0,0.0);
        assertEquals(0,poor.breaks.millis());assertFalse(poor.hasSpeed);
        RouteData interrupted=new RouteData();for(int t=0;t<=180;t++)interrupted.add(12.95,77.6,t*1_000_000_000L,1,t==90?100.0:4.0,0.0);
        assertEquals(0,interrupted.breaks.millis());
    }
    public void testSlowTravelOutOfClusterIsNotABreak() {
        RouteData data=new RouteData();for(int t=0;t<=180;t++)point(data,t,t*.4,.4);
        assertEquals(0,data.breaks.millis());
    }
    public void testCrawlingTrafficAccumulatesSubThreeMetreSteps() {
        RouteData data=new RouteData();for(int t=0;t<=180;t++)point(data,t,t,1.0);
        RideStats stats=new RideStats(data,180000);assertEquals(0,stats.breakMillis);
        assertTrue("One-metre GPS steps must contribute distance",data.distance>175&&data.distance<181);
        assertEquals(3.6,stats.averageKmh,.1);assertEquals(3.6,stats.topKmh,.000001);
    }
    public void testTopSpeedRejectsIsolatedSpikeAndUsesSupportedPeak() {
        RouteData data=new RouteData();double[] speeds={10,10,80,10,10,30,31,32,31,10};
        for(int t=0;t<speeds.length;t++)point(data,t,t*10,speeds[t]);
        assertEquals(31.0,data.maxSpeed,.000001);
        RouteData disconnected=new RouteData();point(disconnected,0,0,80.0);point(disconnected,30,10,70.0);assertFalse(disconnected.hasSpeed);
    }
    public void testEmptyAndZeroRideTimeNeverDivideByZero() {
        RideStats empty=new RideStats(new RouteData(),0);assertFalse(empty.hasAverage);assertFalse(empty.hasTop);assertEquals("—",empty.average());
        RouteData stopped=new RouteData();for(int t=0;t<=150;t++)point(stopped,t,0,0.0);
        RideStats clamped=new RideStats(stopped,1000);assertEquals(1000,clamped.breakMillis);assertEquals(0,clamped.rideMillis);assertFalse(clamped.hasAverage);
    }
    public void testExportSummaryMatchesBreakStatsWithoutChangingRawSamples() throws Exception {
        String database="stats-"+UUID.randomUUID()+".db";File file=null;
        try(RideStore store=new RideStore(getContext(),database)) {
            long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1);List<RideStore.Sample> batch=new ArrayList<>();
            RouteData data=profile();
            for(int t=0;t<=300;t++) {
                RouteData.Point point=data.points.get(t);RideStore.Sample p=new RideStore.Sample();p.kind="gps";p.utc=p.received=1000+t*1000;p.mono=point.mono;
                p.lat=point.lat;p.lon=point.lon;p.accuracy=4.0;p.speed=t>=60&&t<=240?0.0:10.0;batch.add(p);
            }
            store.commit(ride,segment,batch,300000,300_000_000_000L);store.finish(ride,"saved","");file=RideExport.zip(getContext().getCacheDir(),store,ride);
            try(ZipFile zip=new ZipFile(file)) {
                assertEquals(7,zip.size());String summary=new String(zip.getInputStream(zip.getEntry("summary.csv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                String[] row=summary.split("\n")[1].split(",",-1);
                assertEquals("2",row[0]);assertEquals("300000",row[1]);assertEquals("120000",row[2]);assertEquals("180000",row[3]);assertEquals("1",row[4]);assertEquals("301",row[8]);
                assertEquals(new RideStats(data,300000).averageKmh,Double.parseDouble(row[6]),.001);
                String raw=new String(zip.getInputStream(zip.getEntry("gps.csv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                assertEquals(302,raw.split("\n").length);assertEquals(301,store.ride(ride).gps);
            }
        } finally {if(file!=null)file.delete();getContext().deleteDatabase(database);}
    }
    public void testAllCardsReflectBreakStatsWithoutMovingTheRouteOrNames() {
        RouteData withBreak=profile(),withoutBreak=new RouteData();withoutBreak.points.addAll(withBreak.points);withoutBreak.distance=withBreak.distance;
        withoutBreak.hasSpeed=withBreak.hasSpeed;withoutBreak.maxSpeed=withBreak.maxSpeed;
        RideStore.Ride ride=new RideStore.Ride();ride.start=1000;ride.elapsed=300000;CardLabels names=new CardLabels("City","Coffee");
        for(int design=0;design<10;design++)for(boolean clear:new boolean[]{false,true}) {
            Bitmap a=CardRenderer.render(ride,withBreak,design,true,clear,null,0,names),b=CardRenderer.render(ride,withoutBreak,design,true,clear,null,0,names);
            boolean different=false;
            for(int y=0;y<1350;y+=5)for(int x=0;x<1080;x+=5) {
                if(y<1040||y>1280)assertEquals(a.getPixel(x,y),b.getPixel(x,y));else if(a.getPixel(x,y)!=b.getPixel(x,y))different=true;
            }
            assertTrue("Ride/break/average footer must reflect the computed metrics",different);a.recycle();b.recycle();
        }
    }
}
