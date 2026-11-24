package com.kunstruct.ridetracker;

import android.graphics.*;
import android.test.AndroidTestCase;
import java.util.*;

public final class CardLabelsTest extends AndroidTestCase {
    @Override protected void setUp() throws Exception {
        super.setUp();assertTrue("Metadata tests require an emulator",android.os.Build.HARDWARE.equals("ranchu")||android.os.Build.HARDWARE.equals("goldfish"));
    }
    public void testBlankLongAndUnicodeNames() {
        assertFalse(new CardLabels(null," \n\t ").hasNames());
        CardLabels names=new CardLabels("  Bengaluru\n Central  ","ನಂದಿ ಬೆಟ್ಟ 🏍️");
        assertEquals("Bengaluru Central",names.start);assertEquals("ನಂದಿ ಬೆಟ್ಟ 🏍️",names.finish);
        String emoji="🏍".repeat(100);CardLabels longName=new CardLabels(emoji,"");
        assertEquals(80,longName.start.codePointCount(0,longName.start.length()));
        assertTrue(Character.isLowSurrogate(longName.start.charAt(longName.start.length()-1)));
    }
    public void testNamesPersistPerRideAndClearIndependently() {
        long a=9000000001L,b=9000000002L;
        try {
            new CardLabels("Home","Hills").save(getContext(),a);new CardLabels("Office","").save(getContext(),b);
            assertEquals("Hills",CardLabels.load(getContext(),a).finish);assertEquals("Office",CardLabels.load(getContext(),b).start);
            CardLabels.clear(getContext(),a);assertFalse(CardLabels.load(getContext(),a).hasNames());assertEquals("Office",CardLabels.load(getContext(),b).start);
            CardLabels.EMPTY.save(getContext(),b);assertFalse(CardLabels.load(getContext(),b).hasNames());
        } finally {CardLabels.clear(getContext(),a);CardLabels.clear(getContext(),b);}
    }
    public void testOptionalNameRowsStayInsideEveryRoutePanel() {
        RouteData data=MapTest.route();RideStore.Ride ride=new RideStore.Ride();ride.start=1000;ride.elapsed=120000;
        CardLabels[] options={CardLabels.EMPTY,new CardLabels("Bengaluru",""),new CardLabels("","Nandi Hills"),
                new CardLabels("Bengaluru","ನಂದಿ ಬೆಟ್ಟ 🏍️"),new CardLabels("A very long place name ".repeat(10),"وجهة الرحلة")};
        for(int design=0;design<10;design++)for(boolean clear:new boolean[]{false,true}) {
            Bitmap baseline=CardRenderer.render(ride,data,design,true,clear);Set<Integer> hashes=new HashSet<>();
            for(CardLabels names:options) {
                Bitmap image=CardRenderer.render(ride,data,design,true,clear,null,0,names);
                if(!names.hasNames())assertTrue("Blank names must preserve the old card",baseline.sameAs(image));
                int[] pixels=new int[1080*1350];image.getPixels(pixels,0,1080,0,0,1080,1350);assertTrue(hashes.add(Arrays.hashCode(pixels)));
                RectF frame=CardRenderer.mapFrame(design);
                for(int y=0;y<1350;y+=7)for(int x=0;x<1080;x+=7)if(!frame.contains(x,y))assertEquals("Names must not cover title, stats or privacy footer",baseline.getPixel(x,y),image.getPixel(x,y));
                image.recycle();
            }
            baseline.recycle();
        }
    }
    public void testNamedMapsAlignAcrossStylesAndKeepTransparentOutsidePanel() throws Exception {
        RouteData data=MapTest.route(),visible=CardRenderer.privateRoute(data,true);CardLabels names=new CardLabels("Bengaluru","Nandi Hills");
        RideStore.Ride ride=new RideStore.Ride();ride.start=1000;
        for(int design=0;design<10;design++) {
            RectF original=CardRenderer.mapFrame(design),frame=CardRenderer.mapFrame(design,true);
            assertEquals(original.bottom,frame.bottom);assertEquals(original.width(),frame.width());assertEquals(original.top+82,frame.top);
            MapBackground map=MapBackground.load(visible,frame.width(),frame.height(),MapTest::tile);
            for(int style=1;style<=4;style++)for(boolean clear:new boolean[]{false,true}) {
                Bitmap image=CardRenderer.render(ride,data,design,true,clear,map,style,names);
                assertEquals(1080,image.getWidth());assertEquals(1350,image.getHeight());
                assertEquals(255,Color.alpha(image.getPixel((int)original.left+20,(int)original.top+10)));
                if(clear)assertEquals(0,Color.alpha(image.getPixel(1079,600)));image.recycle();
            }
            Bitmap projected=Bitmap.createBitmap(1080,1350,Bitmap.Config.ARGB_8888);
            map.draw(new Canvas(projected),visible,frame.left,frame.top,frame.width(),frame.height(),1,Color.RED,Color.BLACK);
            MapBackground.Viewport viewport=new MapBackground.Viewport(visible,frame.width(),frame.height());RouteData.Point middle=visible.points.get(visible.points.size()/2);
            assertEquals(Color.RED,projected.getPixel(Math.round(viewport.x(middle,frame.left,frame.width())),Math.round(viewport.y(middle,frame.top,frame.height()))));projected.recycle();
        }
    }
}
