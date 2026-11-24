package com.kunstruct.ridetracker;

import android.graphics.*;
import android.test.AndroidTestCase;
import java.io.*;
import java.util.*;

public final class MapTest extends AndroidTestCase {
    @Override protected void setUp() throws Exception {
        super.setUp();assertTrue("Map fixture tests require an emulator",android.os.Build.HARDWARE.equals("ranchu")||android.os.Build.HARDWARE.equals("goldfish"));
    }
    static RouteData route() {
        RouteData data=new RouteData();for(int i=0;i<100;i++)data.add(12.95+i*.00012,77.6+Math.sin(i*.1)*.001,(i+1)*1_000_000_000L,1,4.0,12.0);return data;
    }
    private static int tileColor(int x,int y) {return Color.rgb(20+x%100,80+y%100,120);}
    static Bitmap tile(int z,int x,int y) {
        Bitmap b=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);b.eraseColor(tileColor(x,y));return b;
    }
    public void testMercatorAndDateLineUseLocalRouteBounds() throws Exception {
        assertEquals(.5,MapBackground.worldX(0,.5),.0000001);assertEquals(.5,MapBackground.worldY(0),.0000001);
        assertTrue(MapBackground.worldY(90)>=-.000001);assertTrue(MapBackground.worldY(-90)<=1.000001);
        RouteData route=new RouteData();route.add(0,179.999,1,1,4.0,10.0);route.add(0,-179.999,31_000_000_000L,1,4.0,10.0);
        MapBackground.Viewport view=new MapBackground.Viewport(route,1000,600);
        assertTrue(Math.abs(MapBackground.worldX(-179.999,view.origin)-MapBackground.worldX(179.999,view.origin))<.0001);
        MapBackground.Plan plan=new MapBackground.Plan(route);assertTrue(plan.columns*plan.rows<=MapBackground.MAX_TILES);
        MapBackground.load(route,(z,x,y)->{assertTrue(x>=0&&x<(1<<z));assertTrue(y>=0&&y<(1<<z));return tile(z,x,y);});
    }
    public void testTilesAndRouteShareTheSameProjection() throws Exception {
        RouteData route=route();MapBackground.Plan plan=new MapBackground.Plan(route,960,600);
        MapBackground map=MapBackground.load(route,960,600,MapTest::tile);Bitmap out=Bitmap.createBitmap(960,600,Bitmap.Config.ARGB_8888);
        map.draw(new Canvas(out),route,0,0,960,600,1,Color.RED,Color.BLACK);
        MapBackground.Viewport viewport=new MapBackground.Viewport(route,960,600);
        RouteData.Point middle=route.points.get(50);
        assertEquals(Color.RED,out.getPixel(Math.round(viewport.x(middle,0,960)),Math.round(viewport.y(middle,0,600))));
        boolean matched=false;
        for(int x=60;x<900;x+=120)for(int y=60;y<500;y+=90) {
            double wx=viewport.centreX+(x-480)/viewport.scale,wy=viewport.centreY+(y-300)/viewport.scale;
            int expected=tileColor(Math.floorMod((int)Math.floor(wx*(1<<plan.zoom)),1<<plan.zoom),(int)Math.floor(wy*(1<<plan.zoom)));
            if(out.getPixel(x,y)==expected)matched=true;
        }
        assertTrue("Map pixels must align with geographic coordinates",matched);out.recycle();
    }
    public void testFourMapTreatmentsAcrossEveryShareDesign() throws Exception {
        RouteData data=route();RouteData visible=CardRenderer.privateRoute(data,true);
        RideStore.Ride ride=new RideStore.Ride();ride.id=99;ride.start=1795490000000L;ride.elapsed=120000;
        Set<Integer> styles=new HashSet<>();
        for(int style=1;style<=4;style++)for(int design=0;design<10;design++) {
            RectF frame=CardRenderer.mapFrame(design);MapBackground map=MapBackground.load(visible,frame.width(),frame.height(),MapTest::tile);
            Bitmap b=CardRenderer.render(ride,data,design,true,false,map,style);
            assertEquals(1080,b.getWidth());assertEquals(1350,b.getHeight());
            if(design==0){int[] pixels=new int[1080*1350];b.getPixels(pixels,0,1080,0,0,1080,1350);assertTrue(styles.add(Arrays.hashCode(pixels)));}
            b.recycle();
            Bitmap clear=CardRenderer.render(ride,data,design,true,true,map,style);assertEquals(0,Color.alpha(clear.getPixel(1079,600)));clear.recycle();
        }
        assertEquals(4,styles.size());
    }
    public void testTileFailureAndCancellationLeaveLineExportAvailable() throws Exception {
        RouteData route=route();
        try {MapBackground.load(route,(z,x,y)->{throw new IOException("Offline");});fail("Failure must be reported");}catch(IOException expected) {}
        try {
            Thread.currentThread().interrupt();
            try {MapBackground.load(route,MapTest::tile);fail("Cancelled load must stop");}catch(InterruptedIOException expected) {}
        } finally {Thread.interrupted();}
        RideStore.Ride ride=new RideStore.Ride();ride.start=1000;
        Bitmap plain=CardRenderer.render(ride,route,0,false,false),fallback=CardRenderer.render(ride,route,0,false,false,null,3);
        assertTrue(plain.sameAs(fallback));plain.recycle();fallback.recycle();
    }
    public void testInvalidTileImagesAreRejectedBeforeDecodingPixels() throws Exception {
        try {MapBackground.decodeTile(new ByteArrayInputStream("not an image".getBytes()));fail("Invalid image");}catch(IOException expected) {}
        try {MapBackground.decodeTile(new ByteArrayInputStream(new byte[1024*1024+1]));fail("Oversized response");}catch(IOException expected) {}
        Bitmap large=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);ByteArrayOutputStream bytes=new ByteArrayOutputStream();large.compress(Bitmap.CompressFormat.PNG,100,bytes);large.recycle();
        try {MapBackground.decodeTile(new ByteArrayInputStream(bytes.toByteArray()));fail("Unexpected dimensions");}catch(IOException expected) {}
        Bitmap valid=tile(1,1,1);bytes.reset();valid.compress(Bitmap.CompressFormat.PNG,100,bytes);valid.recycle();
        Bitmap decoded=MapBackground.decodeTile(new ByteArrayInputStream(bytes.toByteArray()));assertEquals(256,decoded.getWidth());decoded.recycle();
    }
    public void testEmptyOrHiddenRouteDoesNotFetchAnyTiles() throws Exception {
        int[] fetched={0};
        try {MapBackground.load(new RouteData(),(z,x,y)->{fetched[0]++;return tile(z,x,y);});fail("No map without a route");}catch(IllegalArgumentException expected) {}
        assertEquals(0,fetched[0]);
        RouteData shortRide=new RouteData();shortRide.add(12,77,1,1,4.0,1.0);
        assertTrue(CardRenderer.privateRoute(shortRide,true).points.isEmpty());
    }
}
