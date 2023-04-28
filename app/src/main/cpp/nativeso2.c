#include <jni.h>
#include <android/log.h>



JNIEXPORT void JNICALL
Java_com_example_nativecpp_MainActivity_clickNative2(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "hello", "%s", "native2");
}