plugins {
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.kmpAndroidWithCompose.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(hnau.commons.app.projector)
                implementation(project(":model"))
                implementation(libs.ktiot.mqtt)
                implementation(libs.ktiot.scheme)
            }
        }
    }
}
