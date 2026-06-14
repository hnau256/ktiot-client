-dontwarn org.slf4j.impl.StaticLoggerBinder

-keep class org.eclipse.paho.clent.mqttv3.** {*;}
-keep class org.eclipse.paho.client.mqttv3.*$* { *; }
-keep class org.eclipse.paho.client.mqttv3.logging.JSR47Logger { *; }


-dontwarn org.slf4j.spi.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class com.google.errorprone.annotations.CanIgnoreReturnValue {
    *;
}
-keep class androidx.compose.runtime.internal.** { *; }
-keep class androidx.compose.ui.platform.AndroidCompositionLocals_androidResourcesKt { *; }
-keep class androidx.compose.ui.res.** { *; }
-keep class android.content.res.** { *; }
-keep class android.util.** { *; }
-keep class org.hnau.commons.android.AndroidDynamicColorsGenerator
-dontwarn androidx.test.platform.app.InstrumentationRegistry