package com.kunstruct.ridetracker;

import android.content.Context;
import android.content.SharedPreferences;

/** Manual share-card metadata, kept apart from the recorder's database. */
final class CardLabels {
    static final int MAX_LENGTH=80;
    static final CardLabels EMPTY=new CardLabels("","");
    final String start,finish;

    CardLabels(String start,String finish) {this.start=clean(start);this.finish=clean(finish);}
    boolean hasNames() {return !start.isEmpty()||!finish.isEmpty();}
    private static String clean(String value) {
        String text=value==null?"":value.replaceAll("[\\p{Z}\\p{Cc}\\s]+"," ").trim();
        int count=text.codePointCount(0,text.length());
        return count>MAX_LENGTH?text.substring(0,text.offsetByCodePoints(0,MAX_LENGTH)):text;
    }
    private static SharedPreferences preferences(Context context) {return context.getSharedPreferences("ride-card-labels",Context.MODE_PRIVATE);}
    static CardLabels load(Context context,long ride) {
        SharedPreferences p=preferences(context);return new CardLabels(p.getString(ride+".start",""),p.getString(ride+".finish",""));
    }
    void save(Context context,long ride) {
        preferences(context).edit().putString(ride+".start",start).putString(ride+".finish",finish).apply();
    }
    static void clear(Context context,long ride) {preferences(context).edit().remove(ride+".start").remove(ride+".finish").apply();}
}
