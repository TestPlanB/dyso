package com.pika.sillyboy

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import java.io.File

@Keep
object DynamicSoLauncher {
    private var soPath = ""
    private var beforeSoLoadListener: ((soName: String) -> Boolean)? = null
    fun initDynamicSoConfig(context: Context, soPath: String,beforeLoadListener: ((soName: String) -> Boolean)?=null) {
        this.soPath = soPath
        this.beforeSoLoadListener = beforeLoadListener
        DynamicSo.insertPathToNativeSystem(context, File(soPath))
    }

    fun loadSoDynamically(file: File) {
        if (soPath.isEmpty()) {
            throw RuntimeException("you must call initDynamicSoConfig first. The soPath is empty")
        }
        if(beforeSoLoadListener?.invoke(file.name) == false){
            return
        }
        DynamicSo.loadSoDynamically(file, soPath)
    }

    fun loadSoDynamically(fileName: String) {
        if (soPath.isEmpty()) {
            throw RuntimeException("you must call initDynamicSoConfig first. The soPath is empty")
        }
        if(beforeSoLoadListener?.invoke(fileName) == false){
            return
        }
        DynamicSo.loadSoDynamically(File(soPath + fileName), soPath)
    }


    // 插件调用
    @JvmStatic
    fun loadLibrary(soName: String) {
        Log.e("hello", soName)
        val wrapSoName = "lib${soName}.so"
        loadSoDynamically(wrapSoName)
    }
}