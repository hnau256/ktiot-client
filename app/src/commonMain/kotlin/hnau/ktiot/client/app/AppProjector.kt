package org.hnau.ktiot.client.app

import org.hnau.ktiot.client.model.init.InitModel
import org.hnau.ktiot.client.projector.init.InitProjector
import org.hnau.ktiot.client.projector.init.impl
import org.hnau.ktiot.client.projector.utils.Localization
import kotlinx.coroutines.CoroutineScope
import org.hnau.commons.app.model.app.AppModel
import org.hnau.commons.app.projector.app.AppProjector

fun createAppProjector(
    scope: CoroutineScope,
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
    content = { rootProjector ->
        rootProjector.Content()
    }
)