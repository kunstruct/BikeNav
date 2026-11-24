package com.kunstruct.ridetracker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.*;
import android.location.*;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class RecorderService extends Service implements SensorEventListener, LocationListener {
    public static final String START="com.kunstruct.ridetracker.START", STOP="com.kunstruct.ridetracker.STOP";
    public static final String ERROR="com.kunstruct.ridetracker.ERROR";
    private static final int NOTICE=7;
    private final List<RideStore.Sample> batch=new ArrayList<>(128);
    private HandlerThread thread;
    private Handler worker;
    private RideStore store;
    private LocationManager locations;
    private SensorManager sensors;
    private PowerManager.WakeLock wake;
    private long ride,segment,startMono,startUtc,baseElapsed,lastGpsMono,lastAccelMono,lastLinearMono;
    private long lastGpsRequest,lastSensorRequest;
    private boolean running,closing,hasLinear,gpsEnabled;
    private int commandId;
    private String lastHealth="",lastWarning="",lastNoticeHealth="";

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("recording","Ride recording",NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel("problems","Recording problems",NotificationManager.IMPORTANCE_HIGH));
        thread=new HandlerThread("ride-writer",android.os.Process.THREAD_PRIORITY_FOREGROUND);
        thread.start(); worker=new Handler(thread.getLooper());
        locations=getSystemService(LocationManager.class); sensors=getSystemService(SensorManager.class);
        wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"RideTracker:recording");
        wake.setReferenceCounted(false);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        boolean stop=intent!=null && STOP.equals(intent.getAction());
        if(!stop) {
            try {
                startForeground(NOTICE,notification("Starting recorder…",false),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } catch(RuntimeException e) {
                reportFailure("Android blocked recording: "+e.getMessage()); stopSelf(); return START_NOT_STICKY;
            }
        }
        worker.post(()->{
            if(closing) return;
            commandId=startId;
            try {
                if(store==null) store=new RideStore(this);
                if(stop) { stopRide(); return; }
                if(running) {lastNoticeHealth="";return;} // Restore health text after a duplicate Start.
                RideStore.Ride active=store.active();
                if(intent==null && active==null) { shutdown(false); return; }
                begin(active);
            } catch(Exception e) { fail(e); }
        });
        return stop?START_NOT_STICKY:START_STICKY;
    }

    private void begin(RideStore.Ride active) {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Precise location permission is required. Open Ride Tracker to grant it.");
        Sensor accel=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accel==null) throw new IllegalStateException("Accelerometer is unavailable");
        startForeground(NOTICE,notification("Starting recorder…",false),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        lastGpsMono=lastAccelMono=lastLinearMono=0;
        lastHealth="";lastWarning="";lastNoticeHealth="";batch.clear();
        startMono=SystemClock.elapsedRealtimeNanos(); startUtc=System.currentTimeMillis();
        ride=active==null?store.newRide(startUtc):active.id;
        baseElapsed=active==null?0:active.elapsed;
        segment=store.newSegment(ride,startUtc,startMono,Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1));
        store.event(ride,active==null?"Recording started":"Recorder restarted; gap since last checkpoint. New segment; no fabricated samples.");
        store.event(ride,"Device "+Build.MANUFACTURER+" "+Build.MODEL+" / Android "+Build.VERSION.RELEASE+"; accelerometer "+accel.getName()+"; requested GPS 1 Hz, motion 25 Hz");
        getSystemService(NotificationManager.class).cancel(8);
        getSystemService(NotificationManager.class).cancel(9);
        // Non-wakeup accelerometers stop when the CPU suspends; hold for the rider's entire session.
        wake.acquire();
        running=true;
        registerSensors(); registerGps();
        worker.post(tick);
    }

    private void registerSensors() {
        sensors.unregisterListener(this);
        if(!sensors.registerListener(this,sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),40_000,0,worker))
            throw new IllegalStateException("Could not subscribe to accelerometer");
        Sensor linear=sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        hasLinear=linear!=null && sensors.registerListener(this,linear,40_000,0,worker);
        lastSensorRequest=SystemClock.elapsedRealtimeNanos();
    }

    private void registerGps() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Precise location permission was revoked");
        locations.removeUpdates(this);
        gpsEnabled=locations.isProviderEnabled(LocationManager.GPS_PROVIDER);
        locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,worker.getLooper());
        lastGpsRequest=SystemClock.elapsedRealtimeNanos();
    }

    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(!running) return;
            try {
                flush();
                long now=SystemClock.elapsedRealtimeNanos();
                String health=health(now);
                if(!health.equals(lastHealth)) {
                    store.event(ride,health);store.health(ride,health);lastHealth=health;
                }
                NotificationManager nm=getSystemService(NotificationManager.class);
                if((health.startsWith("WARNING") || lastGpsMono==0) && now-startMono>15_000_000_000L) {
                    if(!health.equals(lastWarning)) {nm.notify(9,notification("Recording needs attention: "+health,true));lastWarning=health;}
                } else if(!lastWarning.isEmpty()) {nm.cancel(9);lastWarning="";}
                if(now-Math.max(lastAccelMono,lastSensorRequest)>5_000_000_000L ||
                        (hasLinear && now-Math.max(lastLinearMono,lastSensorRequest)>5_000_000_000L)) {
                    store.event(ride,"Motion stream stale; re-registering sensors"); registerSensors();
                }
                if(now-Math.max(lastGpsMono,lastGpsRequest)>30_000_000_000L) {
                    store.event(ride,"GPS stream stale; re-requesting fixes"); registerGps();
                }
                if(!health.equals(lastNoticeHealth)) {
                    nm.notify(NOTICE,notification(health,false));lastNoticeHealth=health;
                }
            } catch(Exception e) {fail(e);}
            if(running) worker.postDelayed(this,1000);
        }
    };

    private String health(long now) {
        if(lastAccelMono==0 || now-lastAccelMono>5_000_000_000L) return "WARNING: no fresh acceleration samples";
        if(!gpsEnabled) return "WARNING: GPS is switched off; motion still recording";
        if(lastGpsMono==0) return "Waiting for GPS fix — move outdoors. Motion is recording.";
        if(now-lastGpsMono>10_000_000_000L) return "WARNING: GPS gap; motion still recording";
        if(hasLinear && (lastLinearMono==0 || now-lastLinearMono>5_000_000_000L)) return "WARNING: linear acceleration stalled; GPS + raw motion continue";
        return hasLinear?"GPS + raw & linear acceleration recording":"GPS + raw acceleration recording";
    }

    private RideStore.Sample sample(String kind,long mono,long utc) {
        RideStore.Sample p=new RideStore.Sample();p.kind=kind;p.mono=mono;p.utc=utc;p.received=System.currentTimeMillis();return p;
    }

    @Override public void onSensorChanged(SensorEvent e) {
        if(!running || e.values.length<3) return;
        try {
            if(!Float.isFinite(e.values[0]) || !Float.isFinite(e.values[1]) || !Float.isFinite(e.values[2])) {
                store.event(ride,"Invalid motion sample rejected"); return;
            }
            boolean raw=e.sensor.getType()==Sensor.TYPE_ACCELEROMETER;
            RideStore.Sample p=sample(raw?"accel":"linear",e.timestamp,startUtc+(e.timestamp-startMono)/1_000_000);
            p.x=(double)e.values[0];p.y=(double)e.values[1];p.z=(double)e.values[2];p.accuracy=(double)e.accuracy;
            if(raw) lastAccelMono=e.timestamp; else lastLinearMono=e.timestamp;
            batch.add(p); if(batch.size()>=128) flush();
        } catch(Exception ex) {fail(ex);}
    }

    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) { /* Per-sample accuracy is persisted. */ }

    @Override public void onLocationChanged(Location location) {
        if(!running) return;
        try {
            RideStore.Sample p=sample("gps",location.getElapsedRealtimeNanos(),location.getTime());
            p.lat=location.getLatitude();p.lon=location.getLongitude();
            if(!Double.isFinite(p.lat) || !Double.isFinite(p.lon) || Math.abs(p.lat)>90 || Math.abs(p.lon)>180) {
                store.event(ride,"Invalid GPS coordinates rejected"); return;
            }
            p.accuracy=location.hasAccuracy()?(double)location.getAccuracy():null;
            p.speed=location.hasSpeed()?(double)location.getSpeed():null;
            p.altitude=location.hasAltitude()?location.getAltitude():null;
            p.mock=location.isMock()?1:0;
            lastGpsMono=p.mono;batch.add(p);
            if(batch.size()>=128) flush();
        } catch(Exception e) {fail(e);}
    }

    @Override public void onProviderDisabled(String provider) {gpsEnabled=false;recordEvent("GPS provider disabled; motion continues");}
    @Override public void onProviderEnabled(String provider) {gpsEnabled=true;recordEvent("GPS provider enabled; waiting for fix");}
    @Override public void onStatusChanged(String provider,int status,Bundle extras) {}

    private void recordEvent(String message) {
        if(!running) return;
        try {store.event(ride,message);} catch(Exception e) {fail(e);}
    }

    private void flush() {
        long now=SystemClock.elapsedRealtimeNanos();
        store.commit(ride,segment,batch,baseElapsed+(now-startMono)/1_000_000,now);
        batch.clear(); // Only clear after SQLite commits successfully.
    }

    private void stopRide() {
        if(running) {
            unregister(); flush();store.event(ride,"Stopped by rider");store.finish(ride,"saved","");
        } else {
            RideStore.Ride active=store.active();
            if(active!=null) store.finish(active.id,"interrupted","Recovered durable samples; recorder was not running when stopped.");
        }
        shutdown(false);
    }

    private void unregister() {
        running=false;worker.removeCallbacks(tick);sensors.unregisterListener(this);
        try {locations.removeUpdates(this);}
        catch(RuntimeException e) {Log.w("RideRecorder","Could not remove GPS listener",e);}
    }

    private void fail(Exception e) {
        Log.e("RideRecorder","Recording stopped with an error",e);
        String message="Recording stopped: "+e.getClass().getSimpleName()+" — "+String.valueOf(e.getMessage());
        unregister();
        if(store!=null && ride!=0) {
            try { if(!batch.isEmpty()) flush();store.event(ride,message);store.finish(ride,"interrupted",message); }
            catch(Exception persistenceError) {Log.e("RideRecorder","Could not persist failure; prior commits remain",persistenceError);}
        }
        reportFailure(message);shutdown(true);
    }

    private void reportFailure(String message) {
        getSystemService(NotificationManager.class).notify(8,notification(message,true));
        sendBroadcast(new Intent(ERROR).setPackage(getPackageName()).putExtra("message",message));
    }

    private void shutdown(boolean error) {
        unregister();getSystemService(NotificationManager.class).cancel(9);
        // The final sample batch and saved state are durable before the CPU may sleep.
        if(wake.isHeld())wake.release();
        // A queued new Start must not be stopped by an older asynchronous Stop.
        if(stopSelfResult(commandId)) {closing=true;stopForeground(STOP_FOREGROUND_REMOVE);}
    }

    private Notification notification(String text,boolean error) {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder n=new Notification.Builder(this,error?"problems":"recording")
                .setSmallIcon(R.drawable.ic_ride).setContentTitle(error?"Ride Tracker needs attention":"Ride Tracker · recording")
                .setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(open)
                .setOngoing(!error).setOnlyAlertOnce(!error).setCategory(Notification.CATEGORY_SERVICE);
        if(!error) {
            Intent stop=new Intent(this,RecorderService.class).setAction(STOP);
            PendingIntent action=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            n.addAction(new Notification.Action.Builder(null,"Stop & save",action).build());
        }
        return n.build();
    }

    @Override public void onDestroy() {
        worker.post(()->{
            if(running) {
                try {unregister();flush();store.event(ride,"Service destroyed; recording interrupted");}
                catch(Exception e) {Log.e("RideRecorder","Final flush failed",e);}
            }
            if(wake.isHeld()) wake.release();
            if(store!=null) store.close();thread.quitSafely();
        });
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {return null;}
}
