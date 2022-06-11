package com.example.nativecpp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.lib_sillyboy.DynamicSo;
import com.example.nativecpp.databinding.ActivityMainBinding;
import com.example.lib_sillyboy.elf.ElfParser;


import java.io.File;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'nativecpptwo' library on application startup.
    static {
        //System.loadLibrary("nativecpptwo");

    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String absolutePath = getFilesDir().getAbsolutePath();
        Log.i("hello", "ab " + absolutePath);
        File file = new File(absolutePath + "/dynamic_so/libnativecpptwo.so");
        DynamicSo.loadStaticSo(file);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickTest();

            }
        });
    }

    /**
     * A native method that is implemented by the 'nativecpp' native library,
     * which is packaged with this application.
     */

    public native void clickTest();
}

