package com.wang.appupdate.util;

/**
 * Created on 2016/9/8.
 * Author: wang
 */
public class PatchUtil {

    static {
        System.loadLibrary("ApkPatchLibrary");
    }
    /**
     * native方法 比较路径为oldPath的apk与newPath的apk之间差异，并生成patch包，存储于patchPath
     *
     * 返回：0，说明操作成功
     *
     * @param oldApkPath
     *            示例:/sdcard/old.apk
     * @param newApkPath
     *            示例:/sdcard/new.apk
     * @param patchPath
     *            示例:/sdcard/xx.patch
     * @return
     */
    public static native int diff(String oldApkPath, String newApkPath, String patchPath);

    /**
     * native方法 使用路径为oldApkPath的apk与路径为patchPath的补丁包，合成新的apk，并存储于newApkPath
     *
     * 返回：0，说明操作成功
     *
     * @param oldApkPath 示例:/sdcard/old.apk
     * @param newApkPath 示例:/sdcard/new.apk
     * @param patchPath  示例:/sdcard/xx.patch
     * @return
     */
    public static native int patch(String oldApkPath, String newApkPath, String patchPath);
}
