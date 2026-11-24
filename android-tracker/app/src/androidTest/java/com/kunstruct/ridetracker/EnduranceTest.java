package com.kunstruct.ridetracker;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.location.*;
import android.location.provider.ProviderProperties;
import android.os.*;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class EnduranceTest extends InstrumentationTestCase {
    @Override protected void setUp() throws Exception {
        super.setUp();assertTrue("Synthetic endurance tests require a dedicated emulator",Build.HARDWARE.equals("ranchu")||Build.HARDWARE.equals("goldfish"));
    }
    private String shell(String command) throws IOException {
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            return new String(in.readAllBytes(),StandardCharsets.UTF_8).trim();
        }
    }
    public void testTenMinutesScreenOffWithGpsAndMotion() throws Exception {
        Context context=getInstrumentation().getTargetContext();RideStore store=new RideStore(context);
        Activity activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();LocationManager manager=context.getSystemService(LocationManager.class);
        shell("appops set com.kunstruct.ridetracker android:mock_location allow");
        manager.addTestProvider("gps",new ProviderProperties.Builder().setHasSatelliteRequirement(true).setHasSpeedSupport(true)
                .setAccuracy(ProviderProperties.ACCURACY_FINE).setPowerUsage(ProviderProperties.POWER_USAGE_HIGH).build());
        manager.setTestProviderEnabled("gps",true);
        try {
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            long wait=SystemClock.elapsedRealtime()+20000;
            while((store.active()==null||store.active().accel<20)&&SystemClock.elapsedRealtime()<wait)Thread.sleep(200);
            RideStore.Ride initial=store.active();assertNotNull(initial);assertTrue(initial.accel>=20);
            shell("input keyevent 223");
            Thread.sleep(1000);assertFalse("Screen must actually be off",context.getSystemService(PowerManager.class).isInteractive());
            long began=SystemClock.elapsedRealtime(),next=began;int injected=0;
            while(SystemClock.elapsedRealtime()-began<600000) {
                boolean stopped=injected>=200&&injected<400;int progress=injected<200?injected:stopped?200:injected-200;
                Location fix=new Location("gps");fix.setLatitude(12.95+progress*.00007);fix.setLongitude(77.6+Math.sin(progress*.02)*.003);
                fix.setAccuracy(4);fix.setSpeed(stopped?0:10);fix.setTime(System.currentTimeMillis());fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                manager.setTestProviderLocation("gps",fix);injected++;
                if(injected%30==0) {
                    RideStore.Ride current=store.active();assertNotNull(current);assertEquals(initial.id,current.id);
                    assertTrue("Durable writes must continue",System.currentTimeMillis()-current.checkpoint<5000);
                }
                next+=1000;Thread.sleep(Math.max(1,next-SystemClock.elapsedRealtime()));
            }
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            context.startService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            wait=SystemClock.elapsedRealtime()+10000;while(store.active()!=null&&SystemClock.elapsedRealtime()<wait)Thread.sleep(200);
            assertNull(store.active());RideStore.Ride result=store.ride(initial.id);
            assertTrue("At least 90% of injected GPS fixes must be durable",result.gps>=injected*.9);
            assertTrue("Motion stream must remain continuous",result.accel>12000);
            JSONObject report=new JSONObject();report.put("android",Build.VERSION.RELEASE);report.put("sdk",Build.VERSION.SDK_INT);
            report.put("ride_id",result.id);report.put("elapsed_ms",result.elapsed);report.put("injected_gps",injected);
            report.put("gps_samples",result.gps);report.put("raw_motion_samples",result.accel);report.put("linear_motion_samples",result.linear);
            RideStats stats=new RideStats(RouteData.load(store,result.id),result.elapsed);
            report.put("ride_ms",stats.rideMillis);report.put("break_ms",stats.breakMillis);report.put("break_count",stats.breaks);
            assertEquals("The stationary segment must be one break",1,stats.breaks);
            assertTrue("The whole stationary segment must be excluded",stats.breakMillis>190000&&stats.breakMillis<210000);
            for(String kind:new String[]{"gps","accel","linear"}) {
                double maximum=0;long previous=0,priorSegment=-1;
                try(Cursor c=store.getReadableDatabase().rawQuery("SELECT mono_ns,segment_id FROM samples WHERE ride_id=? AND kind=? ORDER BY id",new String[]{""+result.id,kind})) {
                    while(c.moveToNext()) {
                        long mono=c.getLong(0),segment=c.getLong(1);
                        if(previous>0&&segment==priorSegment)maximum=Math.max(maximum,(mono-previous)/1e9);
                        previous=mono;priorSegment=segment;
                    }
                }
                report.put(kind+"_max_gap_seconds",maximum);
                if(!kind.equals("linear")||result.linear>0)assertTrue(kind+" stream gap: "+maximum,maximum<(kind.equals("gps")?5:2));
            }
            File file=new File(context.getFilesDir(),"endurance.json");try(FileOutputStream out=new FileOutputStream(file)){out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
            try(Cursor c=store.getReadableDatabase().rawQuery("PRAGMA quick_check",null)){c.moveToFirst();assertEquals("ok",c.getString(0));}
        } finally {
            manager.removeTestProvider("gps");shell("appops set com.kunstruct.ridetracker android:mock_location default");
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            getInstrumentation().runOnMainSync(()->activity.finish());store.close();
        }
    }
}
