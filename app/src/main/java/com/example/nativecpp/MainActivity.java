package com.example.nativecpp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.example.nativecpp.databinding.ActivityMainBinding;
import com.pika.sillyboy.DynamicLoad;
import com.pika.sillyboy.DynamicSoLauncher;


import java.io.File;

// 把这个注解删掉，System.loadLibrary就会走正常的流程，否则就会走插桩流程
//@DynamicLoad
public class MainActivity extends AppCompatActivity {


    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getFilesDir().getAbsolutePath() + "/dynamic_so/";

        Log.i("hello", "path:" + path);
        File file = new File(path + "libnative3.so");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // 正常的流程System.loadLibrary
        binding.loadNormal.setOnClickListener(v -> System.loadLibrary("native3"));

        // 调用动态so库
        binding.loadDynamic.setOnClickListener(v -> DynamicSoLauncher.INSTANCE.loadSoDynamically(file));
        binding.native1.setOnClickListener(v -> clickNative1());

        binding.native2.setOnClickListener(v -> clickNative2());

        binding.native3.setOnClickListener(v -> clickNative3());

    }


    public native void clickNative1();
    public native void clickNative2();
    public native void clickNative3();
}

