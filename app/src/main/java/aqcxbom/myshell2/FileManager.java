package aqcxbom.myshell2;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by AqCxBoM on 2017/6/26.
 */

public class FileManager {
    /**
     * 读取assets目录指定文件内容并通过解密方法解密，将结果释放到releaseFile指向的路径中
     * @param ct
     * @param assetFile 资源名
     * @param releaseFile 释放路径
     * @param decMethod 解密方法，public static byte[] dec(byte[] src)
     * @return
     */
    public static File releaseAssetsFile(Context ct, String assetFile, String releaseFile, Method decMethod){
        AssetManager am = ct.getAssets();
        try {
            //打开文件并读取所有内容到ByteArrayOutputStream流中
            InputStream is = am.open(assetFile);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int iRead;
            while((iRead = is.read(buf)) != -1){
                os.write(buf, 0, iRead);
            }
            // 调用解密方法去解密
            byte[] dec = decMethod != null ? (byte[]) decMethod.invoke(null, os.toByteArray()) : os.toByteArray();
            is.close();
            os.close();
            // 输出到指定路径中
            FileOutputStream fos = new FileOutputStream(new File(releaseFile));
            fos.write(dec);
            fos.close();
            return new File(releaseFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
