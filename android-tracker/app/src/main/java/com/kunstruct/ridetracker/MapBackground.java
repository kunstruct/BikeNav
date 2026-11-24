package com.kunstruct.ridetracker;

import android.content.Context;
import android.graphics.*;
import android.net.http.HttpResponseCache;
import android.os.SystemClock;
import java.io.*;
import java.net.*;

/** Optional, on-demand share-card basemap. The recorder never calls this class. */
final class MapBackground {
    static final String[] STYLES={"Line only · offline", "Street · classic", "Street · light", "Street · dark", "Street · paper"};
    static final String CREDIT="© OpenStreetMap contributors · https://www.openstreetmap.org/copyright";
    static final int TILE=256,MAX_TILES=24;
    private final Bitmap image;
    private final Plan plan;

    MapBackground(Bitmap image,Plan plan) {this.image=image;this.plan=plan;}

    static double worldX(double longitude,double origin) {
        double value=(longitude+180)/360;
        return value-Math.floor(value-origin+.5);
    }
    static double worldY(double latitude) {
        double radians=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,latitude)));
        return (1-Math.log(Math.tan(radians)+1/Math.cos(radians))/Math.PI)/2;
    }
    static final class Viewport {
        final double centreX,centreY,scale,origin;
        Viewport(RouteData route,double width,double height) {
            if(route.points.isEmpty())throw new IllegalArgumentException("No visible GPS route");
            origin=(route.points.get(0).lon+180)/360;
            double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
            for(RouteData.Point p:route.points) {
                double x=worldX(p.lon,origin),y=worldY(p.lat);
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
            centreX=(minX+maxX)/2;centreY=(minY+maxY)/2;
            double padding=Math.min(width,height)*.13;
            scale=Math.min((width-2*padding)/Math.max(.00002,maxX-minX),(height-2*padding)/Math.max(.00002,maxY-minY));
        }
        float x(RouteData.Point p,float left,float width) {return (float)(left+width/2+(worldX(p.lon,origin)-centreX)*scale);}
        float y(RouteData.Point p,float top,float height) {return (float)(top+height/2+(worldY(p.lat)-centreY)*scale);}
    }
    static final class Plan {
        final int zoom,firstX,firstY,columns,rows;
        Plan(RouteData route) {this(route,1000,600);}
        Plan(RouteData route,double width,double height) {
            Viewport fit=new Viewport(route,width,height);
            double minX=fit.centreX-width/2/fit.scale,maxX=fit.centreX+width/2/fit.scale;
            double minY=Math.max(0,fit.centreY-height/2/fit.scale),maxY=Math.min(1,fit.centreY+height/2/fit.scale);
            int z=Math.max(0,Math.min(17,(int)Math.floor(Math.log(fit.scale/TILE)/Math.log(2))));
            int x0,y0,x1,y1;
            while(true) {
                int n=1<<z;
                x0=(int)Math.floor(minX*n);x1=(int)Math.floor(maxX*n);
                y0=Math.max(0,Math.min(n-1,(int)Math.floor(minY*n)));y1=Math.max(y0,Math.min(n-1,(int)Math.floor(maxY*n)));
                if((long)(x1-x0+1)*(y1-y0+1)<=MAX_TILES||z==0)break;
                z--;
            }
            zoom=z;firstX=x0;firstY=y0;columns=x1-x0+1;rows=y1-y0+1;
            if(columns*rows>MAX_TILES)throw new IllegalArgumentException("Route is too wide for a share map");
        }
    }
    interface TileSource {Bitmap tile(int zoom,int x,int y) throws IOException;}

    static MapBackground load(Context context,RouteData route,double width,double height) throws IOException {
        ensureCache(context);
        return load(route,width,height,MapBackground::fetchTile);
    }
    static MapBackground load(RouteData route,TileSource source) throws IOException {
        return load(route,1000,600,source);
    }
    static MapBackground load(RouteData route,double width,double height,TileSource source) throws IOException {
        Plan plan=new Plan(route,width,height);long deadline=SystemClock.elapsedRealtime()+35_000;
        Bitmap mosaic=Bitmap.createBitmap(plan.columns*TILE,plan.rows*TILE,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(mosaic);
        try {
            for(int row=0;row<plan.rows;row++)for(int col=0;col<plan.columns;col++) {
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Map loading cancelled");
                if(SystemClock.elapsedRealtime()>deadline)throw new IOException("Map loading timed out. Try again when online.");
                Bitmap tile=source.tile(plan.zoom,Math.floorMod(plan.firstX+col,1<<plan.zoom),plan.firstY+row);
                try {
                    if(tile==null||tile.getWidth()!=TILE||tile.getHeight()!=TILE)throw new IOException("Unexpected map tile");
                    canvas.drawBitmap(tile,col*TILE,row*TILE,null);
                } finally {if(tile!=null)tile.recycle();}
            }
            return new MapBackground(mosaic,plan);
        } catch(IOException|RuntimeException e) {mosaic.recycle();throw e;}
    }
    private static synchronized void ensureCache(Context context) throws IOException {
        if(HttpResponseCache.getInstalled()==null)HttpResponseCache.install(new File(context.getCacheDir(),"map-http"),32L*1024*1024);
    }
    private static Bitmap fetchTile(int z,int x,int y) throws IOException {
        HttpURLConnection connection=(HttpURLConnection)new URL("https://tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png").openConnection();
        connection.setConnectTimeout(6000);connection.setReadTimeout(6000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(true);
        connection.setRequestProperty("User-Agent","RideTracker/0.4.0 (Android; com.kunstruct.ridetracker; https://github.com/kunstruct/BikeNav)");
        try {
            if(connection.getResponseCode()!=200)throw new IOException("Map server unavailable ("+connection.getResponseCode()+")");
            try(InputStream input=connection.getInputStream()) {return decodeTile(input);}
        } finally {connection.disconnect();}
    }
    static Bitmap decodeTile(InputStream input) throws IOException {
        byte[] bytes=input.readNBytes(1024*1024+1);
        if(bytes.length>1024*1024)throw new IOException("Map tile is too large");
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if(bounds.outWidth!=TILE||bounds.outHeight!=TILE||!"image/png".equals(bounds.outMimeType))throw new IOException("Invalid map tile image");
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        if(bitmap==null)throw new IOException("Map tile could not be decoded");return bitmap;
    }
    static ColorMatrixColorFilter filter(int style) {
        if(style==2)return new ColorMatrixColorFilter(new float[]{.20f,.44f,.06f,0,72, .20f,.44f,.06f,0,74, .20f,.44f,.06f,0,76, 0,0,0,1,0});
        if(style==3)return new ColorMatrixColorFilter(new float[]{-.16f,-.40f,-.05f,0,172, -.16f,-.40f,-.05f,0,184, -.16f,-.40f,-.05f,0,198, 0,0,0,1,0});
        if(style==4)return new ColorMatrixColorFilter(new float[]{.24f,.48f,.07f,0,52, .23f,.46f,.06f,0,45, .20f,.40f,.05f,0,32, 0,0,0,1,0});
        return null;
    }
    void draw(Canvas canvas,RouteData route,float left,float top,float width,float height,int style,int ink,int label) {
        Viewport view=new Viewport(route,width,height);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        int save=canvas.save();Path clip=new Path();clip.addRoundRect(new RectF(left,top,left+width,top+height),24,24,Path.Direction.CW);canvas.clipPath(clip);
        canvas.drawColor(style==3?Color.rgb(25,36,49):Color.rgb(232,234,223));
        double worldPixels=TILE*(double)(1<<plan.zoom),scale=view.scale/worldPixels;
        Matrix matrix=new Matrix();matrix.setScale((float)scale,(float)scale);
        matrix.postTranslate((float)(left+width/2+(plan.firstX/(double)(1<<plan.zoom)-view.centreX)*view.scale),
                (float)(top+height/2+(plan.firstY/(double)(1<<plan.zoom)-view.centreY)*view.scale));
        paint.setColorFilter(filter(style));canvas.drawBitmap(image,matrix,paint);paint.setColorFilter(null);
        Path line=new Path();
        for(RouteData.Point p:route.points){float x=view.x(p,left,width),y=view.y(p,top,height);if(p.connect)line.lineTo(x,y);else line.moveTo(x,y);}
        int halo=Color.luminance(ink)>.5?Color.rgb(16,23,19):Color.WHITE;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(halo);paint.setStrokeWidth(Math.max(7,width*.013f));canvas.drawPath(line,paint);
        paint.setColor(ink);paint.setStrokeWidth(Math.max(4,width*.007f));canvas.drawPath(line,paint);paint.setStyle(Paint.Style.FILL);
        for(int i:new int[]{0,route.points.size()-1}) {
            RouteData.Point p=route.points.get(i);float x=view.x(p,left,width),y=view.y(p,top,height);
            paint.setColor(halo);canvas.drawCircle(x,y,Math.max(9,width*.014f),paint);
            paint.setColor(i==0?label:ink);canvas.drawCircle(x,y,Math.max(6,width*.01f),paint);
        }
        paint.setColor(Color.rgb(250,250,250));canvas.drawRoundRect(left+8,top+height-38,left+width-8,top+height-6,6,6,paint);
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setColor(Color.rgb(25,30,28));paint.setTextSize(18);
        canvas.drawText(CREDIT,left+16,top+height-16,paint);canvas.restoreToCount(save);
    }
}
