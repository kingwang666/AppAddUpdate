package com.wang.appaddupdatedemo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wang.appupdate.util.PatchUtil;
import com.wang.appupdate.util.SignUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();


    private String mOldApk = PATH + "/jiudeng1.apk";
    private String mNewApk = PATH + "/jiudeng2.apk";
    private String mPatchPath = PATH + "/test.patch";
    private String mNewApk2 = PATH + "/jiudeng3.apk";

    @BindView(R.id.old_apk_tv)
    TextView mOldApkTV;
    @BindView(R.id.old_apk_size_tv)
    TextView mOldApkSizeTV;
    @BindView(R.id.old_apk_md5_tv)
    TextView mOldApkMd5TV;
    @BindView(R.id.new_apk_tv)
    TextView mNewApkTV;
    @BindView(R.id.new_apk_size_tv)
    TextView mNewApkSizeTV;
    @BindView(R.id.new_apk_md5_tv)
    TextView mNewApkMd5TV;
    @BindView(R.id.patch_tv)
    TextView mPatchTV;
    @BindView(R.id.patch_size_tv)
    TextView mPatchSizeTV;
    @BindView(R.id.new_apk_2_tv)
    TextView mNewApk2TV;
    @BindView(R.id.new_apk_2_size_tv)
    TextView mNewApk2SizeTV;
    @BindView(R.id.new_apk_2_md5_tv)
    TextView mNewApk2Md5TV;
    @BindView(R.id.msg_tv)
    TextView mMsgTV;
    @BindView(R.id.get_patch_btn)
    Button mGetPatchBtn;
    @BindView(R.id.get_new_apk_btn)
    Button mGetNewApkBtn;
    @BindView(R.id.delete_btn)
    Button mDeleteBtn;
    @BindView(R.id.loading)
    ProgressBar mLoading;

    private Subscription mSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.get_patch_btn, R.id.get_new_apk_btn, R.id.delete_btn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.get_patch_btn:
                clearText();
                File file = new File(mOldApk);
                File newFile = new File(mNewApk);
                if (!file.exists()) {
                    mMsgTV.setText("旧APK不存在");
                    return;
                }
                if (!newFile.exists()) {
                    mMsgTV.setText("新APK不存在");
                    return;
                }
                File p = new File(mPatchPath);
                if (p.exists()) {
                    p.delete();
                }
                setEnabled(false);
                mOldApkTV.setText(mOldApk);
                mOldApkSizeTV.setText(String.format("%.2f M", getFileSize(file)));
                mOldApkMd5TV.setText(SignUtils.getMd5ByFile(file));

                mNewApkTV.setText(mNewApk);
                mNewApkSizeTV.setText(String.format("%.2f M", getFileSize(newFile)));
                mNewApkMd5TV.setText(SignUtils.getMd5ByFile(newFile));
                mSubscription = Observable.just("")
                        .map(new Func1<String, Integer>() {
                            @Override
                            public Integer call(String s) {
                                return PatchUtil.diff(mOldApk, mNewApk, mPatchPath);
                            }
                        })
                        .map(new Func1<Integer, String>() {
                            @Override
                            public String call(Integer integer) {
                                return checkResult(integer);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                setEnabled(true);
                                mMsgTV.setText(e.toString());
                            }

                            @Override
                            public void onNext(String s) {
                                mMsgTV.setText(s);
                                setEnabled(true);
                                if (s.equals("success")) {
                                    mPatchTV.setText(mPatchPath);
                                    mPatchSizeTV.setText(String.format("%.2f M", getFileSize(new File(mPatchPath))));
                                }
                            }
                        });
                break;
            case R.id.get_new_apk_btn:
                File oldApk = new File(mOldApk);
                File patch = new File(mPatchPath);
                final File newApk2 = new File(mNewApk2);
                if (!oldApk.exists()) {
                    mMsgTV.setText("旧APK不存在");
                    return;
                }
                if (!patch.exists()) {
                    mMsgTV.setText("补丁文件不存在");
                    return;
                }
                if (newApk2.exists()) {
                    newApk2.delete();
                }
                setEnabled(false);
                mSubscription = Observable.just("")
                        .map(new Func1<String, Integer>() {
                            @Override
                            public Integer call(String s) {
                                return PatchUtil.patch(mOldApk, mNewApk2, mPatchPath);
                            }
                        })
                        .map(new Func1<Integer, String>() {
                            @Override
                            public String call(Integer integer) {
                                return checkResult(integer);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                setEnabled(true);
                                mMsgTV.setText(e.toString());
                            }

                            @Override
                            public void onNext(String s) {
                                mMsgTV.setText(s);
                                setEnabled(true);
                                if (s.equals("success")) {
                                    mNewApk2TV.setText(mNewApk2);
                                    mNewApk2SizeTV.setText(String.format("%.2f M", getFileSize(newApk2)));
                                    mNewApk2Md5TV.setText(SignUtils.getMd5ByFile(newApk2));
                                }
                            }
                        });
                break;
            case R.id.delete_btn:
                clearText();
                setEnabled(false);
                File la = new File(mNewApk2);
                if (la.exists()){
                    la.delete();
                }
                File ji = new File(mPatchPath);
                if (ji.exists()){
                    ji.delete();
                }
                setEnabled(true);
                break;
        }
    }

    private String checkResult(int ret) {
        switch (ret) {
            case 0:
                return "success";
            case 1:
                return "缺少文件路径";
            case 2:
                return "读取旧apk失败";
            case 3:
                return "读取新的apk失败";
            case 4:
                return "打开或读取patch文件失败";
            case 5:
                return "内存分配失败";
            case 6:
                return "创建、打开或读取patch文件失败";
            case 7:
                return "计算文件差异性或者写入patch文件失败";
            case 8:
                return "计算压缩的大小差异数据失败";
            case 9:
                return "无用补丁";
            case 10:
                return "合并apk失败";
        }
        return "未知错误";
    }

    private float getFileSize(File file) {
        return (float) (file.length() / (1024 * 1024 * 1.0));
    }

    private void clearText() {
        mOldApkTV.setText("");
        mOldApkSizeTV.setText("");
        mOldApkMd5TV.setText("");
        mNewApkTV.setText("");
        mNewApkSizeTV.setText("");
        mNewApkMd5TV.setText("");
        mPatchTV.setText("");
        mPatchSizeTV.setText("");
        mNewApk2TV.setText("");
        mNewApk2SizeTV.setText("");
        mNewApk2Md5TV.setText("");
        mMsgTV.setText("");
    }

    private void setEnabled(boolean enabled) {
        mLoading.setVisibility(!enabled ? View.VISIBLE : View.GONE);
        mGetNewApkBtn.setEnabled(enabled);
        mGetPatchBtn.setEnabled(enabled);
        mDeleteBtn.setEnabled(enabled);
    }

    @Override
    public void onBackPressed() {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        super.onDestroy();
    }


}
