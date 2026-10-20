package br.com.trilha.foco;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

/** Entrega somente o APK previamente validado ao instalador de pacotes Android. */
public final class UpdateApkProvider extends ContentProvider {
    @Override public boolean onCreate() {return true;}
    @Override public String getType(Uri uri) {return "application/vnd.android.package-archive";}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder) {return null;}
    @Override public Uri insert(Uri uri,ContentValues values) {throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] selectionArgs) {throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs) {throw new UnsupportedOperationException();}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(getContext()==null || !"r".equals(mode) || !"1o.apk".equals(uri.getLastPathSegment())
           || !"br.com.trilha.foco.update".equals(uri.getAuthority()))
            throw new FileNotFoundException("Arquivo não autorizado");
        File apk=new File(getContext().getCacheDir(),"pending-1o.apk");
        if(!apk.isFile())throw new FileNotFoundException("Atualização indisponível");
        return ParcelFileDescriptor.open(apk,ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
