#include <jni.h>
#include <android/log.h>

//
// Created by chenhailiang on 2023/4/27.
//


JNIEXPORT void JNICALL
Java_com_example_nativecpp_MainActivity_clickNative3(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "hello", "%s", "native3");
}