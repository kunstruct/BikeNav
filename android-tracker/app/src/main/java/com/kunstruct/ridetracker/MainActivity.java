package com.kunstruct.ridetracker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final ExecutorService mapIo=Executors.newSingleThreadExecutor();
    private Future<?> mapWork;
    private RideStore store;
    private LinearLayout page,history;
    private TextView status,counts,detailStats;
    private Button start,stop;
    private boolean refreshing,destroyed,resumed;
    private long selected;
    private boolean historyDirty=true;
    private long listedActive=-1;
    private int homeGeneration;
    private String listedLatest="";
    private final SimpleDateFormat dates=new SimpleDateFormat("EEE, d MMM · HH:mm",Locale.getDefault());
    private final BroadcastReceiver errors=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) {alert(i.getStringExtra("message"));}
    };
    private final Runnable poll=new Runnable() {
        @Override public void run() {if(resumed&&selected==0){refresh();main.postDelayed(this,1000);}}
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);store=new RideStore(this);
        getWindow().setStatusBarColor(RouteView.BG);getWindow().setNavigationBarColor(RouteView.BG);
        home();
    }
    private int dp(float value) {return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout newPage() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(RouteView.BG);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(24),dp(26),dp(24),dp(40));
        scroll.addView(layout);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });return layout;
    }
    private TextView text(LinearLayout parent,String value,int size,int color) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(6),0,dp(10));
        parent.addView(t);return t;
    }
    private Button button(LinearLayout parent,String value,Runnable action) {
        Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(Color.WHITE);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(34,49,39));bg.setCornerRadius(dp(14));b.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(8),0,dp(8));parent.addView(b,lp);
        b.setOnClickListener(v->action.run());return b;
    }
    private void home() {
        selected=0;homeGeneration++;historyDirty=true;listedActive=-1;listedLatest="";page=newPage();
        text(page,"RIDE TRACKER   /   PIXEL",12,RouteView.GREEN);
        text(page,"Keep the ride.",34,Color.WHITE).setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        text(page,"GPS + motion. Saved on your phone.",16,RouteView.MUTED);
        status=text(page,"Checking recorder…",20,RouteView.GREEN);
        counts=text(page,"",15,RouteView.MUTED);
        start=button(page,"Start ride",this::startRide);start.setId(R.id.start_ride);
        ((GradientDrawable)start.getBackground()).setColor(RouteView.GREEN);start.setTextColor(RouteView.BG);
        stop=button(page,"Stop & save",()->new AlertDialog.Builder(this).setTitle("Finish this ride?")
                .setMessage("All received samples will be saved before recording stops.")
                .setPositiveButton("Stop & save",(d,w)->command(RecorderService.STOP)).setNegativeButton("Keep recording",null).show());stop.setId(R.id.stop_ride);
        PowerManager power=getSystemService(PowerManager.class);
        if(!power.isIgnoringBatteryOptimizations(getPackageName()))
            button(page,"Allow unrestricted recording",()->{
                try {startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())));}
                catch(ActivityNotFoundException e) {startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
            });
        text(page,"Before leaving: enable precise location, allow notifications, start outdoors and check that GPS + motion counts increase. Then lock your screen. Recording continues independently of this screen.",14,RouteView.MUTED);
        text(page,"Raw acceleration includes gravity; linear acceleration is also saved when available. Phone movement is not calibrated bike braking or lean.",13,RouteView.MUTED);
        text(page,"YOUR RIDES",13,RouteView.GREEN);
        history=new LinearLayout(this);history.setOrientation(LinearLayout.VERTICAL);page.addView(history);
        refresh();if(resumed){main.removeCallbacks(poll);main.post(poll);}
    }
    private void startRide() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT>=33) {
            ArrayList<String> permissions=new ArrayList<>();permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            if(Build.VERSION.SDK_INT>=33)permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            requestPermissions(permissions.toArray(new String[0]),2);return;
        }
        if(!getSystemService(LocationManager.class).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this).setTitle("Turn on location first").setMessage("The recorder needs GPS enabled to capture your route.")
                    .setPositiveButton("Location settings",(d,w)->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setNegativeButton("Cancel",null).show();return;
        }
        command(RecorderService.START);
    }
    private void command(String action) {
        try {
            Intent command=new Intent(this,RecorderService.class).setAction(action);
            if(RecorderService.STOP.equals(action))startService(command);else startForegroundService(command);
            status.setText("Saving recorder state…");
        }
        catch(RuntimeException e) {alert("Could not start recorder: "+e.getMessage());}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request!=2)return;
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)) startRide();
        else new AlertDialog.Builder(this).setTitle("Precise location + notifications required")
                .setMessage("Choose precise location and allow notifications so recording health remains visible. You can enable these in app settings.")
                .setPositiveButton("App settings",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))))
                .setNegativeButton("Later",null).show();
    }
    private void refresh() {
        if(refreshing||selected!=0||destroyed)return;refreshing=true;
        int generation=homeGeneration;boolean reload=historyDirty;long priorActive=listedActive;String priorLatest=listedLatest;
        io.execute(()->{
            try {
                RideStore.Ride active=store.active();long activeId=active==null?0:active.id;
                RideStore.Ride latest=store.latest();String latestKey=latest==null?"":latest.id+":"+latest.status+":"+latest.note;
                List<RideStore.Ride> rides=reload||priorActive!=activeId||!priorLatest.equals(latestKey)?store.rides():null;
                main.post(()->{if(!destroyed&&selected==0&&generation==homeGeneration)update(active,rides,latestKey);refreshing=false;});
            } catch(Exception e) {main.post(()->{refreshing=false;if(!destroyed&&selected==0&&generation==homeGeneration)status.setText("Storage unavailable: "+e.getMessage());});}
        });
    }
    private void update(RideStore.Ride active,List<RideStore.Ride> rides,String latestKey) {
        long now=System.currentTimeMillis();
        if(active==null) {status.setText("Ready when you are.");status.setTextColor(RouteView.GREEN);counts.setText("No ride is recording.");start.setEnabled(true);start.setText("Start ride");stop.setEnabled(false);}
        else {
            boolean stale=now-active.checkpoint>6000;
            boolean motion=active.lastMotion!=0&&now-active.lastMotion<5000,gps=active.lastGps!=0&&now-active.lastGps<10000;
            boolean warning=active.note.startsWith("WARNING");
            status.setText(stale?"Recorder interrupted or stalled — resume below":warning?active.note:!motion?"Waiting for motion samples…":!gps?"Recording motion · waiting for GPS":"Recording · GPS + motion");
            status.setTextColor(stale||warning||!motion||!gps?Color.rgb(255,196,110):RouteView.GREEN);
            counts.setText(RouteView.duration(active.elapsed)+" recorded\n"+active.gps+" GPS  /  "+active.accel+" raw  /  "+active.linear+" linear\n"+
                    "GPS: "+age(active.lastGps,now)+"  ·  motion: "+age(active.lastMotion,now)+"\nLast durable write: "+age(active.checkpoint,now));
            start.setEnabled(stale);start.setText(stale?"Resume recorder":"Recording in background");stop.setEnabled(true);
        }
        start.setAlpha(start.isEnabled()?1f:.4f);stop.setAlpha(stop.isEnabled()?1f:.35f);
        if(rides!=null) {
            listedActive=active==null?0:active.id;listedLatest=latestKey;historyDirty=false;history.removeAllViews();
            if(rides.isEmpty())text(history,"Your first ride starts above.",16,RouteView.MUTED);
            for(RideStore.Ride r:rides) {
                button(history,dates.format(new Date(r.start))+"  ·  "+r.status,()->showRide(r.id));
                if(!r.recording()&&!r.note.isEmpty())text(history,r.note,13,Color.rgb(255,196,110));
            }
        }
    }
    private static String age(long time,long now) {return time==0?"no samples yet":Math.max(0,(now-time)/1000)+"s ago";}
    private void showRide(long id) {
        selected=id;page=newPage();button(page,"← All rides",this::home);
        text(page,"YOUR RIDE",12,RouteView.GREEN);detailStats=text(page,"Loading saved samples…",20,Color.WHITE);
        io.execute(()->{
            try {
                RideStore.Ride ride=store.ride(id);RouteData data=RouteData.load(store,id);
                main.post(()->{if(!destroyed&&selected==id)detail(ride,data);});
            } catch(Exception e) {main.post(()->alert("Could not load ride: "+e.getMessage()));}
        });
    }
    private void detail(RideStore.Ride ride,RouteData data) {
        if(ride==null) {home();return;}
        RideStats stats=new RideStats(data,ride.elapsed);
        detailStats.setText(String.format(Locale.US,"%.2f km\n",data.distance/1000)+
                "Ride time  "+RouteView.duration(stats.rideMillis)+"\nBreak time  "+RouteView.duration(stats.breakMillis)+
                "  ·  "+stats.breaks+" break"+(stats.breaks==1?"":"s")+"\nAverage speed  "+stats.average()+"\nTop speed  "+stats.top());
        text(page,"Recorded total: "+RouteView.duration(stats.recordedMillis)+". Breaks are estimated from stops of at least 2 minutes. Short stops and GPS gaps stay in ride time; top speed filters isolated GPS spikes.",13,RouteView.MUTED);
        text(page,dates.format(new Date(ride.start))+" · "+ride.status,14,RouteView.MUTED);
        page.addView(new RouteView(this,data),new LinearLayout.LayoutParams(-1,dp(320)));
        text(page,ride.gps+" GPS · "+ride.accel+" raw · "+ride.linear+" linear samples\n"+data.gaps+" route breaks · "+data.rejected+" fixes excluded from preview; raw data retained",14,RouteView.MUTED);
        if(!ride.note.isEmpty())text(page,ride.note,15,Color.rgb(255,196,110));
        if(ride.recording())text(page,"This ride is still recording. This preview is a snapshot; return to All rides for live health.",14,RouteView.GREEN);
        button(page,"Create ride card · 10 designs",()->cards(ride,data));
        button(page,"Export raw data (.zip)",()->new AlertDialog.Builder(this).setTitle("Export location + motion data?")
                .setMessage("This ZIP contains exact coordinates, timestamps, raw/linear motion, segments and recording events. Share only with people you choose.")
                .setPositiveButton("Export",(d,w)->export(ride.id)).setNegativeButton("Cancel",null).show());
        if(!ride.recording())button(page,"Delete this ride",()->new AlertDialog.Builder(this).setTitle("Delete this ride permanently?")
                .setMessage("The route, raw samples and this app’s cached exports will be removed.")
                .setPositiveButton("Delete",(d,w)->io.execute(()->{
                    try {if(!store.delete(ride.id))throw new IOException("Ride is recording or already deleted");CardLabels.clear(this,ride.id);RideExport.clean(getCacheDir(),ride.id);main.post(()->{if(!destroyed)home();});}
                    catch(Exception e){main.post(()->alert("Could not delete: "+e.getMessage()));}
                })).setNegativeButton("Keep",null).show());
    }
    private void cards(RideStore.Ride ride,RouteData data) {
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(12),dp(8),dp(12),dp(8));
        Spinner templates=new Spinner(this);templates.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,CardRenderer.NAMES));layout.addView(templates);
        Spinner maps=new Spinner(this);maps.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,MapBackground.STYLES));layout.addView(maps);
        CardLabels[] labels={CardLabels.load(this,ride.id)};
        EditText startName=placeInput(layout,"Start name (optional)",labels[0].start,R.id.card_start_name);
        EditText finishName=placeInput(layout,"Finish name (optional)",labels[0].finish,R.id.card_finish_name);
        text(layout,"Leave names blank to omit them. Names are shared as written, even when route ends are hidden.",12,RouteView.MUTED);
        CheckBox hide=new CheckBox(this);hide.setText("Hide route ends (300 m)");hide.setChecked(true);layout.addView(hide);
        CheckBox transparent=new CheckBox(this);transparent.setText("Transparent outside route / map");layout.addView(transparent);
        TextView notice=text(layout,"Line preview works offline. Online maps reveal the viewed area to OpenStreetMap; recordings stay on your phone.",12,RouteView.MUTED);
        Button retry=button(layout,"Retry map",()->{});retry.setVisibility(View.GONE);
        ImageView image=new ImageView(this);image.setId(R.id.card_preview);image.setBackgroundColor(Color.rgb(110,110,110));image.setAdjustViewBounds(true);image.setContentDescription("Preview of the selected ride card");
        layout.addView(image,new LinearLayout.LayoutParams(-1,dp(300)));
        TextView credits=text(layout,"Maps: © OpenStreetMap contributors · view license",12,RouteView.MUTED);
        credits.setOnClickListener(v->{try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/copyright")));}catch(ActivityNotFoundException e){alert("https://www.openstreetmap.org/copyright");}});
        MapBackground[] background={null};boolean[] mapHidden={true},mapNamed={false},opened={true},loading={false};int[] generation={0},mapTemplate={0};Button[] shareButton={null};
        Runnable draw=()->{
            if(destroyed||!opened[0])return;
            // RenderThread can retain the previous frame; let GC release displayed bitmaps safely.
            MapBackground map=mapHidden[0]==hide.isChecked()&&mapNamed[0]==labels[0].hasNames()&&mapTemplate[0]==templates.getSelectedItemPosition()?background[0]:null;
            image.setImageBitmap(CardRenderer.render(ride,data,templates.getSelectedItemPosition(),hide.isChecked(),transparent.isChecked(),map,maps.getSelectedItemPosition(),labels[0]));
            image.setContentDescription("Ride card preview"+(labels[0].start.isEmpty()?"":" · From "+labels[0].start)+(labels[0].finish.isEmpty()?"":" · To "+labels[0].finish));
            if(shareButton[0]!=null) {
                shareButton[0].setEnabled(!loading[0]);
                shareButton[0].setText(loading[0]?"Loading map…":maps.getSelectedItemPosition()>0&&map==null?"Share line PNG":"Share PNG");
            }
        };
        Runnable loadMap=()->{
            if(destroyed||!opened[0])return;
            int token=++generation[0];if(mapWork!=null)mapWork.cancel(true);loading[0]=false;retry.setVisibility(View.GONE);
            if(maps.getSelectedItemPosition()==0) {notice.setText("Line preview works offline. Online maps reveal the viewed area to OpenStreetMap; recordings stay on your phone.");draw.run();return;}
            boolean hidden=hide.isChecked(),named=labels[0].hasNames();
            int template=templates.getSelectedItemPosition();
            if(background[0]!=null&&mapHidden[0]==hidden&&mapNamed[0]==named&&mapTemplate[0]==template){notice.setText("Map ready · map colors change without downloading again.");draw.run();return;}
            background[0]=null;RouteData visible=CardRenderer.privateRoute(data,hidden);
            if(visible.points.isEmpty()){notice.setText(hidden?"No visible route after hiding endpoints. Disable hiding to show this short ride, or use line only.":"No usable GPS route for a map.");draw.run();return;}
            loading[0]=true;notice.setText("Loading street map… Recording continues independently.");draw.run();
            mapWork=mapIo.submit(()->{
                try {
                    RectF frame=CardRenderer.mapFrame(template,named);
                    MapBackground map=MapBackground.load(this,visible,frame.width(),frame.height());
                    main.post(()->{if(destroyed||!opened[0]||generation[0]!=token)return;background[0]=map;mapHidden[0]=hidden;mapNamed[0]=named;mapTemplate[0]=template;loading[0]=false;notice.setText("Map ready · © OpenStreetMap contributors");draw.run();});
                } catch(IOException|RuntimeException e) {
                    main.post(()->{if(destroyed||!opened[0]||generation[0]!=token)return;loading[0]=false;notice.setText("Map unavailable. The line preview can still be shared. "+e.getMessage());retry.setVisibility(View.VISIBLE);draw.run();});
                }
            });
        };
        retry.setOnClickListener(v->loadMap.run());
        TextWatcher namesChanged=new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable s){
                boolean hadNames=labels[0].hasNames();labels[0]=new CardLabels(startName.getText().toString(),finishName.getText().toString());
                labels[0].save(MainActivity.this,ride.id);main.removeCallbacks(draw);
                if(hadNames!=labels[0].hasNames())loadMap.run();else main.postDelayed(draw,120);
            }
        };
        startName.addTextChangedListener(namesChanged);finishName.addTextChangedListener(namesChanged);
        templates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){loadMap.run();}
            @Override public void onNothingSelected(AdapterView<?> p){}
        });
        maps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){loadMap.run();}
            @Override public void onNothingSelected(AdapterView<?> p){}
        });
        hide.setOnCheckedChangeListener((b,c)->loadMap.run());transparent.setOnCheckedChangeListener((b,c)->draw.run());
        ScrollView scroll=new ScrollView(this);scroll.addView(layout);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Your ride, your style").setView(scroll)
                .setPositiveButton("Share PNG",null).setNegativeButton("Close",null).create();
        dialog.setOnShowListener(d->{shareButton[0]=dialog.getButton(AlertDialog.BUTTON_POSITIVE);draw.run();shareButton[0].setOnClickListener(v->{
            int style=templates.getSelectedItemPosition(),mapStyle=maps.getSelectedItemPosition();boolean hidden=hide.isChecked(),clear=transparent.isChecked();
            MapBackground map=mapHidden[0]==hidden&&mapNamed[0]==labels[0].hasNames()&&mapTemplate[0]==style?background[0]:null;
            CardLabels names=labels[0];
            io.execute(()->{
                try {
                    Bitmap bmp=CardRenderer.render(ride,data,style,hidden,clear,map,mapStyle,names);
                    File file=new File(getCacheDir(),"ride-"+ride.id+"-"+System.currentTimeMillis()+".png");
                    try(FileOutputStream out=new FileOutputStream(file)){if(!bmp.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG export failed");}
                    finally {bmp.recycle();}
                    main.post(()->share(file));
                } catch(Exception e){main.post(()->alert("Could not create image: "+e.getMessage()));}
            });
        });});
        dialog.setOnDismissListener(d->{opened[0]=false;generation[0]++;main.removeCallbacks(draw);if(mapWork!=null)mapWork.cancel(true);image.setImageDrawable(null);});dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private EditText placeInput(LinearLayout layout,String caption,String value,int id) {
        TextView label=text(layout,caption,13,RouteView.MUTED);label.setLabelFor(id);
        EditText input=new EditText(this);input.setId(id);input.setSingleLine(true);input.setHint("Leave blank");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(CardLabels.MAX_LENGTH)});input.setText(value);
        layout.addView(input,new LinearLayout.LayoutParams(-1,dp(48)));return input;
    }
    private void export(long id) {
        Toast.makeText(this,"Preparing a consistent data snapshot…",Toast.LENGTH_LONG).show();
        io.execute(()->{try {File f=RideExport.zip(getCacheDir(),store,id);main.post(()->share(f));}
            catch(Exception e){main.post(()->alert("Export failed; recorded data is intact: "+e.getMessage()));}});
    }
    private void share(File file) {
        if(destroyed)return;Uri uri=ExportProvider.uri(file);
        Intent send=new Intent(Intent.ACTION_SEND).setType(file.getName().endsWith(".png")?"image/png":"application/zip")
                .putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri("Ride export",uri));
        try {startActivity(Intent.createChooser(send,"Share your ride"));}catch(ActivityNotFoundException e){alert("No share app is installed.");}
    }
    private void alert(String message) {if(!destroyed)new AlertDialog.Builder(this).setTitle("Ride Tracker").setMessage(message).setPositiveButton("OK",null).show();}
    @Override protected void onResume() {
        super.onResume();resumed=true;
        registerReceiver(errors,new IntentFilter(RecorderService.ERROR),Context.RECEIVER_NOT_EXPORTED);
        main.post(poll);
    }
    @Override protected void onPause() {resumed=false;main.removeCallbacks(poll);unregisterReceiver(errors);super.onPause();}
    @Override public void onBackPressed() {if(selected!=0)home();else super.onBackPressed();}
    @Override protected void onDestroy() {destroyed=true;if(mapWork!=null)mapWork.cancel(true);mapIo.shutdownNow();main.removeCallbacksAndMessages(null);io.execute(()->store.close());io.shutdown();super.onDestroy();}
}
