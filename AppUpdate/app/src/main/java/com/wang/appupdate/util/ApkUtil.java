package com.wang.appupdate.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.text.TextUtils;

import com.wang.appupdate.BuildConfig;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

public class ApkUtil {

    /**
     * 获取已安装apk的PackageInfo
     *
     * @param context
     * @param packageName
     * @return
     */
    public static PackageInfo getInstalledApkPackageInfo(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);

        Iterator<PackageInfo> it = apps.iterator();
        while (it.hasNext()) {
            PackageInfo packageinfo = it.next();
            String thisName = packageinfo.packageName;
            if (thisName.equals(packageName)) {
                return packageinfo;
            }
        }

        return null;
    }

    /**
     * 判断apk是否已安装
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return installed;
    }

    /**
     * 获取已安装Apk文件的源Apk文件
     * 如：/data/app/com.sina.weibo-1.apk
     *
     * @param context
     * @param packageName
     * @return
     */
    public static String getSourceApkPath(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName))
            return null;

        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            return appInfo.sourceDir;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getSourceApkPath(Context context) {
        if (context != null) {
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
                return appInfo.sourceDir;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 保存apk文件到指定位置
     * @param apk apk文件
     * @param savePath 保存的位置
     * @return
     */
    public static boolean saveApk(File apk, String savePath) {
        FileInputStream in = null;
        RandomAccessFile accessFile = null;
        try {
            in = new FileInputStream(apk);
            byte[] buf = new byte[1024 * 4];
            int len;
            File file = new File(savePath);
            accessFile = new RandomAccessFile(file, "rw");
            FileDescriptor fd = accessFile.getFD();
            while ((len = in.read(buf)) != -1) {
                accessFile.write(buf, 0, len);
            }
            fd.sync();
            accessFile.close();
            in.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (in != null){
                    in.close();
                }
                if (accessFile != null){
                    accessFile.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 安装Apk
     *
     * @param context
     * @param apkPath
     */
    public static void installApk(Context context, String apkPath) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + apkPath),
                "application/vnd.android.package-archive");

        context.startActivity(intent);
    }
}