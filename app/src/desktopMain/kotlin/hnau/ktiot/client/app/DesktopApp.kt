@file:OptIn(InternalComposeUiApi::class)

package org.hnau.ktiot.client.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.hnau.commons.app.model.app.DesktopApp
import org.hnau.commons.app.model.theme.ThemeBrightness


@OptIn(InternalComposeApi::class)
fun main() {
    Logger.setLogWriters(platformLogWriter())

    val appScope = CoroutineScope(SupervisorJob())
    val app = DesktopApp(
        scope = appScope,
        seed = createPinFinAppSeed(
            defaultBrightness = ThemeBrightness.Dark,
        ),
    )
    val projector = createAppProjector(
        scope = appScope,
        model = app,
    )
    application {
        val scale = 2f
        Window(
            onCloseRequest = { exitApplication() },
            title = "KtIoT",
            /*state = rememberWindowState(
                width = 480.dp * scale,
                height = 640.dp * scale,
            ),*/
            //icon = rememberVectorPainter(pinfinIcon.s256),
        ) {
            val density = remember(scale) { Density(scale) }
            val insets = remember(density) { WindowInsets(density) }
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalPlatformWindowInsets provides insets,
            ) {
                projector.Content()
            }
        }
    }
}

@OptIn(InternalComposeUiApi::class)
private class WindowInsets(
    density: Density,
) : PlatformWindowInsets {

    override val systemBars: PlatformInsets = density.PlatformInsets(
        top = 16.dp,
        bottom = 16.dp,
    )
}