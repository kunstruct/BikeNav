package com.kunstruct.ridetracker;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Only explicit, read-only exports in this application's cache can be shared. */
public final class ExportProvider extends ContentProvider {
    static Uri uri(File file) {return new Uri.Builder().scheme("content").authority("com.kunstruct.ridetracker.exports").appendPath(file.getName()).build();}
    private File file(Uri uri) throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(uri.getPathSegments().size()!=1 || name==null || !name.matches("ride-[0-9]+-[0-9]+\\.(zip|png)")) throw new FileNotFoundException("Unknown export");
        File f=new File(getContext().getCacheDir(),name);
        if(!f.isFile()) throw new FileNotFoundException("Export is no longer available");
        return f;
    }
    @Override public boolean onCreate() {return true;}
    @Override public String getType(Uri uri) {return uri.getLastPathSegment()!=null&&uri.getLastPathSegment().endsWith(".png")?"image/png":"application/zip";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order) {
        try {
            File f=file(uri);
            String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor c=new MatrixCursor(cols);MatrixCursor.RowBuilder row=c.newRow();
            for(String column:cols) row.add(OpenableColumns.DISPLAY_NAME.equals(column)?f.getName():OpenableColumns.SIZE.equals(column)?f.length():null);
            return c;
        } catch(FileNotFoundException e) {return null;}
    }
    @Override public Uri insert(Uri uri,ContentValues values) {throw new UnsupportedOperationException("Read only");}
    @Override public int update(Uri uri,ContentValues v,String where,String[] args) {throw new UnsupportedOperationException("Read only");}
    @Override public int delete(Uri uri,String where,String[] args) {throw new UnsupportedOperationException("Read only");}
}
