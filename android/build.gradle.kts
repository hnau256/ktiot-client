import java.util.Properties

plugins {
    id("com.android.application") //TODO remove
    id("org.jetbrains.kotlin.android") //TODO remove
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.jvmAndroidApp.get().pluginId)
}

android {
    namespace = "hnau.ktiot"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        val versionPropsFile = file("version.properties")
        val versionProps =
            Properties().apply {
                load(versionPropsFile.inputStream())
            }
        val localVersionCode = (versionProps["versionCode"] as String).toInt()
        versionName = versionProps["versionName"] as String + "." + localVersionCode
        versionCode = localVersionCode

        tasks.named("preBuild") {
            doFirst {
                versionProps.setProperty("versionCode", (localVersionCode + 1).toString())
                versionProps.store(versionPropsFile.outputStream(), null)
            }
        }
    }

    signingConfigs {
        create("qa") {
            storeFile = file("keystores/qa.keystore")
            storePassword = "password"
            keyAlias = "qa"
            keyPassword = "password"
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            isDebuggable = false
            proguardFile("proguard-rules.pro")
            // signingConfig = signingConfigs.getByName("release")
        }
        create("qa") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("qa")
            applicationIdSuffix = ".qa"
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
}

dependencies {
    implementation(hnau.commons.app.projector)
    implementation(hnau.commons.app.model)
    implementation(project(":model"))
    implementation(project(":projector"))
    implementation(project(":app"))
}
