package aqcxbom.myshell2;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.ArrayMap;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import dalvik.system.DexClassLoader;

/**
 * Created by AqCxBoM on 2017/6/26.
 */

public class ProxyApplication extends Application {
    private final String TAG = "AqCxBoM";
    //旧的APPLICATION路径要求存放在这里
    private static final String appKey = "APPLICATION_CLASS_NAME";
    private String OdexPath;
    private String cachePath;
    private String libPath;
    private String srcDex;
    @Override
    public void onCreate() {
        String appClassName = null;
        //读取meta_data数据查看是否存在原始application
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if(bundle != null && bundle.containsKey(appKey)){
                appClassName = bundle.getString(appKey);
            }
            else
                return;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // 下面为强制重构Application，参考Android源码
        // frameworks/base/core/java/android/app/ActivityThread.java
        // frameworks/base/core/java/android/app/LoadedApk.java
        // 首先清除原始的app数据
        // 1.调用ActivityThread类的currentActivityThread方法获取ActivityThread类对象
        Object currentActivityThread = RefInvoke.invokeStaticMethod(
                "android.app.ActivityThread",
                "currentActivityThread",
                new Class[]{}, new Object[]{}
        );
        // 2.通过ActivityThread类对象获取其mBoundApplication域对象
        Object mBoundApplication = RefInvoke.getFieldObject(
                "android.app.ActivityThread",
                currentActivityThread,
                "mBoundApplication"
        );
        // 3.通过mBoundApplication对象获取info对象，这是一个LoadedApk类型
        Object loadedApkInfo = RefInvoke.getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication,
                "info"
        );
        // 4.清除LoadedApk对象mApplication域数据
        RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication",
                loadedApkInfo, null);
        // 5.获取ActivityThread对象的mInitalApplication域对象（初始化Application）
        Object oldApplication = RefInvoke.getFieldObject(
                "android.app.ActivityThread",
                currentActivityThread,
                "mInitalApplication"
        );

        //获取ActivityThread对象域：final ArrayList<Application> mAllApplications
        //                                                  = new ArrayList<Application>();
        ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread, "mAllApplications"
        );
        // 从中清除旧appliction信息
        mAllApplications.remove(oldApplication);
        // 从LoadedApk对象mBoundApplication类，拿mApplicationInfo域对象
        ApplicationInfo appinfo_in_loadedApk = (ApplicationInfo) RefInvoke.getFieldObject(
                "android.app.LoadedApk", loadedApkInfo, "mApplicationInfo"
        );
        // 获取ActivityThread;->mBoundApplication对象的域 ApplicationInfo appInfo;对象
        ApplicationInfo appinfo_in_AppBindData = (ApplicationInfo) RefInvoke.getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication,
                "appInfo"
        );

        //改写APP信息
        appinfo_in_loadedApk.className = appClassName;
        appinfo_in_AppBindData.className = appClassName;
        // 调用LoadedApk类的makeApplication方法生成新的APPLICATION
        Application app = (Application) RefInvoke.invokeMethod(
                "android.app.LoadedApk", "makeApplication", loadedApkInfo,
                new Class[]{boolean.class, Instrumentation.class},
                new Object[]{false, null}
        );
        // 新的Application设置到mInitalApplication域
        RefInvoke.setFieldOjbect("android.app.ActivityThread",
                "mInitalApplication", currentActivityThread, app);
        // 获取ActivityThread类mProviderMap域对象
        HashMap mProviderMap = (HashMap) RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread, "mProviderMap");
        // 遍历mProviderMap所有成员，将新Application设置过去
        Iterator it = mProviderMap.values().iterator();
        while(it.hasNext()){
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldObject(
                    "android.app.ActivityThread$ProviderClientRecord",
                    providerClientRecord,"mLocalProvider");
            RefInvoke.setFieldOjbect("android.content.ContentProvider",
                    "mContext", localProvider, app);
        }
        super.onCreate();
    }

    /**
     * 解密加载DEX并代换到LoadedApk中的mClassLoader域中
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        File cache = getDir(".fshell", MODE_PRIVATE);
        OdexPath = getDir(".fodex", MODE_PRIVATE).getAbsolutePath();
        cachePath = cache.getAbsolutePath();
        libPath = getApplicationInfo().nativeLibraryDir;
        srcDex = cachePath + "/decrypt.dex";

        try {
            File decFile = FileManager.releaseAssetsFile(this, "encrypt", srcDex, getClass().getDeclaredMethod("decMethod", byte[].class));
            if(!decFile.exists())
                decFile.createNewFile();

            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[]{}, new Object[]{}
            );
            String packageName = getPackageName();
            ArrayMap mPackage = (ArrayMap)RefInvoke.getFieldObject(
                    "android.app.ActivityThread", currentActivityThread,
                    "mPackages"
            );
            WeakReference wr = (WeakReference)mPackage.get(packageName);

            // 加载解密后的dex文件并替换到ActivityThread类mPackages成员的LoadedApk域中
            DexClassLoader dLoader = new DexClassLoader(decFile.getAbsolutePath(),
                    OdexPath, libPath, (ClassLoader)RefInvoke.getFieldObject(
                    "android.app.LoadedApk", wr.get(), "mClassLoader"
            ));

            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader", wr.get(), dLoader);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] decMethod(byte[] in){
        return in;
    }
}
