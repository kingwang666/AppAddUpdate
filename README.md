# AppAddUpdate #
Android app 增量更新 
 
参考[https://github.com/cundong/SmartAppUpdates](https://github.com/cundong/SmartAppUpdates)  

该app未提供服务端代码（服务端代码的增量更新实现可参考diff.c和patch.c，原理和客户端一致）

## ScreenShoots ##
![](http://i.imgur.com/nutEUKE.jpg)

## Android Studio NDK ##
1. 首先下载NDK  
![](http://i.imgur.com/VTabP2r.png)
2. 在项目的<font color="blue">gradle.properties</font>文件下添加<font color="green">android.useDeprecatedNdk=true</font>
3. 新建含有native方法的类(如 PatchUtil.java)
4. 编译一下在`app\build\intermediates\classes\debug`目录下对应类的包名下会自动创建出PatchUtil.clasee
5. 点击Android Studio的Terminal进入`app\build\intermediates\classes\debug`目录下
6. Android Studio2.0及以上输入`javah -classpath . -jni om.wang.appupdate.util(packName).PatchUtil(ClassName)` 其他版本输入`javah -jni om.wang.appupdate.util(packName).PatchUtil(ClassName)`创建对应的.h文件(在`app\build\intermediates\classes\debug`根目录下)
7. 新建jni文件夹  
![](http://i.imgur.com/TwDxie1.png)  
创建的.h文件剪切过来。编写对应的c文件(**命名随意**)
8. 在app的build.gradle配置  
![](http://i.imgur.com/XdEbtDQ.png)
9. 运行即可，对应的.so在文件夹  
![](http://i.imgur.com/B7Zulc4.png)

## API ##
### PatchUtil ###
- `int diff(String oldApkPath, String newApkPath,String patchPath)`  
 比较路径为oldPath的apk与newPath的apk之间差异，并生成patch包.


- `int patch(String oldApkPath, String newApkPath, String patchPath)`  
使用路径为oldApkPath的apk与路径为patchPath的补丁包，合成新的apk，并存储newApkPath  
### 返回码 ###
0-success  
1-缺少文件路径  
2-读取旧apk失败  
3-读取新的apk失败  
4-打开或读取patch文件失败  
5-内存分配失败  
6-创建、打开或读取patch文件失败  
7-计算文件差异性或者写入patch文件失败  
8-计算压缩的大小差异数据失败  
9-无用的patch补丁  
10-合并apk失败

### SignUtil ###
- `String getMd5ByFile(File file)`  
获取对应文件的md5值

- `boolean checkMd5(File file, String md5)`  
判断文件的MD5是否为指定值

- `boolean checkMd5(String filePath, String md5)`  
判断文件的MD5是否为指定值

### ApkUtil ###


- `PackageInfo getInstalledApkPackageInfo(Context context, String packageName)`  
获取已安装apk的PackageInfo


- `boolean isInstalled(Context context, String packageName)`  
判断apk是否已安装


- `String getSourceApkPath(Context context, String packageName)`  
获取已安装Apk文件的源Apk文件


- `String getSourceApkPath(Context context)`  
获取已安装Apk文件的源Apk文件


- `public static void installApk(Context context, String apkPath)`  
安装Apk
    