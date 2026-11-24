package com.kunstruct.ridetracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class StorageTest extends AndroidTestCase {
    private RideStore store;
    private String database;
    @Override protected void setUp() throws Exception {
        super.setUp();
        assertTrue("Fault tests require a dedicated emulator",android.os.Build.HARDWARE.equals("ranchu")||android.os.Build.HARDWARE.equals("goldfish"));
        database="test-"+UUID.randomUUID()+".db";store=new RideStore(getContext(),database);
    }
    @Override protected void tearDown() throws Exception {if(store!=null)store.close();if(database!=null)getContext().deleteDatabase(database);super.tearDown();}
    private RideStore.Sample motion(long mono) {
        RideStore.Sample p=new RideStore.Sample();p.kind="accel";p.utc=1000+mono/1_000_000;p.mono=mono;p.received=p.utc;
        p.x=-2.5;p.y=1.25;p.z=9.8;p.accuracy=3.0;return p;
    }
    public void testCommittedSamplesSurviveReopen() {
        long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1);
        List<RideStore.Sample> samples=new ArrayList<>();for(int i=0;i<1000;i++)samples.add(motion(i*40_000_000L));
        store.commit(ride,segment,samples,40000,40_000_000_000L);store.close();store=new RideStore(getContext(),database);
        assertEquals(1000,store.ride(ride).accel);assertEquals(40000,store.ride(ride).elapsed);
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT x,y,z FROM samples ORDER BY id DESC LIMIT 1",null)) {
            assertTrue(c.moveToFirst());assertEquals(-2.5,c.getDouble(0));assertEquals(1.25,c.getDouble(1));assertEquals(9.8,c.getDouble(2));
        }
    }
    public void testFailedBatchRollsBackSamplesAndCounters() {
        long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1);
        RideStore.Sample bad=motion(2);bad.kind=null;
        try {store.commit(ride,segment,Arrays.asList(motion(1),bad),1000,10);fail("Expected invalid batch to fail");}
        catch(IllegalArgumentException|SQLiteConstraintException expected) {}
        assertEquals(0,store.ride(ride).accel);
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT count(*) FROM samples",null)){c.moveToFirst();assertEquals(0,c.getInt(0));}
    }
    public void testOnlyOneActiveRideAndDeletionCascades() {
        long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1);
        try {store.newRide(2000);fail("Second active ride must be refused");} catch(SQLiteConstraintException expected) {}
        assertFalse(store.delete(ride));store.commit(ride,segment,Arrays.asList(motion(1)),1000,10);store.event(ride,"test");
        store.finish(ride,"saved","");assertTrue(store.delete(ride));
        for(String table:new String[]{"rides","segments","samples","events"})
            try(Cursor c=store.getReadableDatabase().rawQuery("SELECT count(*) FROM "+table,null)){c.moveToFirst();assertEquals(0,c.getInt(0));}
    }
    public void testWalUsesFullSynchronization() {
        try(Cursor c=store.getWritableDatabase().rawQuery("PRAGMA synchronous",null)){c.moveToFirst();assertEquals(2,c.getInt(0));}
        try(Cursor c=store.getWritableDatabase().rawQuery("PRAGMA journal_mode",null)){c.moveToFirst();assertEquals("wal",c.getString(0));}
    }
    public void testFullDatabasePreservesPreviousCommit() {
        long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1);
        store.commit(ride,segment,Arrays.asList(motion(1)),1000,1);
        long pages;
        try(Cursor c=store.getWritableDatabase().rawQuery("PRAGMA page_count",null)){c.moveToFirst();pages=c.getLong(0);}
        // Pin the primary connection; a WAL reader's per-connection limit cannot constrain writes.
        store.getWritableDatabase().beginTransactionNonExclusive();
        try {
            try(Cursor c=store.getWritableDatabase().rawQuery("PRAGMA max_page_count="+pages,null)){c.moveToFirst();assertEquals(pages,c.getLong(0));}
            store.getWritableDatabase().setTransactionSuccessful();
        } finally {store.getWritableDatabase().endTransaction();}
        List<RideStore.Sample> samples=new ArrayList<>();for(int i=0;i<2000;i++)samples.add(motion(i+2));
        try {store.commit(ride,segment,samples,2000,2001);fail("A full database must report failure");}
        catch(android.database.sqlite.SQLiteFullException expected) {}
        assertEquals(1,store.ride(ride).accel);
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT count(*) FROM samples",null)){c.moveToFirst();assertEquals(1,c.getInt(0));}
    }
    public void testFourHoursOfSamplesRemainConsistent() {
        long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1),mono=0;
        List<RideStore.Sample> batch=new ArrayList<>();
        // Four hours at 25 Hz: exercise database growth and 14,400 separate durable commits.
        for(int second=0;second<14400;second++) {
            batch.clear();
            for(int i=0;i<25;i++){mono+=40_000_000;batch.add(motion(mono));}
            store.commit(ride,segment,batch,(second+1)*1000L,mono);
        }
        store.close();store=new RideStore(getContext(),database);
        assertEquals(360000,store.ride(ride).accel);assertEquals(14400000,store.ride(ride).elapsed);
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT COUNT(*),COUNT(DISTINCT mono_ns) FROM samples",null)) {
            c.moveToFirst();assertEquals(360000,c.getLong(0));assertEquals(360000,c.getLong(1));
        }
        try(Cursor c=store.getReadableDatabase().rawQuery("PRAGMA quick_check",null)){c.moveToFirst();assertEquals("ok",c.getString(0));}
    }
    public void testRawZipContainsExactSamplesAndEvents() throws Exception {
        long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1);
        store.commit(ride,segment,Arrays.asList(motion(40_000_000)),1000,1_000_000_000);store.event(ride,"restart, \"gap\"");
        File file=RideExport.zip(getContext().getCacheDir(),store,ride);
        try(ZipFile zip=new ZipFile(file)) {
            assertEquals(7,zip.size());String csv=new String(zip.getInputStream(zip.getEntry("motion.csv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(csv.contains("\"-2.5\""));assertTrue(csv.contains("\"40000000\""));
            assertNotNull(zip.getEntry("segments.csv"));assertNotNull(zip.getEntry("events.csv"));
        } finally {file.delete();}
    }
    public void testReadOnlyExportSnapshotAllowsConcurrentRecording() throws Exception {
        long ride=store.newRide(1000),segment=store.newSegment(ride,1000,0,1);
        store.commit(ride,segment,Arrays.asList(motion(1)),1000,1);
        try(android.database.sqlite.SQLiteDatabase snapshot=android.database.sqlite.SQLiteDatabase.openDatabase(
                store.getReadableDatabase().getPath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY)) {
            snapshot.beginTransactionReadOnly();
            java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
            Thread writer=new Thread(()->{try(RideStore other=new RideStore(getContext(),database)) {
                other.commit(ride,segment,Arrays.asList(motion(2),motion(3)),2000,3);
            } catch(Throwable t){failure.set(t);}});
            try {
                try(Cursor c=snapshot.rawQuery("SELECT COUNT(*) FROM samples",null)){c.moveToFirst();assertEquals(1,c.getLong(0));}
                writer.start();writer.join(2000);assertFalse("Export snapshot must not block recorder writes",writer.isAlive());
                if(failure.get()!=null)throw new AssertionError(failure.get());
                try(Cursor c=snapshot.rawQuery("SELECT COUNT(*) FROM samples",null)){c.moveToFirst();assertEquals(1,c.getLong(0));}
            } finally {snapshot.endTransaction();writer.join(2000);}
        }
        assertEquals(3,store.ride(ride).accel);
    }
    public void testRouteRejectsJumpsPoorAccuracyAndBridgesNoGaps() {
        RouteData data=new RouteData();data.add(12,77,1_000_000_000L,1,5.0,10.0);
        data.add(12.0001,77,2_000_000_000L,1,5.0,10.0);assertTrue(data.distance>10&&data.distance<12);
        data.add(40,40,3_000_000_000L,1,5.0,20.0);assertEquals(1,data.rejected);assertEquals(2,data.points.size());
        data.add(12.0002,77,4_000_000_000L,1,5.0,10.0);assertFalse(data.points.get(2).connect);
        data.add(12.0003,77,20_000_000_000L,1,5.0,10.0);assertFalse(data.points.get(3).connect);
        data.add(12.0004,77,21_000_000_000L,2,5.0,null);assertFalse(data.points.get(4).connect);
        data.add(12.0005,77,22_000_000_000L,2,100.0,null);assertEquals(2,data.rejected);
        assertEquals(10.0,data.maxSpeed);
    }
    public void testStationaryDriftAndOutOfOrderFixes() {
        RouteData data=new RouteData();for(int i=0;i<100;i++)data.add(12+(i%2)*.00001,77,(i+1)*1_000_000_000L,1,5.0,.1);
        assertEquals(0.0,data.distance);data.add(12,77,1,1,5.0,null);assertEquals(1,data.rejected);
    }
    public void testPrivacyAndTenDistinctTransparentDesigns() throws Exception {
        RouteData data=new RouteData();for(int i=0;i<180;i++)data.add(12+i*.00015,77+Math.sin(i*.06)*.008,(i+1)*1_000_000_000L,1,5.0,12.0);
        RouteData hidden=CardRenderer.privateRoute(data,true);assertTrue(hidden.points.size()<data.points.size());assertFalse(hidden.points.isEmpty());
        RouteData.Point first=data.points.get(0),last=data.points.get(data.points.size()-1);
        for(RouteData.Point point:hidden.points){assertTrue(RouteData.metres(first.lat,first.lon,point.lat,point.lon)>=300);assertTrue(RouteData.metres(last.lat,last.lon,point.lat,point.lon)>=300);}
        RideStore.Ride ride=new RideStore.Ride();ride.id=42;ride.start=1795490000000L;ride.elapsed=3600000;
        Set<Integer> fingerprints=new HashSet<>();
        File previews=new File(getContext().getFilesDir(),"template-previews");assertTrue(previews.isDirectory()||previews.mkdirs());
        for(int i=0;i<CardRenderer.NAMES.length;i++) {
            Bitmap bitmap=CardRenderer.render(ride,data,i,true,false);assertEquals(1080,bitmap.getWidth());assertEquals(1350,bitmap.getHeight());
            int[] pixels=new int[1080*1350];bitmap.getPixels(pixels,0,1080,0,0,1080,1350);assertTrue(fingerprints.add(Arrays.hashCode(pixels)));
            try(FileOutputStream out=new FileOutputStream(new File(previews,"template-"+i+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
            Bitmap transparent=CardRenderer.render(ride,data,i,true,true);assertEquals(0,android.graphics.Color.alpha(transparent.getPixel(1079,600)));transparent.recycle();
        }
        assertEquals(10,fingerprints.size());
    }
    public void testEmptyRideCardAndUnknownSpeed() {
        RouteData data=new RouteData();data.add(12,77,1,1,null,null);assertTrue(data.points.isEmpty());assertFalse(data.hasSpeed);
        RideStore.Ride ride=new RideStore.Ride();ride.start=1000;Bitmap card=CardRenderer.render(ride,data,0,true,false);assertNotNull(card);card.recycle();
    }
}
