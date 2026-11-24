package com.kunstruct.ridetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import java.util.ArrayList;
import java.util.List;

/** The database is the source of truth; recording never depends on an Activity. */
public class RideStore extends SQLiteOpenHelper {
    public static final String NAME = "rides.db";

    public RideStore(Context context) { this(context, NAME); }
    RideStore(Context context, String name) {
        super(context, name, null, 1);
        // Configure the connection pool itself: a PRAGMA in onConfigure can be reset by Android.
        setOpenParams(new SQLiteDatabase.OpenParams.Builder()
                .addOpenFlags(SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)
                .setJournalMode(SQLiteDatabase.JOURNAL_MODE_WAL)
                .setSynchronousMode(SQLiteDatabase.SYNC_MODE_FULL).build());
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE rides (id INTEGER PRIMARY KEY, start_utc_ms INTEGER NOT NULL, " +
                "end_utc_ms INTEGER, status TEXT NOT NULL, elapsed_ms INTEGER NOT NULL DEFAULT 0, " +
                "gps_count INTEGER NOT NULL DEFAULT 0, accel_count INTEGER NOT NULL DEFAULT 0, " +
                "linear_count INTEGER NOT NULL DEFAULT 0, last_gps_utc_ms INTEGER, last_motion_utc_ms INTEGER, " +
                "checkpoint_utc_ms INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '', analytics_version INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE UNIQUE INDEX one_recording ON rides(status) WHERE status='recording'");
        db.execSQL("CREATE TABLE segments (id INTEGER PRIMARY KEY, ride_id INTEGER NOT NULL REFERENCES rides(id) ON DELETE CASCADE, " +
                "start_utc_ms INTEGER NOT NULL, start_mono_ns INTEGER NOT NULL, end_mono_ns INTEGER NOT NULL, boot_count INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE samples (id INTEGER PRIMARY KEY, ride_id INTEGER NOT NULL REFERENCES rides(id) ON DELETE CASCADE, " +
                "segment_id INTEGER NOT NULL REFERENCES segments(id) ON DELETE CASCADE, kind TEXT NOT NULL, " +
                "utc_ms INTEGER NOT NULL, mono_ns INTEGER NOT NULL, received_utc_ms INTEGER NOT NULL, " +
                "latitude REAL, longitude REAL, x REAL, y REAL, z REAL, accuracy REAL, speed_mps REAL, altitude_m REAL, mock INTEGER)");
        db.execSQL("CREATE INDEX samples_ride_kind ON samples(ride_id,kind,id)");
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY, ride_id INTEGER NOT NULL REFERENCES rides(id) ON DELETE CASCADE, " +
                "utc_ms INTEGER NOT NULL, mono_ns INTEGER NOT NULL, message TEXT NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("A non-destructive database migration is required");
    }

    public static final class Ride {
        public long id, start, end, elapsed, gps, accel, linear, lastGps, lastMotion, checkpoint;
        public String status, note;
        public boolean recording() { return "recording".equals(status); }
    }

    private static Ride read(Cursor c) {
        Ride r = new Ride();
        r.id=c.getLong(0); r.start=c.getLong(1); r.end=c.getLong(2); r.status=c.getString(3);
        r.elapsed=c.getLong(4); r.gps=c.getLong(5); r.accel=c.getLong(6); r.linear=c.getLong(7);
        r.lastGps=c.getLong(8); r.lastMotion=c.getLong(9); r.checkpoint=c.getLong(10); r.note=c.getString(11);
        return r;
    }

    public List<Ride> rides() {
        List<Ride> result = new ArrayList<>();
        try (Cursor c=getReadableDatabase().rawQuery("SELECT * FROM rides ORDER BY id DESC",null)) {
            while(c.moveToNext()) result.add(read(c));
        }
        return result;
    }

