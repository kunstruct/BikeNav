package com.kunstruct.ridetracker;

import android.app.Activity;
import android.content.*;
import android.location.*;
import android.location.provider.ProviderProperties;
import android.os.*;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Explicit 60-second before/after workload check; emulator battery percentage is not a battery benchmark. */
public final class RecorderEnergyCheck extends InstrumentationTestCase {
    private String shell(String cmd) throws IOException {
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(cmd))) {
            return new String(in.readAllBytes(),StandardCharsets.UTF_8).trim();
        }
    }
    private JSONObject notifications() throws Exception {
        JSONArray current=new JSONObject(shell("dumpsys notification --stats")).getJSONObject("stats").getJSONArray("current");
        for(int i=0;i<current.length();i++)if("com.kunstruct.ridetracker".equals(current.getJSONObject(i).optString("key")))return current.getJSONObject(i);
        return new JSONObject();
    }
    private long cpu(String pid) throws Exception {
        String stat=shell("run-as com.kunstruct.ridetracker cat /proc/"+pid+"/stat");
        String[] fields=stat.substring(stat.lastIndexOf(')')+1).trim().split("\\s+");
        return Long.parseLong(fields[11])+Long.parseLong(fields[12]);
    }
    public void testSixtySecondsScreenOffRecorderWork() throws Exception {
        assertTrue(Build.HARDWARE.equals("ranchu")||Build.HARDWARE.equals("goldfish"));
        Context context=getInstrumentation().getTargetContext();RideStore store=new RideStore(context);
        Activity activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();LocationManager manager=context.getSystemService(LocationManager.class);
        shell("appops set com.kunstruct.ridetracker android:mock_location allow");
        manager.addTestProvider("gps",new ProviderProperties.Builder().setHasSatelliteRequirement(true).setHasSpeedSupport(true)
                .setAccuracy(ProviderProperties.ACCURACY_FINE).setPowerUsage(ProviderProperties.POWER_USAGE_HIGH).build());manager.setTestProviderEnabled("gps",true);
        try {
            context.startForegroundService(new Intent(context,RecorderService.class).setAction(RecorderService.START));
            long timeout=SystemClock.elapsedRealtime()+20000;
            while((store.active()==null||store.active().accel<30)&&SystemClock.elapsedRealtime()<timeout)Thread.sleep(200);
            assertNotNull(store.active());String pid=shell("pidof com.kunstruct.ridetracker:recorder");assertFalse(pid.isEmpty());
            shell("input keyevent 223");Thread.sleep(500);
            JSONObject before=notifications();long cpuBefore=cpu(pid);RideStore.Ride initial=store.active();long start=SystemClock.elapsedRealtime();
            for(int i=0;i<60;i++) {
                Location fix=new Location("gps");fix.setLatitude(12.95+i*.00007);fix.setLongitude(77.6);fix.setAccuracy(4);fix.setSpeed(10);
                fix.setTime(System.currentTimeMillis());fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());manager.setTestProviderLocation("gps",fix);
                Thread.sleep(Math.max(1,start+(i+1)*1000-SystemClock.elapsedRealtime()));
            }
            RideStore.Ride after=store.active();assertNotNull(after);assertEquals(initial.id,after.id);
            JSONObject finish=notifications(),report=new JSONObject();
            report.put("version",context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName);
            report.put("duration_ms",SystemClock.elapsedRealtime()-start);report.put("recorder_cpu_ticks",cpu(pid)-cpuBefore);
            report.put("notification_enqueues",finish.optInt("numEnqueuedByApp")-before.optInt("numEnqueuedByApp"));
            report.put("notification_updates",finish.optInt("numUpdatedByApp")-before.optInt("numUpdatedByApp"));
            report.put("gps_samples",after.gps-initial.gps);report.put("raw_samples",after.accel-initial.accel);report.put("linear_samples",after.linear-initial.linear);
            report.put("checkpoint_age_ms",System.currentTimeMillis()-after.checkpoint);
            assertTrue(after.accel-initial.accel>1300);assertTrue(after.gps-initial.gps>=55);assertTrue(System.currentTimeMillis()-after.checkpoint<5000);
            try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"recorder-work.json"))){out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        } finally {
            context.startService(new Intent(context,RecorderService.class).setAction(RecorderService.STOP));
            long stop=SystemClock.elapsedRealtime()+10000;while(store.active()!=null&&SystemClock.elapsedRealtime()<stop)Thread.sleep(200);
            manager.removeTestProvider("gps");shell("appops set com.kunstruct.ridetracker android:mock_location default");
            shell("input keyevent 224");shell("wm dismiss-keyguard");getInstrumentation().runOnMainSync(()->activity.finish());store.close();
        }
    }
}
