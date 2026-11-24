package com.kunstruct.ridetracker;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import android.widget.TextView;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class HomeWorkTest extends InstrumentationTestCase {
    private static final class CountingStore extends RideStore {
        final AtomicInteger history=new AtomicInteger(),active=new AtomicInteger(),latest=new AtomicInteger();
        CountingStore(Context context){super(context);}
        @Override public java.util.List<Ride> rides(){history.incrementAndGet();return super.rides();}
        @Override public Ride active(){active.incrementAndGet();return super.active();}
        @Override public Ride latest(){latest.incrementAndGet();return super.latest();}
    }
    private Object field(Object object,String name) throws Exception {Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void invoke(Activity activity,String method,Class<?>[] types,Object...args) throws Exception {
        Method m=MainActivity.class.getDeclaredMethod(method,types);m.setAccessible(true);
        getInstrumentation().runOnMainSync(()->{try{m.invoke(activity,args);}catch(Exception e){throw new RuntimeException(e);}});
    }
    public void testHistoryCachingStaticDetailAndBreakStatistics() throws Exception {
        assertTrue(Build.HARDWARE.equals("ranchu")||Build.HARDWARE.equals("goldfish"));Context context=getInstrumentation().getTargetContext();
        Activity activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        CountingStore store=new CountingStore(context);long ride=0;
        try {
            Handler main=(Handler)field(activity,"main");Runnable poll=(Runnable)field(activity,"poll");
            getInstrumentation().runOnMainSync(()->main.removeCallbacks(poll));
            ExecutorService io=(ExecutorService)field(activity,"io");io.submit(()->{}).get();getInstrumentation().waitForIdleSync();
            RideStore previous=(RideStore)field(activity,"store");Field sf=MainActivity.class.getDeclaredField("store");sf.setAccessible(true);
            getInstrumentation().runOnMainSync(()->{try{sf.set(activity,store);}catch(Exception e){throw new RuntimeException(e);}});previous.close();
            invoke(activity,"home",new Class<?>[]{});Thread.sleep(3500);
            assertEquals("Idle home must not scan full history each second",1,store.history.get());assertTrue(store.active.get()>=2);
            ride=store.newRide(System.currentTimeMillis()-300000);long segment=store.newSegment(ride,System.currentTimeMillis()-300000,0,1);
            List<RideStore.Sample> samples=new ArrayList<>();RouteData data=StatsTest.profile();
            for(int t=0;t<=300;t++) {
                RouteData.Point point=data.points.get(t);RideStore.Sample p=new RideStore.Sample();p.kind="gps";p.mono=point.mono;p.utc=p.received=1000+t*1000;
                p.lat=point.lat;p.lon=point.lon;p.accuracy=4.0;p.speed=t>=60&&t<=240?0.0:10.0;samples.add(p);
            }
            store.commit(ride,segment,samples,300000,300_000_000_000L);store.finish(ride,"saved","Stats fixture");
            long end=SystemClock.elapsedRealtime()+7000;while(store.history.get()<2&&SystemClock.elapsedRealtime()<end)Thread.sleep(100);
            assertTrue("Even a ride saved between polls must refresh history",store.history.get()>=2);
            invoke(activity,"showRide",new Class<?>[]{long.class},ride);
            end=SystemClock.elapsedRealtime()+7000;String[] detail={""};
            do {
                getInstrumentation().runOnMainSync(()->{try{detail[0]=((TextView)field(activity,"detailStats")).getText().toString();}catch(Exception e){throw new RuntimeException(e);}});
                if(detail[0].contains("Break time"))break;Thread.sleep(100);
            }while(SystemClock.elapsedRealtime()<end);
            assertTrue(detail[0],detail[0].contains("Ride time  0:02:00"));assertTrue(detail[0],detail[0].contains("Break time  0:03:00"));
            assertTrue(detail[0],detail[0].contains(new RideStats(data,300000).average()));assertTrue(detail[0],detail[0].contains("Top speed  36.0 km/h"));
            io.submit(()->{}).get();getInstrumentation().waitForIdleSync();int active=store.active.get(),latest=store.latest.get();Thread.sleep(2500);
            assertEquals("Static detail must not poll the recorder",active,store.active.get());assertEquals(latest,store.latest.get());
            int reads=store.history.get();invoke(activity,"home",new Class<?>[]{});Thread.sleep(1500);assertTrue(store.history.get()>reads);
        } finally {if(ride!=0)store.delete(ride);getInstrumentation().runOnMainSync(()->activity.finish());}
    }
}
