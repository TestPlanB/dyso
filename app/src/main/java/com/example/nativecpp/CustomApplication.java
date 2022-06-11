package com.example.nativecpp;

import android.app.Application;


import com.example.lib_sillyboy.DynamicSo;

import java.io.File;

public class CustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        String absolutePath = getFilesDir().getAbsolutePath();
        File file = new File(absolutePath + "/dynamic_so");
        if (!file.exists()) {
            file.mkdir();
        }
        // 在合适的时候将自定义路径插入so检索路径
        DynamicSo.insertPathToNativeSystem(this,file);



    }


}
