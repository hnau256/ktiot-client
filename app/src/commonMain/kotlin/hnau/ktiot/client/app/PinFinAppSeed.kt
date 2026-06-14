package org.hnau.ktiot.client.app

import org.hnau.commons.app.model.app.AppFilesDirProvider
import org.hnau.commons.app.model.app.AppSeed
import org.hnau.commons.app.model.file.plus
import org.hnau.commons.app.model.preferences.impl.FileBasedPreferences
import org.hnau.ktiot.client.model.init.InitModel
import org.hnau.ktiot.client.model.init.impl

fun createPinFinAppSeed(
    appFilesDirProvider: AppFilesDirProvider,
): AppSeed<InitModel, InitModel.Skeleton> = AppSeed(
    skeletonSerializer = InitModel.Skeleton.serializer(),
    createDefaultSkeleton = { InitModel.Skeleton() },
    createModel = { scope, skeleton ->
        val appFilesDir = appFilesDirProvider.getAppFilesDir()
        InitModel(
            scope = scope,
            dependencies = InitModel.Dependencies.impl(
                preferencesFactory = FileBasedPreferences.Factory(
                    preferencesFile = appFilesDir + "preferences.txt"
                )
            ),
            skeleton = skeleton,
        )
    },
)