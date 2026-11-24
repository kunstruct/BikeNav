package com.kunstruct.ridetracker;

import android.content.Context;
import android.os.Build;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Explicit check that the final analytics build preserves/replays the completed raw endurance run. */
public final class EnduranceReplayCheck extends InstrumentationTestCase {
    public void testCompletedRecordingReplaysOnCurrentBuild() throws Exception {
        assertTrue(Build.HARDWARE.equals("ranchu")||Build.HARDWARE.equals("goldfish"));Context context=getInstrumentation().getTargetContext();
        JSONObject previous;
        try(InputStream in=context.openFileInput("endurance.json")){previous=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}
        try(RideStore store=new RideStore(context)) {
            RideStore.Ride ride=store.ride(previous.getLong("ride_id"));assertNotNull(ride);
            assertEquals(previous.getLong("gps_samples"),ride.gps);assertEquals(previous.getLong("raw_motion_samples"),ride.accel);
            assertEquals(previous.getLong("linear_motion_samples"),ride.linear);assertEquals("saved",ride.status);
            RouteData data=RouteData.load(store,ride.id);RideStats stats=new RideStats(data,ride.elapsed);
            assertEquals(previous.getLong("break_ms"),stats.breakMillis);assertEquals(previous.getLong("ride_ms"),stats.rideMillis);assertEquals(1,stats.breaks);
            JSONObject report=new JSONObject();report.put("ride_id",ride.id);report.put("ride_ms",stats.rideMillis);report.put("break_ms",stats.breakMillis);
            report.put("distance_m",data.distance);report.put("average_kmh",stats.averageKmh);report.put("top_kmh",stats.topKmh);
            report.put("raw_counts_unchanged",true);
            try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"endurance-replay.json"))){out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        }
    }
}
