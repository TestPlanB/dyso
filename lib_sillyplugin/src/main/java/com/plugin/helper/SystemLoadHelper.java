package com.plugin.helper;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

// power by pika

/*
public static void loadLibrary(String libname) {
        Runtime.getRuntime().loadLibrary0(Reflection.getCallerClass(), libname);
        }
 */
public class SystemLoadHelper {
    private static String PACKAGE_PATH = "com/pika/sillyboy";
    private static String OWNER = "java/lang/System";
    private static String METHOD_NAME =  "loadLibrary";
    private static String METHOD_DESC = "(Ljava/lang/String;)V";
    private static String DYNAMIC_OWNER  = "com/pika/sillyboy/DynamicSoLauncher";
    public static void transClass(ClassNode classNode) {
        if (classNode.name.startsWith(PACKAGE_PATH)){
            return;
        }
        classNode.methods.forEach(methodNode -> methodNode.instructions.forEach(abstractInsnNode -> {
            // 如果是InvokeStatic才继续进行
            if (abstractInsnNode.getOpcode() == Opcodes.INVOKESTATIC) {
                transformInvokeStatic((MethodInsnNode) abstractInsnNode);
            }
        }));
    }

    static void transformInvokeStatic(MethodInsnNode methodInsnNode) {
        // (Ljava/lang/String;)V loadLibrary java/lang/System
        if (OWNER.equals(methodInsnNode.owner) && METHOD_NAME.equals(methodInsnNode.name) && METHOD_DESC.equals(methodInsnNode.desc)) {
            methodInsnNode.owner = DYNAMIC_OWNER;
        }
    }


}
