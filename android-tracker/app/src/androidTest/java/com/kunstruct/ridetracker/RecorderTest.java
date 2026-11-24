package com.kunstruct.ridetracker;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.*;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Runs on the isolated test emulator with precise location and notifications pre-granted. */
public final class RecorderTest extends InstrumentationTestCase {
    @Override protected void setUp() throws Exception {
        super.setUp();assertTrue("Fault tests require a dedicated emulator",Build.HARDWARE.equals("ranchu")||Build.HARDWARE.equals("goldfish"));
    }
    private Context context;
    private RideStore store;
    private Activity activity;
    private String shell(String command) throws IOException {
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            return new String(in.readAllBytes(),StandardCharsets.UTF_8).trim();
        }
    }
    public void testGpsOutagePreservesMotionAndRecovers() throws Exception {
        context=getInstrumentation().getTargetContext();store=new RideStore(context);
        activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();
        LocationManager manager=context.getSystemService(LocationManager.class);
        shell("appops set com.kunstruct.ridetracker android:mock_location allow");
        manager.addTestProvider("gps",new ProviderProperties.Builder().setHasSatelliteRequirement(true)
                .setHasSpeedSupport(true).setHasAltitudeSupport(true).setAccuracy(ProviderProperties.ACCURACY_FINE)
                .setPowerUsage(ProviderProperties.POWER_USAGE_HIGH).build());
        manager.setTestProviderEnabled("gps",true);
        try {
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            RideStore.Ride first=waitForSamples(20,20000);
            shell("input keyevent 223");
            for(int i=0;i<12;i++) {fix(manager,i);Thread.sleep(1100);}
            Thread.sleep(1500);RideStore.Ride before=store.active();assertTrue("GPS stream must be persisted",before.gps>=10);
            manager.setTestProviderEnabled("gps",false);Thread.sleep(12000);
            RideStore.Ride outage=store.active();assertEquals(first.id,outage.id);
            assertTrue("Motion must continue during GPS loss",outage.accel>before.accel+150);
            assertTrue("Cached provider state must show GPS disabled",outage.note.contains("GPS is switched off"));
            boolean warning=false;for(android.service.notification.StatusBarNotification n:context.getSystemService(android.app.NotificationManager.class).getActiveNotifications())if(n.getId()==9)warning=true;
            assertTrue("GPS loss must still post an attention notification",warning);
            manager.setTestProviderEnabled("gps",true);
            for(int i=12;i<18;i++){fix(manager,i);Thread.sleep(1100);}
            Thread.sleep(1500);assertTrue("GPS must recover without a new ride",store.active().gps>before.gps);
            for(android.service.notification.StatusBarNotification n:context.getSystemService(android.app.NotificationManager.class).getActiveNotifications())assertTrue("Recovered GPS must clear attention notification",n.getId()!=9);
            try(Cursor c=store.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM samples WHERE ride_id=? AND kind='gps' AND mock=1",new String[]{""+first.id})) {
                c.moveToFirst();assertTrue(c.getLong(0)>=10);
            }
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            context.startService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            long end=SystemClock.elapsedRealtime()+10000;
            while(store.active()!=null&&SystemClock.elapsedRealtime()<end)Thread.sleep(250);
            assertNull(store.active());
        } finally {
            manager.removeTestProvider("gps");shell("appops set com.kunstruct.ridetracker android:mock_location default");
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            getInstrumentation().runOnMainSync(()->activity.finish());store.close();
        }
    }
    private void fix(LocationManager manager,int index) {
        Location fix=new Location("gps");fix.setLatitude(12.95+index*.0001);fix.setLongitude(77.6+Math.sin(index*.1)*.0001);
        fix.setAccuracy(4);fix.setSpeed(10);fix.setAltitude(900);fix.setTime(System.currentTimeMillis());
        fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());manager.setTestProviderLocation("gps",fix);
    }
    public void testSlowFailedMapLoadDoesNotInterruptRecording() throws Exception {
        context=getInstrumentation().getTargetContext();store=new RideStore(context);
        activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();LocationManager manager=context.getSystemService(LocationManager.class);
        shell("appops set com.kunstruct.ridetracker android:mock_location allow");
        manager.addTestProvider("gps",new ProviderProperties.Builder().setHasSatelliteRequirement(true).setHasSpeedSupport(true)
                .setAccuracy(ProviderProperties.ACCURACY_FINE).setPowerUsage(ProviderProperties.POWER_USAGE_HIGH).build());
        manager.setTestProviderEnabled("gps",true);
        try {
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            RideStore.Ride before=waitForSamples(30,20000);
            try {
                MapBackground.load(MapTest.route(),(z,x,y)->{
                    for(int i=0;i<5;i++){fix(manager,i);SystemClock.sleep(1000);}
                    throw new IOException("Simulated slow map connection failure");
                });fail("Map error must be reported");
            } catch(IOException expected) {}
            Thread.sleep(1500);RideStore.Ride after=store.active();assertNotNull(after);assertEquals(before.id,after.id);
            assertTrue("Motion must survive failed map loading",after.accel>before.accel+100);
            assertTrue("GPS must survive failed map loading",after.gps>=before.gps+4);
            assertTrue(System.currentTimeMillis()-after.checkpoint<5000);
        } finally {
            context.startService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            long end=SystemClock.elapsedRealtime()+10000;while(store.active()!=null&&SystemClock.elapsedRealtime()<end)Thread.sleep(250);
            manager.removeTestProvider("gps");shell("appops set com.kunstruct.ridetracker android:mock_location default");
            getInstrumentation().runOnMainSync(()->activity.finish());store.close();
        }
    }
    public void testForcedDozeWithBatteryExemptionKeepsBothStreams() throws Exception {
        context=getInstrumentation().getTargetContext();store=new RideStore(context);
        activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();
        PowerManager power=context.getSystemService(PowerManager.class);boolean wasExempt=power.isIgnoringBatteryOptimizations(context.getPackageName());
        LocationManager manager=context.getSystemService(LocationManager.class);
        shell("dumpsys deviceidle whitelist +com.kunstruct.ridetracker");
        shell("appops set com.kunstruct.ridetracker android:mock_location allow");
        manager.addTestProvider("gps",new ProviderProperties.Builder().setHasSatelliteRequirement(true).setHasSpeedSupport(true)
                .setAccuracy(ProviderProperties.ACCURACY_FINE).setPowerUsage(ProviderProperties.POWER_USAGE_HIGH).build());
        manager.setTestProviderEnabled("gps",true);
        try {
            assertTrue(power.isIgnoringBatteryOptimizations(context.getPackageName()));
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            RideStore.Ride before=waitForSamples(30,20000);
            shell("dumpsys battery unplug");shell("input keyevent 223");String idleResult=shell("dumpsys deviceidle force-idle");
            long idleDeadline=SystemClock.elapsedRealtime()+5000;
            while(!power.isDeviceIdleMode()&&SystemClock.elapsedRealtime()<idleDeadline)Thread.sleep(100);
            assertTrue("Device must really enter Doze: "+idleResult,power.isDeviceIdleMode());
            for(int i=0;i<30;i++){fix(manager,i);Thread.sleep(1000);}
            RideStore.Ride during=store.active();assertNotNull(during);assertEquals(before.id,during.id);
            assertTrue("Motion must persist during Doze",during.accel>=before.accel+600);
            assertTrue("GPS must persist during Doze",during.gps>=before.gps+25);
            assertTrue("Writes must stay fresh during Doze",System.currentTimeMillis()-during.checkpoint<5000);
        } finally {
            shell("dumpsys deviceidle unforce");shell("dumpsys battery reset");shell("input keyevent 224");shell("wm dismiss-keyguard");
            context.startService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            long end=SystemClock.elapsedRealtime()+10000;while(store.active()!=null&&SystemClock.elapsedRealtime()<end)Thread.sleep(250);
            manager.removeTestProvider("gps");shell("appops set com.kunstruct.ridetracker android:mock_location default");
            if(!wasExempt)shell("dumpsys deviceidle whitelist -com.kunstruct.ridetracker");
            getInstrumentation().runOnMainSync(()->activity.finish());store.close();
        }
    }
    public void testRapidStopStartKeepsTheNewRideRunning() throws Exception {
        context=getInstrumentation().getTargetContext();store=new RideStore(context);
        activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();
        try {
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            RideStore.Ride before=waitForSamples(30,20000);
            context.startService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            long end=SystemClock.elapsedRealtime()+20000;RideStore.Ride after=null;
            while(SystemClock.elapsedRealtime()<end) {
                after=store.active();if(after!=null&&after.id!=before.id&&after.accel>=30)break;Thread.sleep(250);
            }
            assertNotNull(after);assertTrue(after.id!=before.id);assertTrue(after.accel>=30);
            assertEquals("saved",store.ride(before.id).status);
            context.startService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            end=SystemClock.elapsedRealtime()+10000;while(store.active()!=null&&SystemClock.elapsedRealtime()<end)Thread.sleep(250);
            assertNull(store.active());
        } finally {getInstrumentation().runOnMainSync(()->activity.finish());store.close();}
    }
    public void testRecorderProcessDeathRestartsIntoNewSegment() throws Exception {
        context=getInstrumentation().getTargetContext();store=new RideStore(context);
        activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();
        try {
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            RideStore.Ride before=waitForSamples(40,20000);
            String pid=shell("pidof com.kunstruct.ridetracker:recorder");assertTrue(pid.matches("[0-9]+"));
            shell("input keyevent 223");
            shell("run-as com.kunstruct.ridetracker kill -9 "+pid);
            RideStore.Ride after=waitForSamples(before.accel+50,45000);assertEquals(before.id,after.id);
            try(Cursor c=store.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM segments WHERE ride_id=?",new String[]{""+before.id})) {
                c.moveToFirst();assertTrue("Restart must be a distinct segment",c.getInt(0)>=2);
            }
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            long end=SystemClock.elapsedRealtime()+10000;
            while(store.active()!=null&&SystemClock.elapsedRealtime()<end)Thread.sleep(250);
            assertNull(store.active());assertTrue(store.ride(before.id).accel>=before.accel);
        } finally {
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            getInstrumentation().runOnMainSync(()->activity.finish());store.close();
        }
    }
    private RideStore.Ride waitForSamples(long minimum,long timeout) throws Exception {
        long end=SystemClock.elapsedRealtime()+timeout;
        while(SystemClock.elapsedRealtime()<end) {
            RideStore.Ride ride=store.active();
            if(ride!=null&&ride.accel>=minimum)return ride;
            Thread.sleep(250);
        }
        fail("Timed out waiting for "+minimum+" durable motion samples");return null;
    }
    public void testScreenOffDuplicateStartAndStop() throws Exception {
        context=getInstrumentation().getTargetContext();store=new RideStore(context);
        Intent launch=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity=getInstrumentation().startActivitySync(launch);getInstrumentation().waitForIdleSync();
        try {
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            RideStore.Ride first=waitForSamples(30,20000);long id=first.id;
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            Thread.sleep(1500);assertEquals(id,store.active().id);
            shell("input keyevent 223");Thread.sleep(10000);
            RideStore.Ride locked=store.active();assertNotNull(locked);assertEquals(id,locked.id);
            assertTrue("Motion must continue with screen off",locked.accel>first.accel+100);
            assertTrue("A durable checkpoint must remain fresh",System.currentTimeMillis()-locked.checkpoint<5000);
            try(Cursor c=store.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM rides WHERE status='recording'",null)) {c.moveToFirst();assertEquals(1,c.getInt(0));}
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            long end=SystemClock.elapsedRealtime()+10000;
            while(store.active()!=null&&SystemClock.elapsedRealtime()<end)Thread.sleep(250);
            assertNull(store.active());RideStore.Ride saved=store.ride(id);assertEquals("saved",saved.status);
            long count=saved.accel;Thread.sleep(1500);assertEquals(count,store.ride(id).accel);
            File zip=RideExport.zip(context.getCacheDir(),store,id);assertTrue(zip.length()>0);zip.delete();
        } finally {
            shell("input keyevent 224");shell("wm dismiss-keyguard");
            getInstrumentation().runOnMainSync(()->activity.finish());store.close();
        }
    }
}
