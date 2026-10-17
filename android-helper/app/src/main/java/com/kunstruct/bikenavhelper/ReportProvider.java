package com.kunstruct.bikenavhelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Exposes exactly one generated report to explicitly granted share recipients. */
public final class ReportProvider extends ContentProvider {
    public static final Uri URI = Uri.parse("content://com.kunstruct.bikenavhelper.reports/debug-report.txt");
    @Override public boolean onCreate() { return true; }
    private File report(Uri uri) throws FileNotFoundException {
        if (!URI.equals(uri) || getContext() == null) throw new FileNotFoundException();
        return new File(getContext().getCacheDir(), "debug-report.txt");
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(report(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri) { return "text/plain"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        try {
            File f = report(uri); Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = "BikeNav-debug-report.txt";
                else if (OpenableColumns.SIZE.equals(columns[i])) row[i] = f.length();
            }
            cursor.addRow(row);
        } catch (FileNotFoundException ignored) { }
        return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
