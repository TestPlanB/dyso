package com.plugin.core


import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class DynamicPlugin implements Plugin<Project> {
    Project project
    @Override
    void apply(Project target) {
        project = target
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(new DynamicTransform(project), Collections.EMPTY_LIST)
    }
}


