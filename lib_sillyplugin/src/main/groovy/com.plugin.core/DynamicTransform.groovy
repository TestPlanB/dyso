package com.plugin.core


import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.plugin.helper.SystemLoadHelper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class DynamicTransform extends Transform {
    private static final FileTime ZERO = FileTime.fromMillis(0)
    private static final String FILE_SEP = File.separator
    private WaitableExecutor waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
    private Project project

    DynamicTransform() {
    }

    DynamicTransform(Project outProject) {
        this.project = outProject
    }

    @Override
    String getName() {
        return "DynamicTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(@NonNull TransformInvocation transformInvocation) {
        println '--------------- DynamicTransform start --------------- ' + transformInvocation.incremental
        def startTime = System.currentTimeMillis()
        Collection<TransformInput> inputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        if (!transformInvocation.incremental) {
            if (outputProvider != null) {
                outputProvider.deleteAll()
            }
        }

        boolean flagForCleanDexBuilderFolder = false

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectoryInput(directoryInput, outputProvider, transformInvocation.incremental)
            }
            input.jarInputs.each { JarInput jarInput ->
                Status status = jarInput.getStatus()
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR)
                if (transformInvocation.incremental) {
                    switch (status) {
                        case Status.NOTCHANGED:
                            break
                        case Status.ADDED:
                        case Status.CHANGED:
                            waitableExecutor.execute(new Callable<Object>() {
                                @Override
                                Object call() throws Exception {
                                    transformJar(jarInput.file, dest, jarInput)
                                    return null
                                }
                            })
                            break
                        case Status.REMOVED:
                            if (dest.exists()) {
                                FileUtils.forceDelete(dest)
                            }
                            break
                    }
                } else {
                    if (!transformInvocation.incremental && !flagForCleanDexBuilderFolder) {
                        cleanDexBuilderFolder(dest)
                        flagForCleanDexBuilderFolder = true
                    }

                    transformJar(jarInput.file, dest, jarInput)
                }
            }
        }
        def costTime = (System.currentTimeMillis() - startTime) / 1000
        waitableExecutor.waitForTasksWithQuickFail(true)
        println "DynamicTransform cost：$costTime s"
    }

    byte[] handleTransform(byte[] bytes) {
        def classNode = new ClassNode()
        new ClassReader(bytes).accept(classNode, 0)
        classNode = handleTransform(classNode)
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(cw)
        return cw.toByteArray()
    }

    ClassNode handleTransform(ClassNode klass) {
        def targetClass = false
        if (klass.visibleAnnotations != null){
            klass.visibleAnnotations.forEach(new Consumer<AnnotationNode>() {
                @Override
                void accept(AnnotationNode annotationNode) {
                    println annotationNode.desc
                    if (annotationNode.desc == "Lcom/pika/sillyboy/DynamicLoad;"){
                        targetClass = true
                    }
                }
            })
        }
        if (targetClass){
            SystemLoadHelper.transClass(klass)
        }
        return klass
    }

    void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider, boolean incremental) {

        File dest = outputProvider.getContentLocation(directoryInput.getName(),
                directoryInput.getContentTypes(), directoryInput.getScopes(),
                Format.DIRECTORY)
        FileUtils.forceMkdir(dest)
        if (incremental) {
            String srcDirPath = directoryInput.getFile().getAbsolutePath()
            String destDirPath = dest.getAbsolutePath()
            Map<File, Status> fileStatusMap = directoryInput.getChangedFiles()
            for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                Status status = changedFile.getValue()
                File inputFile = changedFile.getKey()
                String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath)
                File destFile = new File(destFilePath)
                switch (status) {
                    case Status.NOTCHANGED:
                        break
                    case Status.REMOVED:
                        if (destFile.exists()) {
                            destFile.delete()
                        }
                        break
                    case Status.ADDED:
                    case Status.CHANGED:
                        try {
                            FileUtils.touch(destFile)
                        } catch (IOException e) {
                            e.printStackTrace()
                            com.google.common.io.Files.createParentDirs(destFile)
                        }
                        transformSingleFile(inputFile, destFile, srcDirPath)
                        break
                }
            }
        } else {
            if (directoryInput.file.isDirectory()) {
                directoryInput.file.eachFileRecurse { File file ->
                    def name = file.name
                    if (checkClassFile(name)) {
                        try {
                            byte[] code = handleTransform(file.bytes)
                            FileOutputStream fos = new FileOutputStream(
                                    file.parentFile.absolutePath + File.separator + name)
                            fos.write(code)
                            fos.close()
                        } catch (Exception e) {
                            println e.message
                        }

                    }
                }
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
    }

    private void transformSingleFile(final File inputFile, final File outputFile, final String srcBaseDir) {
        weaveSingleClassToFile(inputFile, outputFile, srcBaseDir)
    }

    final void weaveSingleClassToFile(File inputFile, File outputFile, String inputBaseDir) throws IOException {
        if (!inputBaseDir.endsWith(FILE_SEP)) {
            inputBaseDir = inputBaseDir + FILE_SEP
        }
        if (isInjectClass(inputFile.getAbsolutePath().replace(inputBaseDir, "").replace(FILE_SEP, "."))) {
            FileUtils.touch(outputFile)
            byte[] bytes = handleTransform(inputFile.bytes)
            FileOutputStream fos = new FileOutputStream(outputFile)
            fos.write(bytes)
            fos.close()
            inputStream.close()
        } else {
            if (inputFile.isFile()) {
                FileUtils.touch(outputFile)
                FileUtils.copyFile(inputFile, outputFile)
            }
        }
    }

    static boolean isInjectClass(String fullQualifiedClassName) {
        return fullQualifiedClassName.endsWith(".class") && !fullQualifiedClassName.contains("R\$") && !fullQualifiedClassName.contains("R.class") && !fullQualifiedClassName.contains("BuildConfig.class")
    }

    void transformJar(File inputJar, File outputJar, JarInput jarInput) throws IOException {
        ZipFile inputZip = new ZipFile(inputJar)
        ZipOutputStream outputZip = new ZipOutputStream(new BufferedOutputStream(
                Files.newOutputStream(outputJar.toPath())))
        Enumeration<? extends ZipEntry> inEntries = inputZip.entries()
        while (inEntries.hasMoreElements()) {
            ZipEntry entry = inEntries.nextElement()
            InputStream originalFile =
                    new BufferedInputStream(inputZip.getInputStream(entry))
            ZipEntry outEntry = new ZipEntry(entry.getName())
            byte[] newEntryContent
            if (!checkClassFile(outEntry.getName().replace("/", "."))) {
                newEntryContent = IOUtils.toByteArray(originalFile)
            } else {
                newEntryContent = handleTransform(IOUtils.toByteArray(originalFile))
            }
            CRC32 crc32 = new CRC32()
            crc32.update(newEntryContent)
            outEntry.setCrc(crc32.getValue())
            outEntry.setMethod(ZipEntry.STORED)
            outEntry.setSize(newEntryContent.length)
            outEntry.setCompressedSize(newEntryContent.length)
            outEntry.setLastAccessTime(ZERO)
            outEntry.setLastModifiedTime(ZERO)
            outEntry.setCreationTime(ZERO)
            outputZip.putNextEntry(outEntry)
            outputZip.write(newEntryContent)
            outputZip.closeEntry()
        }
        outputZip.flush()
        outputZip.close()
    }

    private void cleanDexBuilderFolder(File dest) {
        try {
            String dexBuilderDir = replaceLastPart(dest.getAbsolutePath(), getName(), "dexBuilder")
            File file = new File(dexBuilderDir).getParentFile()
            if (file.exists() && file.isDirectory()) {
                com.android.utils.FileUtils.deleteDirectoryContents(file)
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    private static String replaceLastPart(String originString, String replacement, String toreplace) {
        int start = originString.lastIndexOf(replacement)
        StringBuilder builder = new StringBuilder()
        builder.append(originString.substring(0, start))
        builder.append(toreplace)
        builder.append(originString.substring(start + replacement.length()))
        return builder.toString()
    }

    static boolean checkClassFile(String name) {
        return (name.endsWith(".class") && !name.startsWith("R\$")
                && name != "R.class" && !name.startsWith("BR\$")
                && name != "BR.class" && name != "BuildConfig.class" && !name.startsWith("kotlinx"))
    }
}
