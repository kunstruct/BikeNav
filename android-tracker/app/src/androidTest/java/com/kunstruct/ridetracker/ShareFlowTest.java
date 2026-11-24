package com.kunstruct.ridetracker;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

public final class ShareFlowTest extends InstrumentationTestCase {
    @Override protected void setUp() throws Exception {
        super.setUp();assertTrue("UI fixture tests require a dedicated emulator",android.os.Build.HARDWARE.equals("ranchu")||android.os.Build.HARDWARE.equals("goldfish"));
        android.accessibilityservice.AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();
        info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;getInstrumentation().getUiAutomation().setServiceInfo(info);
    }
    public void testPreviewAndShareProducesReadablePng() throws Exception {
        previewAndShare(false);
    }
    public void testNamedPreviewPersistenceExportAndDeletion() throws Exception {
        previewAndShare(true);
    }
    private void previewAndShare(boolean named) throws Exception {
        Context context=getInstrumentation().getTargetContext();RideStore store=new RideStore(context);
        long ride=store.newRide(System.currentTimeMillis()-180000),segment=store.newSegment(ride,System.currentTimeMillis()-180000,0,1);
        List<RideStore.Sample> points=new ArrayList<>();
        for(int i=0;i<180;i++) {
            RideStore.Sample p=new RideStore.Sample();p.kind="gps";p.utc=System.currentTimeMillis()-180000+i*1000;
            p.received=p.utc;p.mono=(i+1)*1_000_000_000L;p.lat=12+i*.00015;p.lon=77+Math.sin(i*.06)*.008;p.accuracy=5.0;p.speed=12.0;p.mock=1;
            points.add(p);
        }
        store.commit(ride,segment,points,180000,180_000_000_000L);store.finish(ride,"saved","Synthetic preview, not a real ride");
        shell("input keyevent 224");shell("wm dismiss-keyguard");shell("input keyevent 82");
        Activity activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();
        try {
            Method show=MainActivity.class.getDeclaredMethod("showRide",long.class);show.setAccessible(true);
            getInstrumentation().runOnMainSync(()->{try {show.invoke(activity,ride);} catch(Exception e){throw new RuntimeException(e);}});
            click("Create ride card");
            if(named) {
                setName("card_start_name","Bengaluru");setName("card_finish_name","Nandi Hills");
                assertEquals("Bengaluru",CardLabels.load(context,ride).start);
                click("Close");click("Create ride card");
                assertEquals("Bengaluru",node("card_start_name").getText().toString());
                assertEquals("Nandi Hills",node("card_finish_name").getText().toString());
                setName("card_finish_name","");assertEquals("",CardLabels.load(context,ride).finish);
                setName("card_finish_name","Nandi Hills");Thread.sleep(250);
                assertTrue(node("card_preview").getContentDescription().toString().contains("To Nandi Hills"));
            }
            click("Share PNG");
            getInstrumentation().waitForIdleSync();
            long wait=SystemClock.elapsedRealtime()+15000;boolean chooser=false;
            while(SystemClock.elapsedRealtime()<wait) {
                AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
                if(root!=null&&("android".contentEquals(root.getPackageName())||"com.android.intentresolver".contentEquals(root.getPackageName()))){chooser=true;break;}Thread.sleep(100);
            }
            assertTrue("Android share sheet must open after the PNG is complete",chooser);
            long end=SystemClock.elapsedRealtime()+10000;File png=null;
            while(SystemClock.elapsedRealtime()<end) {
                File[] files=context.getCacheDir().listFiles((dir,name)->name.startsWith("ride-"+ride+"-")&&name.endsWith(".png"));
                if(files!=null&&files.length>0){png=files[0];break;}Thread.sleep(100);
            }
            assertNotNull("Share action must write a PNG",png);
            try(InputStream in=context.getContentResolver().openInputStream(ExportProvider.uri(png))) {
                Bitmap bitmap=BitmapFactory.decodeStream(in);assertNotNull(bitmap);assertEquals(1080,bitmap.getWidth());assertEquals(1350,bitmap.getHeight());
                if(named) {
                    Bitmap expected=CardRenderer.render(store.ride(ride),RouteData.load(store,ride),0,true,false,null,0,new CardLabels("Bengaluru","Nandi Hills"));
                    assertTrue("Shared PNG must contain the current names",expected.sameAs(bitmap));expected.recycle();
                }
                bitmap.recycle();
            }
            try {context.getContentResolver().openFileDescriptor(ExportProvider.uri(png),"w");fail("Shared exports must be read-only");}
            catch(FileNotFoundException expected) {}
            if(named) {
                shell("input keyevent 4");click("Close");click("Delete this ride");click("Delete");
                long deletion=SystemClock.elapsedRealtime()+10000;
                while((store.ride(ride)!=null||CardLabels.load(context,ride).hasNames())&&SystemClock.elapsedRealtime()<deletion)Thread.sleep(100);
                assertNull(store.ride(ride));assertFalse(CardLabels.load(context,ride).hasNames());
            }
        } finally {getInstrumentation().runOnMainSync(()->activity.finish());store.close();}
    }
    private AccessibilityNodeInfo node(String id) throws Exception {
        long end=SystemClock.elapsedRealtime()+10000;
        while(SystemClock.elapsedRealtime()<end) {
            AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
            if(root!=null) {
                List<AccessibilityNodeInfo> matches=root.findAccessibilityNodeInfosByViewId("com.kunstruct.ridetracker:id/"+id);
                if(!matches.isEmpty())return matches.get(0);
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Missing UI control "+id);
    }
    private void setName(String id,String value) throws Exception {
        android.os.Bundle args=new android.os.Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);
        assertTrue(node(id).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args));getInstrumentation().waitForIdleSync();
    }
    private void click(String label) throws Exception {
        long end=SystemClock.elapsedRealtime()+30000,lastScroll=0;
        while(SystemClock.elapsedRealtime()<end) {
            AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
            if(root!=null)for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(label)) {
                if(node.isClickable()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return;
            }
            if(root!=null&&SystemClock.elapsedRealtime()-lastScroll>1000) {
                if(!scroll(root,AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))scroll(root,AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                lastScroll=SystemClock.elapsedRealtime();
            }
            Thread.sleep(100);
        }
        fail("No clickable UI element: "+label);
    }
    private boolean scroll(AccessibilityNodeInfo node,int action) {
        if(node.isScrollable()&&node.performAction(action))return true;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null&&scroll(child,action))return true;}
        return false;
    }
    private void shell(String command) throws IOException {
        try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))) {in.readAllBytes();}
    }
}