    public Ride ride(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM rides WHERE id=?",new String[]{""+id})) {
            return c.moveToFirst()?read(c):null;
        }
    }

    public Ride active() {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM rides WHERE status='recording'",null)) {
            return c.moveToFirst()?read(c):null;
        }
    }
    public Ride latest() {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM rides ORDER BY id DESC LIMIT 1",null)) {
            return c.moveToFirst()?read(c):null;
        }
    }

    public long newRide(long utc) {
        ContentValues v=new ContentValues();
        v.put("start_utc_ms",utc); v.put("checkpoint_utc_ms",utc); v.put("status","recording");
        return getWritableDatabase().insertOrThrow("rides",null,v);
    }

    public long newSegment(long ride, long utc, long mono, int boot) {
        ContentValues v=new ContentValues();
        v.put("ride_id",ride); v.put("start_utc_ms",utc); v.put("start_mono_ns",mono);
        v.put("end_mono_ns",mono); v.put("boot_count",boot);
        return getWritableDatabase().insertOrThrow("segments",null,v);
    }

    public void event(long ride, String message) {
        ContentValues v=new ContentValues(); v.put("ride_id",ride);
        v.put("utc_ms",System.currentTimeMillis()); v.put("mono_ns",android.os.SystemClock.elapsedRealtimeNanos());
        v.put("message",message);
        getWritableDatabase().insertOrThrow("events",null,v);
    }

    public static final class Sample {
        String kind;
        long utc, mono, received;
        Double lat, lon, x, y, z, accuracy, speed, altitude;
        Integer mock;
    }

    private static void bind(SQLiteStatement s, int i, Double value) {
        if(value==null) s.bindNull(i); else s.bindDouble(i,value);
    }

    public void commit(long ride, long segment, List<Sample> batch, long elapsed, long mono) {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransactionNonExclusive();
        RuntimeException failure=null;
        try(SQLiteStatement s=db.compileStatement("INSERT INTO samples " +
                "(ride_id,segment_id,kind,utc_ms,mono_ns,received_utc_ms,latitude,longitude,x,y,z,accuracy,speed_mps,altitude_m,mock) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            long gps=0,accel=0,linear=0,lastGps=0,lastMotion=0;
            for(Sample p:batch) {
                s.clearBindings(); s.bindLong(1,ride); s.bindLong(2,segment); s.bindString(3,p.kind);
                s.bindLong(4,p.utc); s.bindLong(5,p.mono); s.bindLong(6,p.received);
                bind(s,7,p.lat); bind(s,8,p.lon); bind(s,9,p.x); bind(s,10,p.y); bind(s,11,p.z);
                bind(s,12,p.accuracy); bind(s,13,p.speed); bind(s,14,p.altitude);
                if(p.mock!=null) s.bindLong(15,p.mock);
                s.executeInsert();
                if("gps".equals(p.kind)) {gps++; lastGps=Math.max(lastGps,p.received);}
                if("accel".equals(p.kind)) {accel++; lastMotion=Math.max(lastMotion,p.received);}
                if("linear".equals(p.kind)) linear++;
            }
            db.execSQL("UPDATE rides SET elapsed_ms=?,gps_count=gps_count+?,accel_count=accel_count+?," +
                    "linear_count=linear_count+?,last_gps_utc_ms=CASE WHEN ?>0 THEN ? ELSE last_gps_utc_ms END," +
                    "last_motion_utc_ms=CASE WHEN ?>0 THEN ? ELSE last_motion_utc_ms END,checkpoint_utc_ms=? WHERE id=?",
                    new Object[]{elapsed,gps,accel,linear,lastGps,lastGps,lastMotion,lastMotion,System.currentTimeMillis(),ride});
            db.execSQL("UPDATE segments SET end_mono_ns=? WHERE id=?",new Object[]{mono,segment});
            db.setTransactionSuccessful();
        } catch(RuntimeException e) {failure=e;throw e;}
        finally {
            // SQLITE_FULL can auto-rollback. Keep its real cause if Android's rollback then fails.
            try {db.endTransaction();}
            catch(RuntimeException rollback) {if(failure!=null)failure.addSuppressed(rollback);else throw rollback;}
        }
    }

    public void finish(long id,String status,String note) {
        ContentValues v=new ContentValues(); v.put("end_utc_ms",System.currentTimeMillis());
        v.put("status",status); v.put("note",note);
        getWritableDatabase().update("rides",v,"id=?",new String[]{""+id});
    }
    public void health(long id,String message) {
        ContentValues v=new ContentValues();v.put("note",message);
        getWritableDatabase().update("rides",v,"id=? AND status='recording'",new String[]{""+id});
    }

    public boolean delete(long id) {
        return getWritableDatabase().delete("rides","id=? AND status<>'recording'",new String[]{""+id})==1;
    }
}
