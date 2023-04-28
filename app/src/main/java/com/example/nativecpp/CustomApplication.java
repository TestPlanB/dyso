package com.example.nativecpp;

import android.app.Application;
import android.util.Log;


import com.pika.sillyboy.DynamicSoLauncher;

import java.io.File;

import kotlin.jvm.functions.Function1;

public class CustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // data/data/com.example.sillyboy/files/dynamic_so
        String path = getFilesDir().getAbsolutePath() + "/dynamic_so/";

        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }

        // 在合适的时候将自定义路径插入so检索路径 需要使用者自己负责在这个路径上有写入权限
        DynamicSoLauncher.INSTANCE.initDynamicSoConfig(this, path, s -> {
            // 处理一些自定义逻辑
            return true;
        });
    }


}
