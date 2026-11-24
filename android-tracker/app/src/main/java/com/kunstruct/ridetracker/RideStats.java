package com.kunstruct.ridetracker;

import java.util.Locale;

final class RideStats {
    static final int VERSION=2;
    final long recordedMillis,rideMillis,breakMillis;
    final int breaks;
    final boolean hasAverage,hasTop;
    final double averageKmh,topKmh;

    RideStats(RouteData route,long elapsed) {
        recordedMillis=Math.max(0,elapsed);breakMillis=Math.min(recordedMillis,route.breaks.millis());
        rideMillis=recordedMillis-breakMillis;breaks=route.breaks.count();
        hasAverage=route.points.size()>=2&&rideMillis>0;hasTop=route.hasSpeed;
        averageKmh=hasAverage?route.distance*3600/rideMillis:0;topKmh=route.maxSpeed*3.6;
    }
    String average() {return hasAverage?String.format(Locale.US,"%.1f km/h",averageKmh):"—";}
    String top() {return hasTop?String.format(Locale.US,"%.1f km/h",topKmh):"—";}
}
