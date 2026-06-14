plugins {
    id(hnau.plugins.kotlin.serialization.get().pluginId)
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.kmpAndroidWithCompose.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(hnau.commons.app.projector)
                implementation(hnau.commons.app.model)
                implementation(project(":model"))
                implementation(project(":projector"))
                implementation(libs.ktiot.scheme)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.hnau.ktiot.client.app.DesktopAppKt"
    }
}
