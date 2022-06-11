

#include <jni.h>
#include <string>
#include<android/log.h>


extern "C"
JNIEXPORT void JNICALL
Java_com_example_nativecpp_MainActivity_clickTest(JNIEnv *env, jobject thiz) {
    // 在这里打印一句话
    __android_log_print(ANDROID_LOG_INFO,"hello"," native 层方法");

}