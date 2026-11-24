package com.kunstruct.ridetracker;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.List;

public final class RouteView extends View {
    private final RouteData data;
    static final int BG=Color.rgb(16,23,19),GREEN=Color.rgb(168,240,105),MUTED=Color.rgb(158,174,164);
    public RouteView(Context context,RouteData data) {super(context);this.data=data;setContentDescription("Recorded route preview; separate lines indicate GPS gaps");}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);drawRoute(canvas,data,0,0,getWidth(),getHeight());
    }
    static void drawRoute(Canvas canvas,RouteData data,float left,float top,float width,float height) {
        drawRoute(canvas,data,left,top,width,height,Color.rgb(23,34,27),GREEN,MUTED);
    }
    static void drawRoute(Canvas canvas,RouteData data,float left,float top,float width,float height,int background,int ink,int label) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(background);
        canvas.drawRoundRect(left,top,left+width,top+height,24,24,p);
        List<RouteData.Point> points=data.points;
        if(points.isEmpty()) {p.setColor(label);p.setTextSize(Math.max(16,width/25));canvas.drawText("No route points to display",left+width*.08f,top+height/2,p);return;}
        double centreLat=points.get(0).lat,cos=Math.max(.01,Math.cos(Math.toRadians(centreLat)));
        double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
        for(RouteData.Point v:points) {double x=longitudeDelta(v.lon,points.get(0).lon)*cos,y=-v.lat;
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        double dx=Math.max(maxX-minX,.0001),dy=Math.max(maxY-minY,.0001);
        float pad=Math.min(width,height)*.13f;
        double scale=Math.min((width-2*pad)/dx,(height-2*pad)/dy);
        double ox=left+width/2.0-(minX+maxX)/2*scale,oy=top+height/2.0-(minY+maxY)/2*scale;
        p.setColor(ink);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(4,width*.008f));p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        Path path=new Path();
        for(RouteData.Point v:points) {
            float x=(float)(ox+longitudeDelta(v.lon,points.get(0).lon)*cos*scale),y=(float)(oy-v.lat*scale);
            if(v.connect) path.lineTo(x,y); else path.moveTo(x,y);
        }
        canvas.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        for(int i:new int[]{0,points.size()-1}) {
            RouteData.Point v=points.get(i);float x=(float)(ox+longitudeDelta(v.lon,points.get(0).lon)*cos*scale),y=(float)(oy-v.lat*scale);
            p.setColor(i==0?label:ink);canvas.drawCircle(x,y,Math.max(6,width*.012f),p);
        }
    }
    private static double longitudeDelta(double lon,double origin) {return ((lon-origin+540)%360)-180;}
    static String duration(long ms) {long sec=Math.max(0,ms/1000);return String.format(java.util.Locale.US,"%d:%02d:%02d",sec/3600,(sec/60)%60,sec%60);}
}
