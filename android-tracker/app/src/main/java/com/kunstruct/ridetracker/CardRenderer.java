package com.kunstruct.ridetracker;

import android.graphics.*;
import android.text.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Ten offline, original compositions; transparent exports can overlay your own photos. */
final class CardRenderer {
    static final String[] NAMES={"01 · Apex / sport","02 · Nightshift / neon","03 · Field Notes / journal",
            "04 · Blueprint / technical","05 · Grand Tour / editorial","06 · Contour / outdoors",
            "07 · Slipstream / race","08 · Postmark / travel","09 · Monolith / minimal","10 · Afterglow / sunset"};
    private static final float[][] MAP_FRAMES={{50,350,980,610},{70,350,940,600},{70,345,940,610},{75,220,930,680},
            {65,460,950,485},{65,380,950,580},{60,430,960,510},{65,395,950,590},{120,460,840,560},{65,390,950,570}};
    static RectF mapFrame(int template) {
        float[] f=MAP_FRAMES[template>=0&&template<MAP_FRAMES.length?template:0];return new RectF(f[0],f[1],f[0]+f[2],f[1]+f[3]);
    }
    static RectF mapFrame(int template,boolean named) {
        RectF frame=mapFrame(template);if(named)frame.top+=82;return frame;
    }
    private final Canvas c;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean transparent;
    private final RideStore.Ride ride;
    private final RouteData route;
    private final String km,date,shortDate;
    private final RideStats stats;
    private MapBackground map;
    private int mapStyle;
    private CardLabels labels=CardLabels.EMPTY;

