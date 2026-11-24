package com.kunstruct.ridetracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import java.util.Locale;

final class RideExport {
    static File zip(File cache,RideStore store,long ride) throws Exception {
        File result=new File(cache,"ride-"+ride+"-"+System.currentTimeMillis()+".zip");
        SQLiteDatabase db=store.getReadableDatabase();
        // A separate read connection gives a consistent export without a long write transaction.
        try(SQLiteDatabase snapshot=SQLiteDatabase.openDatabase(db.getPath(),null,SQLiteDatabase.OPEN_READONLY);
            ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)))) {
            snapshot.beginTransactionReadOnly();
            try {
                csv(zip,snapshot,"ride.csv","SELECT * FROM rides WHERE id=?",ride);
                csv(zip,snapshot,"segments.csv","SELECT * FROM segments WHERE ride_id=? ORDER BY id",ride);
                csv(zip,snapshot,"gps.csv","SELECT * FROM samples WHERE ride_id=? AND kind='gps' ORDER BY id",ride);
                csv(zip,snapshot,"motion.csv","SELECT * FROM samples WHERE ride_id=? AND kind<>'gps' ORDER BY id",ride);
                csv(zip,snapshot,"events.csv","SELECT * FROM events WHERE ride_id=? ORDER BY id",ride);
                summary(zip,snapshot,ride);
                zip.putNextEntry(new ZipEntry("README.txt"));
                zip.write(("Ride Tracker 0.4.0 / schema 1 / derived analytics 2\n"+
                        "UTC timestamps: milliseconds since Unix epoch. mono_ns: elapsedRealtimeNanos, comparable within the same device boot; see segments.boot_count.\n"+
                        "GPS utc_ms comes from Location.getTime(); received_utc_ms is callback wall time. Motion utc_ms is anchored to segment start UTC plus monotonic delta.\n"+
                        "Raw accel includes gravity; linear (if present) excludes gravity. x/y/z in m/s^2, phone axes, signed values; not calibrated motorcycle braking or lean.\n"+
                        "GPS speed in m/s, altitude/accuracy in metres. Motion accuracy is Android sensor status (not metres). Empty CSV fields mean unavailable. mock=1 identifies mock GPS.\n"+
                        "All valid delivered samples are retained; filtered route/metrics do not alter raw samples. GPS gaps/restarts are not fabricated.\n"+
                        "Recorder requests GPS 1 Hz, motion 25 Hz per stream. Actual hardware delivery rates may differ. Batches commit each second or 128 samples.\n"+
                        "elapsed_ms is sum of recorded segment time, including stops; excludes interruption downtime. Events describe gaps and failures.\n"+
                        "summary.csv recomputes current derived analytics; ride.csv analytics_version is the historical recording value. Raw CSV samples are unchanged.\n"+
                        "Ride time = recorded time minus confirmed GPS stops >=120s; low speed <=0.5m/s, accuracy <=25m and 25m cluster. Resume: sustained >=1.5m/s or leave cluster.\n"+
                        "Short stops, missing/poor GPS or speed and gaps remain ride time. Confirmed stop drift is excluded from distance. Top speed uses supported adjacent/median-of-three GPS speeds, not isolated peaks.\n"+
                        "Sensitive location and motion data: share this ZIP only intentionally.\n").getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            } finally {snapshot.endTransaction();}
        } catch(Exception e) {if(!result.delete()) result.deleteOnExit();throw e;}
        return result;
    }
    private static void summary(ZipOutputStream zip,SQLiteDatabase db,long ride) throws IOException {
        long elapsed;
        try(Cursor c=db.rawQuery("SELECT elapsed_ms FROM rides WHERE id=?",new String[]{""+ride})) {
            if(!c.moveToFirst())throw new IOException("Ride is no longer available");elapsed=c.getLong(0);
        }
        RouteData route=RouteData.load(db,ride);RideStats stats=new RideStats(route,elapsed);
        String csv="analytics_version,recorded_ms,ride_ms,break_ms,break_count,distance_m,average_kmh,top_kmh,accepted_gps,rejected_gps,route_gaps\n"+
                String.format(Locale.US,"%d,%d,%d,%d,%d,%.3f,%s,%s,%d,%d,%d\n",RideStats.VERSION,stats.recordedMillis,stats.rideMillis,stats.breakMillis,stats.breaks,route.distance,
                        stats.hasAverage?String.format(Locale.US,"%.3f",stats.averageKmh):"",stats.hasTop?String.format(Locale.US,"%.3f",stats.topKmh):"",route.points.size(),route.rejected,route.gaps);
        zip.putNextEntry(new ZipEntry("summary.csv"));zip.write(csv.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }
    private static void csv(ZipOutputStream zip,SQLiteDatabase db,String name,String query,long ride) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Writer writer=new BufferedWriter(new OutputStreamWriter(zip,StandardCharsets.UTF_8));
        try(Cursor c=db.rawQuery(query,new String[]{""+ride})) {
            String[] columns=c.getColumnNames();
            for(int i=0;i<columns.length;i++) {if(i>0)writer.write(',');writer.write(columns[i]);}writer.write('\n');
            while(c.moveToNext()) {
                for(int i=0;i<columns.length;i++) {
                    if(i>0)writer.write(',');
                    if(!c.isNull(i)) {writer.write('"');writer.write(c.getString(i).replace("\"","\"\""));writer.write('"');}
                }
                writer.write('\n');
            }
        }
        writer.flush();zip.closeEntry();
    }
    static void clean(File cache,long ride) {
        File[] files=cache.listFiles();if(files==null)return;
        for(File f:files) if(f.getName().matches("ride-"+ride+"-[0-9]+\\.(zip|png)")) f.delete();
    }
}
