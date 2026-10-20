package br.com.trilha.foco;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Atualização opcional via GitHub Releases. Nunca interfere nos recursos offline. */
final class UpdateManager {
    private static final String LATEST =
        "https://api.github.com/repos/luan-luz/AtividadeUNC3/releases/latest";
    private static final String ASSET_NAME = "1o.apk";
    private static final String DOWNLOAD_PREFIX =
        "https://github.com/luan-luz/AtividadeUNC3/releases/download/";
    private static final String PREFS = "oneo_updates";
    private static final long CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final long MAX_APK_BYTES = 80L * 1024L * 1024L;
    private static final Handler UI = new Handler(Looper.getMainLooper());

    private UpdateManager() { }

    static void check(Activity activity, boolean manual) {
        if (manual) toast(activity, "Verificando atualizações...");
        if (!manual) {
            SharedPreferences prefs=activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE);
            if (System.currentTimeMillis()-prefs.getLong("lastSuccess",0L)<CHECK_INTERVAL_MS)return;
        }
        new Thread(() -> {
            try {
                JSONObject release=readRelease();
                if (release == null) {
                    if (manual) toast(activity,"Nenhuma versão publicada no GitHub Releases.");
                    return;
                }
                String tag=release.optString("tag_name","");
                String version=tag.startsWith("v")?tag.substring(1):tag;
                if (!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
                    if (manual) toast(activity,"Versão publicada com identificação inválida.");
                    return;
                }
                PackageInfo installed=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
                String current=installed.versionName == null ? "0.0.0" : installed.versionName;
                activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE)
                    .edit().putLong("lastSuccess",System.currentTimeMillis()).apply();
                if (compare(version,current)<=0) {
                    if (manual) toast(activity,"O aplicativo já está atualizado (v"+current+").");
                    return;
                }
                JSONArray assets=release.optJSONArray("assets");
                JSONObject apk=null;
                if(assets!=null)for(int i=0;i<assets.length();i++){
                    JSONObject candidate=assets.optJSONObject(i);
                    if(candidate!=null && ASSET_NAME.equals(candidate.optString("name"))
                        && "uploaded".equals(candidate.optString("state","uploaded"))) {
                        apk=candidate;break;
                    }
                }
                if(apk==null) {
                    if(manual)toast(activity,"A versão publicada não contém 1o.apk.");
                    return;
                }
                String url=apk.optString("browser_download_url","");
                String digest=apk.optString("digest","");
                long size=apk.optLong("size",0L);
                if(!url.startsWith(DOWNLOAD_PREFIX) || !url.contains("/"+tag+"/")
                    || !url.endsWith("/"+ASSET_NAME) || size<=0 || size>MAX_APK_BYTES
                    || !digest.matches("sha256:[0-9a-fA-F]{64}")) {
                    if (manual) toast(activity,"Pacote de atualização sem validação segura.");
                    return;
                }
                final String fileUrl=url, expectedHash=digest.substring(7).toLowerCase(Locale.ROOT),newVersion=version;
                final long expectedSize=size;
                UI.post(() -> {
                    if(activity.isFinishing()||activity.isDestroyed())return;
                    new AlertDialog.Builder(activity).setTitle("Atualização 1o v"+newVersion)
                        .setMessage("Há uma nova versão disponível no GitHub. O download é opcional e não interrompe suas tarefas. O Android solicitará confirmação para instalar.")
                        .setNegativeButton("Mais tarde",null)
                        .setPositiveButton("Baixar e instalar",(d,w)->
                            download(activity,fileUrl,expectedHash,expectedSize))
                        .show();
                });
            } catch(Exception e) {
                if(manual)toast(activity,"Não foi possível verificar agora. O app continua funcionando offline.");
            }
        },"oneo-update-check").start();
    }

    private static JSONObject readRelease() throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(LATEST).openConnection();
        connection.setConnectTimeout(6000);connection.setReadTimeout(6500);
        connection.setRequestProperty("Accept","application/vnd.github+json");
        connection.setRequestProperty("User-Agent","1o-Android-Updater");
        try {
            if(connection.getResponseCode()==404)return null;
            if(connection.getResponseCode()!=200)throw new IllegalStateException("GitHub indisponível");
            try(InputStream input=connection.getInputStream()) {
                byte[] body=readLimited(input,512*1024);
                return new JSONObject(new String(body,StandardCharsets.UTF_8));
            }
        } finally {connection.disconnect();}
    }

    private static byte[] readLimited(InputStream in,int max) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1) {
            if(out.size()+n>max)throw new IllegalStateException("Resposta excessiva");
            out.write(buffer,0,n);
        }
        return out.toByteArray();
    }

    private static int compare(String a,String b) {
        int[] av=parts(a),bv=parts(b);
        for(int i=0;i<3;i++)if(av[i]!=bv[i])return Integer.compare(av[i],bv[i]);
        return 0;
    }
    private static int[] parts(String version) {
        int[] out={0,0,0};String[] data=version.split("\\.");
        for(int i=0;i<Math.min(3,data.length);i++) {
            try {out[i]=Integer.parseInt(data[i]);}catch(Exception ignored) {out[i]=0;}
        }
        return out;
    }

    private static void download(Activity activity,String url,String expectedHash,long expectedSize) {
        toast(activity,"Baixando atualização do 1o...");
        new Thread(()->{
            File pending=new File(activity.getCacheDir(),"pending-1o.apk");
            File temporary=new File(activity.getCacheDir(),"pending-1o.part");
            try {
                if(!url.startsWith(DOWNLOAD_PREFIX))throw new IllegalStateException("Origem inválida");
                HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
                connection.setConnectTimeout(10000);connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent","1o-Android-Updater");
                connection.setInstanceFollowRedirects(true);
                long copied=0;
                MessageDigest sha=MessageDigest.getInstance("SHA-256");
                try {
                    if(connection.getResponseCode()!=200)throw new IllegalStateException("Falha no download");
                    try(InputStream in=connection.getInputStream();
                        FileOutputStream out=new FileOutputStream(temporary)) {
                        byte[] buffer=new byte[16384];int n;
                        while((n=in.read(buffer))!=-1) {
                            copied+=n;
                            if(copied>MAX_APK_BYTES)throw new IllegalStateException("APK excessivo");
                            sha.update(buffer,0,n);out.write(buffer,0,n);
                        }
                        out.flush();
                    }
                } finally {connection.disconnect();}
                if(copied!=expectedSize)throw new IllegalStateException("Tamanho do APK divergente");
                StringBuilder actual=new StringBuilder();
                for(byte digit:sha.digest())actual.append(String.format(Locale.ROOT,"%02x",digit&0xff));
                if(!expectedHash.equals(actual.toString()))throw new IllegalStateException("Hash SHA-256 inválido");
                PackageManager pm=activity.getPackageManager();
                PackageInfo apk=pm.getPackageArchiveInfo(temporary.getAbsolutePath(),0);
                PackageInfo installed=pm.getPackageInfo(activity.getPackageName(),0);
                if(apk==null || !activity.getPackageName().equals(apk.packageName)
                    || versionCode(apk)<=versionCode(installed))
                    throw new IllegalStateException("Pacote ou versão inválida");
                if(pending.exists()&&!pending.delete())throw new IllegalStateException("Cache indisponível");
                if(!temporary.renameTo(pending))throw new IllegalStateException("Falha ao preparar APK");
                UI.post(()->requestInstall(activity));
            } catch(Exception e) {
                temporary.delete();
                toast(activity,"Atualização não instalada: "+e.getMessage());
            }
        },"oneo-update-download").start();
    }

    private static long versionCode(PackageInfo info) {
        if(Build.VERSION.SDK_INT>=28)return info.getLongVersionCode();
        return info.versionCode;
    }

    private static void requestInstall(Activity activity) {
        if(activity.isFinishing()||activity.isDestroyed())return;
        if(Build.VERSION.SDK_INT>=26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity).setTitle("Permitir atualização")
                .setMessage("Autorize a instalação a partir do 1o nas configurações do Android. Depois volte e toque em Verificar atualizações novamente.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Abrir configurações",(d,w)->{
                    try {
                        Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:"+activity.getPackageName()));
                        activity.startActivity(i);
                    } catch(Exception e){toast(activity,"Abra as permissões de instalação nas Configurações.");}
                }).show();
            return;
        }
        try {
            Uri uri=Uri.parse("content://br.com.trilha.foco.update/1o.apk");
            Intent install=new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri,"application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(install);
        } catch(Exception e) {toast(activity,"Não foi possível abrir o instalador Android.");}
    }

    private static void toast(Activity activity,String text) {
        UI.post(()->{
            if(!activity.isFinishing()&&!activity.isDestroyed())
                Toast.makeText(activity,text,Toast.LENGTH_LONG).show();
        });
    }
}
