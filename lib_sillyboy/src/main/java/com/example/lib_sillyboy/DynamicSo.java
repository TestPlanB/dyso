package com.example.lib_sillyboy;

import android.content.Context;

import com.example.lib_sillyboy.elf.ElfParser;
import com.example.lib_sillyboy.tinker.TinkerLoadLibrary;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class DynamicSo {
    public static void loadStaticSo(File soFIle) {
        try {
            ElfParser parser = null;
            final List<String> dependencies;
            try {
                parser = new ElfParser(soFIle);
                dependencies = parser.parseNeededDependencies();
            } finally {
                if (parser != null) {
                    parser.close();
                }

            }
            for (final String dependency : dependencies) {
                // 把本来lib前缀和.so后缀去掉即可
                String dependencySo = dependency.substring(3, dependency.length() - 3);
                System.loadLibrary(dependencySo);
                //loadLibrary(context, libraryLoader.unmapLibraryName(dependency));
            }

        } catch (IOException ignored) {
            // This a redundant step of the process, if our library resolving fails, it will likely
            // be picked up by the system's resolver, if not, an exception will be thrown by the
            // next statement, so its better to try twice.
        }
        // 先把依赖项加载完，再加载本身
        System.loadLibrary(soFIle.getName().substring(3, soFIle.getName().length() - 3));
    }

    public static void insertPathToNativeSystem(Context context,File file){
        try {
            TinkerLoadLibrary.installNativeLibraryPath(context.getClassLoader(), file);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
