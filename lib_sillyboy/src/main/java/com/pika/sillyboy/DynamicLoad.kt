package com.pika.sillyboy

import androidx.annotation.Keep


@Keep
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DynamicLoad()