    private CardRenderer(Bitmap bitmap,RideStore.Ride ride,RouteData route,boolean transparent) {
        c=new Canvas(bitmap);this.transparent=transparent;this.ride=ride;this.route=route;
        km=String.format(Locale.US,"%.1f",route.distance/1000);stats=new RideStats(route,ride.elapsed);
        date=new SimpleDateFormat("EEE, d MMM yyyy",Locale.ENGLISH).format(new Date(ride.start)).toUpperCase(Locale.ROOT);
        shortDate=new SimpleDateFormat("dd.MM.yy",Locale.US).format(new Date(ride.start));
    }
    static Bitmap render(RideStore.Ride ride,RouteData data,int template,boolean hideEnds,boolean transparent) {
        return render(ride,data,template,hideEnds,transparent,null,0);
    }
    static Bitmap render(RideStore.Ride ride,RouteData data,int template,boolean hideEnds,boolean transparent,MapBackground map,int mapStyle) {
        return render(ride,data,template,hideEnds,transparent,map,mapStyle,CardLabels.EMPTY);
    }
    static Bitmap render(RideStore.Ride ride,RouteData data,int template,boolean hideEnds,boolean transparent,MapBackground map,int mapStyle,CardLabels labels) {
        Bitmap bitmap=Bitmap.createBitmap(1080,1350,Bitmap.Config.ARGB_8888);
        CardRenderer r=new CardRenderer(bitmap,ride,data,transparent);
        r.map=map;r.mapStyle=mapStyle;r.labels=labels;
        RouteData preview=privateRoute(data,hideEnds);
        switch(template) {
            case 1:r.night(preview);break;case 2:r.journal(preview);break;case 3:r.blueprint(preview);break;
            case 4:r.editorial(preview);break;case 5:r.contour(preview);break;case 6:r.race(preview);break;
            case 7:r.postcard(preview);break;case 8:r.minimal(preview);break;case 9:r.sunset(preview);break;
            default:r.sport(preview);
        }
        r.label(hideEnds?"ROUTE ENDS HIDDEN · 300 M":"FULL ROUTE · START / FINISH VISIBLE",64,1305,18,
                template==2||template==4||template==7?color("#41473D"):color("#D9E2DA"));
        return bitmap;
    }
    static RouteData privateRoute(RouteData data,boolean hide) {
        if(!hide)return data;
        RouteData out=new RouteData();if(data.points.isEmpty())return out;
        RouteData.Point first=data.points.get(0),last=data.points.get(data.points.size()-1);boolean previous=false;
        for(RouteData.Point v:data.points) {
            boolean visible=RouteData.metres(first.lat,first.lon,v.lat,v.lon)>=300&&RouteData.metres(last.lat,last.lon,v.lat,v.lon)>=300;
            if(visible) {RouteData.Point p=new RouteData.Point(v.lat,v.lon,v.mono,v.segment);p.connect=v.connect&&previous;out.points.add(p);}
            previous=visible;
        }
        return out;
    }
    private static int color(String hex){return Color.parseColor(hex);}
    private void base(String hex){if(!transparent)c.drawColor(color(hex));}
    private void rect(float x,float y,float w,float h,int color){p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,p);}
    private void line(float x,float y,float x2,float y2,int color,float width){p.setColor(color);p.setStrokeWidth(width);p.setStyle(Paint.Style.STROKE);c.drawLine(x,y,x2,y2,p);p.setStyle(Paint.Style.FILL);}
    private void text(String value,float x,float y,float size,int color,String family,boolean bold) {
        p.setShader(null);p.setColor(color);p.setTextSize(size);p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.create(family,bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(value,x,y,p);
    }
    private void label(String s,float x,float y,float size,int color){text(s,x,y,size,color,"sans-serif-medium",false);}
    private void title(String s,float x,float y,float size,int color){text(s,x,y,size,color,"sans-serif-condensed",true);}
    private void route(RouteData data,int template,int ink,int bg,int label) {
        if(labels.hasNames())names(mapFrame(template));
        RectF frame=mapFrame(template,labels.hasNames());float x=frame.left,y=frame.top,w=frame.width(),h=frame.height();
        if(map!=null&&mapStyle>0&&!data.points.isEmpty())map.draw(c,data,x,y,w,h,mapStyle,ink,label);
        else RouteView.drawRoute(c,data,x,y,w,h,transparent?Color.TRANSPARENT:bg,ink,label);
    }
    private void names(RectF frame) {
        p.setColor(color("#18251F"));c.drawRoundRect(frame.left,frame.top,frame.right,frame.top+70,14,14,p);
        float left=frame.left+18,width=frame.width()-36;
        if(!labels.start.isEmpty()&&!labels.finish.isEmpty()) {
            float column=(width-64)/2;
            place("FROM",labels.start,left,frame.top,column);
            label("→",left+column+18,frame.top+49,32,Color.WHITE);
            place("TO",labels.finish,left+column+64,frame.top,column);
        } else place(labels.start.isEmpty()?"TO":"FROM",labels.start.isEmpty()?labels.finish:labels.start,left,frame.top,width);
    }
    private void place(String caption,String name,float x,float top,float width) {
        label(caption,x,top+22,16,color("#A8C0B0"));
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.WHITE);paint.setTextSize(28);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        StaticLayout layout=StaticLayout.Builder.obtain(name,0,name.length(),paint,(int)width)
                .setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).build();
        int save=c.save();c.clipRect(x,top+25,x+width,top+66);c.translate(x,top+29);layout.draw(c);c.restoreToCount(save);
    }
    private void stats(int ink,int muted,int template) {
        line(64,1040,1016,1040,muted,1);
        if(template==8) {
            metric(RouteView.duration(stats.rideMillis),"RIDE TIME",64,1100,440,ink,muted,template);
            metric(RouteView.duration(stats.breakMillis),"BREAK TIME",570,1100,440,ink,muted,template);
        } else {
            metric(km+" km","DISTANCE",64,1100,290,ink,muted,template);
            metric(RouteView.duration(stats.rideMillis),"RIDE TIME",390,1100,290,ink,muted,template);
            metric(RouteView.duration(stats.breakMillis),"BREAK TIME",716,1100,300,ink,muted,template);
        }
        metric(stats.average(),"AVERAGE · EXCLUDES BREAKS",64,1210,440,ink,muted,template);
        metric(stats.top(),"TOP SPEED · GPS",570,1210,440,ink,muted,template);
        label("STOPS ≥ 2 MIN COUNT AS BREAKS",64,1270,17,muted);
    }
    private void metric(String value,String caption,float x,float y,float width,int ink,int muted,int template) {
        String family=template==3?"monospace":template==2||template==4||template==7?"serif":"sans-serif-condensed";
        p.setTypeface(Typeface.create(family,Typeface.BOLD));p.setTextSize(48);
        String fitted=TextUtils.ellipsize(value,new TextPaint(p),width,TextUtils.TruncateAt.END).toString();
        text(fitted,x,y,48,ink,family,true);label(caption,x,y+34,20,muted);
    }
    private void sport(RouteData data) {
        int white=Color.WHITE,orange=color("#FF6B32");base("#111314");
        rect(0,0,18,1350,orange);label("RIDE TRACKER  /  MOTORCYCLE",64,96,24,orange);
        title("JUST RIDE.",58,245,132,white);label(date,66,303,26,color("#B1B7B7"));
        route(data,0,orange,Color.TRANSPARENT,white);
        stats(white,color("#AEB7B2"),0);
    }
    private void night(RouteData data) {
        int cyan=color("#71F7EC"),white=color("#F2FFFD");base("#071A22");
        label("N I G H T S H I F T",64,95,28,cyan);label(shortDate,850,95,24,white);
        title("CHASE THE",64,220,90,white);title("QUIET.",64,320,112,cyan);
        route(data,1,cyan,Color.TRANSPARENT,white);
        stats(white,cyan,1);
    }
    private void journal(RouteData data) {
        int black=color("#29372B"),rust=color("#BA542D");base("#F0EBDD");
        if(!transparent)for(int y=45;y<1280;y+=45)line(45,y,1035,y,color("#DDD8C9"),1);
        text("Field notes",65,160,88,black,"serif",false);label("A MOTORCYCLE JOURNAL",68,215,23,rust);
        label("ENTRY  "+String.format(Locale.US,"%04d",ride.id),68,300,26,black);label(shortDate,800,300,25,black);
        route(data,2,rust,Color.TRANSPARENT,black);
        stats(black,rust,2);
    }
    private void blueprint(RouteData data) {
        int blue=color("#70DBFA"),white=color("#E5F8FF");base("#0B2943");
        if(!transparent)for(int x=30;x<1080;x+=60)line(x,30,x,1270,color("#153A53"),1);
        if(!transparent)for(int y=30;y<1280;y+=60)line(30,y,1050,y,color("#153A53"),1);
        text("R O U T E / 0 1",64,110,47,white,"monospace",true);label("MOTORCYCLE FIELD RECORD",64,160,23,blue);
        route(data,3,blue,Color.TRANSPARENT,white);
        stats(white,blue,3);
    }
    private void editorial(RouteData data) {
        int black=color("#181C18"),red=color("#C3452D");base("#F7F5EB");
        label("THE MOTORCYCLE JOURNAL",64,85,23,black);label("VOL. "+ride.id,870,85,22,red);
        line(64,110,1016,110,black,2);text("Grand",60,270,152,black,"serif",false);
        text("Tour.",60,425,152,black,"serif",false);label(date,635,358,22,red);
        route(data,4,red,Color.TRANSPARENT,black);
        stats(black,red,4);
    }
    private void contour(RouteData data) {
        int lime=color("#DBE8A0"),cream=color("#F2EEDC");base("#25382C");
        if(!transparent) {p.setColor(color("#3C503B"));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
            for(int i=0;i<14;i++)c.drawOval(-420+i*24,180+i*22,1300-i*15,1450-i*17,p);p.setStyle(Paint.Style.FILL);}
        label("OUT THERE / MOTORCYCLE",64,95,24,lime);title("TAKE THE",64,230,105,cream);title("LONG WAY.",64,345,105,cream);
        route(data,5,lime,Color.TRANSPARENT,cream);
        stats(cream,lime,5);
    }
    private void race(RouteData data) {
        int red=color("#F04635"),white=color("#FAF8EC");base("#171819");
        if(!transparent) {Path block=new Path();block.moveTo(730,0);block.lineTo(1080,0);block.lineTo(1080,1350);block.lineTo(350,1350);block.close();p.setColor(red);c.drawPath(block,p);}
        label("R I D E / T R A C K E R",64,96,26,white);title("SLIP",60,250,150,white);title("STREAM",60,400,150,white);
        route(data,6,white,Color.TRANSPARENT,color("#FFE5D9"));
        stats(white,white,6);
    }
    private void postcard(RouteData data) {
        int green=color("#254D41"),orange=color("#B65130");base("#E8EFE1");
        text("Wish you",64,185,100,green,"serif",true);text("rode here.",64,290,100,green,"serif",true);
        label("A POSTCARD FROM THE ROAD",68,350,22,orange);
        route(data,7,green,transparent?Color.TRANSPARENT:color("#F8F6E9"),orange);
        stats(green,orange,7);
    }
    private void minimal(RouteData data) {
        base("#101010");int white=Color.WHITE,grey=color("#AFAFAF");
        label("R I D E",64,95,25,white);label(shortDate,850,95,24,grey);
        title(km,58,350,240,white);label("K I L O M E T R E S",70,420,29,grey);
        route(data,8,white,Color.TRANSPARENT,grey);
        stats(white,grey,8);
    }
    private void sunset(RouteData data) {
        if(!transparent) {p.setShader(new LinearGradient(0,0,850,1350,new int[]{color("#282C57"),color("#9F4773"),color("#E88A62")},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1080,1350,p);p.setShader(null);}
        int cream=color("#FFF4D9");label("A F T E R G L O W",64,100,29,cream);
        text("Stay a little",64,240,95,cream,"serif",false);text("longer.",64,350,115,cream,"serif",false);
        route(data,9,cream,Color.TRANSPARENT,Color.WHITE);
        stats(cream,cream,9);
    }
}
