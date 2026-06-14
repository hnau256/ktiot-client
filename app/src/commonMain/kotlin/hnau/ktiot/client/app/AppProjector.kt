package org.hnau.ktiot.client.app

import androidx.compose.runtime.CompositionLocalProvider
import org.hnau.ktiot.client.model.init.InitModel
import org.hnau.ktiot.client.projector.init.InitProjector
import org.hnau.ktiot.client.projector.init.impl
import org.hnau.ktiot.client.projector.utils.Localization
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.model.app.AppModel
import org.hnau.commons.app.model.theme.ThemeBrightness
import org.hnau.commons.app.model.theme.color.Hue
import org.hnau.commons.app.model.theme.palette.SystemPalettes
import org.hnau.commons.app.projector.app.AppProjector
import org.hnau.commons.app.projector.fractal.context.FContext
import org.hnau.commons.app.projector.fractal.context.LocalFContext

fun createAppProjector(
    scope: CoroutineScope,
    createSystemPalettes: (ThemeBrightness) -> SystemPalettes,
    model: AppModel<InitModel, InitModel.Skeleton>,
): AppProjector<InitModel, InitModel.Skeleton, InitProjector> = AppProjector(
    scope = scope,
    model = model,
    createProjector = { scope, model ->
        InitProjector(
            scope = scope,
            model = model,
            dependencies = InitProjector.Dependencies.impl(
                localization = Localization(),
            ),
        )
    },
    fallbackHue = Hue(300),
    createSystemPalettes = createSystemPalettes,
    content = { rootProjector, contentPadding, palettes ->
        CompositionLocalProvider(
            LocalFContext provides FContext.createBase(
                palettes = palettes,
            )
        ) {
            rootProjector.Content(
                contentPadding = contentPadding,
            )
        }
    }
)