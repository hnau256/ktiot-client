plugins {
    id(hnau.plugins.kotlin.serialization.get().pluginId)
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.kmpAndroid.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(hnau.commons.app.model)
                implementation(hnau.kotlinx.io)
                implementation(hnau.kotlinx.serialization.cbor)
                implementation(hnau.kotlinx.serialization.json)
                implementation(libs.ktiot.scheme)
                implementation(libs.ktiot.mqtt)
            }
        }
    }
}
