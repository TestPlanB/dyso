#include <jni.h>
#include <string>
#include <signal.h>
#include<android/log.h>

jobject obj;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativecpp_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";

    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_nativecpp_MainActivity_test(JNIEnv *env, jobject thiz, jstring test) {
    std::string temp = "c++";

    std::string region = env->GetStringUTFChars(test,0);
    std::string result = region +temp;

    return env ->NewStringUTF(result.c_str());


}



void SigFunc(int sig_num){
    switch (sig_num) {
        case SIGABRT:{
        __android_log_print(ANDROID_LOG_INFO,"hello","SIGABRT catch");
            break;
        }

    }
}
// c++调起java
extern "C"
JNIEXPORT void JNICALL
Java_com_example_nativecpp_MainActivity_onJavaClick(JNIEnv *env, jobject thiz, jobject listener) {
    //  注册信号监听
    struct sigaction sigc;
    sigc.sa_handler = SigFunc;
    sigemptyset(&sigc.sa_mask);
    sigc.sa_flags = SA_RESTART;
    int flag = sigaction(SIGABRT,&sigc,NULL);
    if(flag == -1){
        __android_log_print(ANDROID_LOG_INFO,"hello","register -1");
    }
    __android_log_print(ANDROID_LOG_INFO,"hello","jni test");


//    jclass  listenerCls = env->GetObjectClass(listener);
//    printf("xc1");
//    jmethodID id=env->GetMethodID(listenerCls,"onClick","(Ljava/lang/String;)V");
//    printf("xc2");
//    obj = env->NewGlobalRef(listener);
//    printf("xc3");
//    env->CallVoidMethod(obj,id,env->NewStringUTF("call from c=="));



}


// 测试crash，crash先于监听抓取函数
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_nativecpp_MainActivity_throwNativeCrash(JNIEnv *env, jobject thiz) {
    int i = 0/0;
    jstring j = (jstring) "132";

    __android_log_print(ANDROID_LOG_INFO,"hello","jni will crash");
    return j;
}




