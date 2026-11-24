package com.kunstruct.ridetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import java.io.*;

/** Explicit manual provider check, excluded from the normal fixture-only suite. */
public final class LiveMapCheck extends InstrumentationTestCase {
    public void testSelectedMapExportsAndOfflineAvailability() throws Exception {
        assertTrue(android.os.Build.HARDWARE.equals("ranchu"));
        Context context=getInstrumentation().getTargetContext();
        try {
            RideStore.Ride ride=new RideStore.Ride();ride.id=99;ride.start=System.currentTimeMillis()-300000;ride.elapsed=300000;
            RouteData data=new RouteData();double north=0,east=0;
            for(int second=0;second<=300;second++) {
                boolean stopped=second>=60&&second<=240;
                if(second>0&&(second<=60||second>240)) {
                    int progress=second<=60?second:second-180;double angle=Math.sin(progress*.08)*1.1;
                    north+=10*Math.cos(angle);east+=10*Math.sin(angle);
                }
                data.add(12.95+north/111195,77.6+east/(111195*Math.cos(Math.toRadians(12.95))),second*1_000_000_000L,1,4.0,stopped?0.0:10.0);
            }
            assertEquals(180000,new RideStats(data,ride.elapsed).breakMillis);
            RouteData visible=CardRenderer.privateRoute(data,true);
            CardLabels names=new CardLabels("City start","Coffee stop");
            RectF frame=CardRenderer.mapFrame(0,true);
            MapBackground map=MapBackground.load(context,visible,frame.width(),frame.height());
            for(int style=1;style<=4;style++) {
                Bitmap image=CardRenderer.render(ride,data,0,true,false,map,style,names);
                try(FileOutputStream out=new FileOutputStream(new File(context.getCacheDir(),"live-map-"+style+".png"))) {
                    assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));
                } finally {image.recycle();}
            }
            shell("svc wifi disable");shell("svc data disable");Thread.sleep(1500);
            // Expired provider tiles require revalidation; offline availability is best effort.
            try {assertNotNull(MapBackground.load(context,visible,frame.width(),frame.height()));}
            catch(IOException offline) {
                Bitmap line=CardRenderer.render(ride,data,0,true,false,null,1,names);
                assertEquals(1080,line.getWidth());line.recycle();
            }
            RouteData uncached=new RouteData();uncached.add(0,0,1,1,4.0,1.0);uncached.add(.001,.001,2_000_000_000L,1,4.0,1.0);
            try {MapBackground.load(context,uncached,frame.width(),frame.height());fail("Uncached map should report offline");}
            catch(IOException expected) {}
        } finally {shell("svc wifi enable");shell("svc data enable");}
    }
    private void shell(String command) throws IOException {
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))){in.readAllBytes();}
    }
}
