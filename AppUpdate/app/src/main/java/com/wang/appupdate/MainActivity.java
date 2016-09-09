package com.wang.appupdate;

import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


import com.wang.appupdate.util.ApkUtil;
import com.wang.appupdate.util.PatchUtil;
import com.wang.appupdate.util.SignUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    String path = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", ApkUtil.getSourceApkPath(this));
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (PatchUtil.diff(path + "/jiudeng1.apk", path + "/jiudeng2.apk", path + "/test.patch") == 0) {
                    PatchUtil.patch(path + "/jiudeng1.apk", path + "/jiudeng3.apk", path + "/test.patch");
                    boolean success = SignUtil.checkMd5(path + "/jiudeng2.apk", SignUtil.getMd5ByFile(new File(path + "/jiudeng3.apk")));
                    Log.d("MainActivity", success ? "true" : "false");
                }
            }
        }).start();

    }
}
