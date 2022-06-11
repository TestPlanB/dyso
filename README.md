# SillyBoy
android 动态加载so库实现
## 详细介绍
https://juejin.cn/post/7107958280097366030

## 使用说明
### 声明
注意：由于这是一个基础库，如果需要在正式项目中使用，还是需要继续封装或者优化的，这里只是动态加价so的实现，在实战中应当配合so版本管理，下载器等实际使用
### 本地使用
该项目可以用于本地配置使用，只需拷贝lib_sillyboy这个module到自己的项目即可，请按照以下条件使用

1.拷贝lib_sillyboy这个module到自己的项目

2.【可选】复制以下task到app的build.gradle（暂时没想到其他解决办法，因为在lib库中配置只对当前lib生效），然后在deleteSoName中配置想要删除的so库，手动删除则不用

```
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

3.在so文件下载成功后，使用  ```DynamicSo.insertPathToNativeSystem(this,file); ``` file为路径，如app例子项目所示

4.可全局替换System.load/在需要用到native方法前调用  ```DynamicSo.loadStaticSo(file); ```file为目标so文件



## 项目层级介绍
* **app下是使用例子**
* **lib_sillyboy 是SillyBoy的封装实现**

## 环境准备
建议直接用最新的稳定版本Android Studio打开工程。目前项目已适配`Android Studio Arctic Fox | 2020.3.1`
### 
