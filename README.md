# SillyBoy
android 动态加载so库实现 你所不知道的“船新”版本
## 关于demo
本项目是动态so加载的核心流程，所以并不直接演示so的下载过程，需要demo使用者将test_so_package的三个demo so库放到指定的演示位置（模拟so已经下载好了，可对接自己使用的下载方式），本例子是放在目录
```
data/data/com.example.sillyboy/files/dynamic_so
```
![image](https://user-images.githubusercontent.com/65278264/235083927-1a23914d-44da-493f-88ab-1b3dfa99326d.png)


## 设计原理
文档请看这里：https://juejin.cn/post/7107958280097366030

## 使用说明
### 声明
注意：由于这是一个基础库，如果需要在正式项目中使用，还是需要继续封装或者优化的，这里只是动态加载so的实现，在实战中应当配合so版本管理，下载器等实际使用
### 使用步骤
1. 如何删除本地的so库【可选】
```
方式1
packagingOptions下增加
exclude 'lib/arm64-v8a/xxx.so'
exclude 'lib/armeabi-v7a/xxx.so'
```

```
方式2
复制以下task到app的build.gradle

如果需要删除so的过程中进行一些列的定制化操作，可参考如下task，见app目录例子
ext {
    deleteSoName = ["libnativecpptwo.so","libnativecpp.so"]
}
// 这个是初始化 -配置 -执行阶段中，配置阶段执行的任务之一，完成afterEvaluate就可以得到所有的tasks，从而可以在里面插入我们定制化的数据
task(dynamicSo) {
}.doLast {
    println("dynamicSo insert!!!! ")
    //projectDir 在哪个project下面，projectDir就是哪个路径
    print(getRootProject().findAll())

    def file = new File("${projectDir}/build/intermediates/merged_native_libs/debug/out/lib")
    //默认删除所有的so库
    if (file.exists()) {
        file.listFiles().each {
            if (it.isDirectory()) {
                it.listFiles().each {
                    target ->
                        print("file ${target.name}")
                        def compareName = target.name
                        deleteSoName.each {
                            if (compareName.contains(it)) {
                                target.delete()
                            }
                        }
                }
            }
        }
    } else {
        print("nil")
    }
}
afterEvaluate {
    print("dynamicSo task start")
    def customer = tasks.findByName("dynamicSo")
    def merge = tasks.findByName("mergeDebugNativeLibs")
    def strip = tasks.findByName("stripDebugDebugSymbols")
    if (merge != null || strip != null) {
        customer.mustRunAfter(merge)
        strip.dependsOn(customer)
    }

}
```
2. 初始化

通过initDynamicSoConfig 方法初始化，第一个参数是context，第二个参数是path，即下载完的so的path，第三个参数是一个回调，在调用动态so加载的时候会先回调，如果返回值为true，才会真正进入到so的加载逻辑，提供给使用者版本校验等一些列活动
```
    // 在合适的时候将自定义路径插入so检索路径 需要使用者自己负责在这个路径上有写入权限
        DynamicSoLauncher.INSTANCE.initDynamicSoConfig(this, path, s -> {
            // 处理一些自定义逻辑
            return true;
        });
```
3. 调用so加载【有两种使用姿势】

3.1 手动加载

在使用到某个so方法前，需要调用loadSoDynamically（代替System.loadLibrary方法），参数是so的名称或者一个File，该File位于初始化时传入的path之内即可
```
DynamicSoLauncher.INSTANCE.loadSoDynamically(file)
```

3.2 注解加载

在需要采取动态so加载的类上，添加@DynamicLoad注解即可，内部会采用字节码插桩的方式，会把类中所有的System.loadLibrary 替换为DynamicSoLauncher内部的so加载
```
//@DynamicLoad
public class MainActivity extends AppCompatActivity {
```
注意，执行这个步骤需要发布当前的lib_sillyplugin插件
![image](https://user-images.githubusercontent.com/65278264/235086729-fa92051d-4282-4ffe-9817-d04de2587711.png)
之后在需要的工程build.gradle引入即可，跟一般的插件引入方式一样
```
apply plugin: 'com.plugins.core'
```
ps：这里其实已经可以实现了无痕替代，只是考虑到大多数可能只动态加载少数的so，才提供出来注解的方式去限定范围。


## 项目层级介绍
* **app下是使用例子**
* **lib_sillyboy 是SillyBoy的封装实现**
* **lib_sillyplugin 是SillyBoy的插件，用于替换代码的System.loadLibrary,默认关闭了，可看代码注释打开**

## 环境准备
建议直接用最新的稳定版本Android Studio打开工程。目前项目已适配`Android Studio Arctic Fox | 2020.3.1`
### 


## 后续todo
* **maven发布**
* **贡献者名单**
